package com.workflow.sociallabs.node.nodes.ai.agent;

import com.workflow.sociallabs.node.nodes.ai.agent.exception.LlmProviderException;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.*;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.AgentMemoryManager;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.MemoryConfig;
import com.workflow.sociallabs.node.nodes.ai.agent.state.AgentState;
import com.workflow.sociallabs.node.nodes.ai.agent.state.AgentStep;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public final class AgentRunner {

    private final LlmProviderRegistry providerRegistry;
    private final ToolRegistry toolRegistry;
    private final ToolExecutionPipeline pipeline;
    private final AgentMemoryManager memoryManager;
//    private final MeterRegistry meterRegistry;

    private static final String API_KEY = "apiKey";

    public AgentResult run(
            @NonNull AgentState initialState,
            @NonNull ToolContext context,
            @NonNull MemoryConfig memoryConfig,
            @NonNull Map<String, Object> itemJson
    ) {
        Optional<String> apiKey = context.getCredential(API_KEY, String.class);
        if (apiKey.isEmpty()) {
            return failure(initialState, "Api key is missed for agent node", AgentResult.FailureType.LLM_ERROR);
        }

        // 1. Завантажуємо history
        List<AgentMessage> history = memoryManager.loadHistory(
                memoryConfig,
                context.getWorkflowId(),
                context.getAgentNodeId(),
                itemJson
        );

        // 2. Prepend history до AgentState
        AgentState state = history.isEmpty()
                ? initialState
                : initialState.withPrependedHistory(history);

        LlmProvider<?, ?> provider = providerRegistry.forModel(state.getModelId());
        Instant deadline = Instant.now(context.getClock()).plusMillis(state.getLimits().getTimeoutMs());

        // 3. Запам'ятовуємо початковий розмір — щоб зберегти тільки НОВІ
        int historySize = state.getMessages().size();

        log.info("AgentRunner started model={} maxIter={} workflowId={}", state.getModelId().value(), state.getLimits().getMaxIterations(), context.getWorkflowId());

        while (true) {

            // --- Guardrails ---
            AgentLimits limits = state.getLimits();

            if (state.getIterations() >= limits.getMaxIterations()) {
                return failure(state, "Max iterations reached", AgentResult.FailureType.MAX_ITERATIONS);
            }
            if (state.getToolCallsUsed() >= limits.getMaxToolCalls()) {
                return failure(state, "Max tool calls reached", AgentResult.FailureType.MAX_ITERATIONS);
            }
            if (state.getTotalTokensUsed() >= limits.getMaxTotalTokens()) {
                return failure(state, "Max tokens exceeded", AgentResult.FailureType.MAX_TOKENS);
            }
            if (state.getConsecutiveErrors() >= limits.getMaxConsecutiveErrors()) {
                return failure(state, "Too many consecutive errors", AgentResult.FailureType.MAX_CONSECUTIVE_ERRORS);
            }
            if (Instant.now(context.getClock()).isAfter(deadline)) {
                return failure(state, "Agent timeout", AgentResult.FailureType.TIMEOUT);
            }

            // --- LLM call ---
            AgentRequest request = buildRequest(state);
            AgentResponse response;

            try {
                response = provider.complete(request, apiKey.get());
            } catch (LlmProviderException e) {
                log.error("LLM call failed: {}", e.getMessage(), e);
                return failure(state, "LLM error: " + e.getMessage(), AgentResult.FailureType.LLM_ERROR);
            }

            state = state
                    .withNewMessage(response.getMessage())
                    .withTokens(response.getUsage());

            // --- Route response ---
            AgentMessage.AssistantMessage msg = response.getMessage();

            if (msg.isFinalAnswer()) {
                log.info("Agent finished iterations={} tokens={}", state.getIterations(), state.getTotalTokensUsed());

                // 5. Зберігаємо тільки НОВІ повідомлення (не всю history)
                List<AgentMessage> messages = state.getMessages().subList(historySize, state.getMessages().size());
                memoryManager.saveHistory(memoryConfig, context.getWorkflowId(), context.getAgentNodeId(), itemJson, messages);

                return new AgentResult.Success(
                        msg.text(),
                        state.getSteps(),
                        TokenUsage.builder()
                                .inputTokens((int) state.getTotalTokensUsed())
                                .outputTokens(0)
                                .build()
                );
            }

            if (msg.isToolCall()) {
                state = executeToolCalls(msg.toolCalls(), state, context);
                continue;
            }

            return failure(state, "Unexpected LLM response", AgentResult.FailureType.UNKNOWN);
        }
    }

    private AgentState executeToolCalls(
            List<ToolCallRequest> calls,
            AgentState state,
            ToolContext context
    ) {
        for (ToolCallRequest call : calls) {
            ToolExecutionResult result = pipeline.execute(call, context);

            AgentMessage.ToolResultMessage toolMsg = new AgentMessage.ToolResultMessage(
                    call.callId(),
                    call.toolName(),
                    result.getToolResult()
            );

            AgentStep step = new AgentStep.ToolStep(Instant.now(context.getClock()), result);
            state = state.withToolResult(toolMsg, step);
        }
        return state;
    }

    private AgentRequest buildRequest(AgentState state) {
        return AgentRequest.builder()
                .modelId(state.getModelId())
                .systemPrompt(state.getSystemPrompt())
                .messages(state.getMessages())
                .tools(toolRegistry.getDefinitions(List.copyOf(toolRegistry.getAllNames())))
                .build();
    }

    private AgentResult failure(AgentState state, String reason, AgentResult.FailureType type) {
//        meterRegistry.counter("agent.failure", "type", type.name()).increment();
        log.warn("Agent failed: {} type={}", reason, type);
        return new AgentResult.Failure(reason, type, state.getSteps());
    }
}
