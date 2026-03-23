package com.workflow.sociallabs.node.nodes.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import com.workflow.sociallabs.node.nodes.ai.agent.AgentResult;
import com.workflow.sociallabs.node.nodes.ai.agent.AgentRunner;
import com.workflow.sociallabs.node.nodes.ai.agent.exception.AgentExecutionException;
import com.workflow.sociallabs.node.nodes.ai.agent.state.AgentState;
import com.workflow.sociallabs.node.nodes.ai.agent.state.AgentStep;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.parameters.AiAgentNodeParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.*;

/**
 * Executor для AI Agent ноди.
 * <p>
 * Реалізує ReAct (Reasoning + Acting) loop:
 * <p>
 * 1. Отримує user prompt (з вхідних даних або шаблону)
 * 2. Будує повідомлення: system + memory + user
 * 3. Викликає LLM
 * 4. Якщо LLM повертає tool calls → виконує tools → додає результати → repeat
 * 5. Якщо LLM дає фінальну відповідь → повертає результат
 * <p>
 * Інтеграція з Tools:
 * - Кожен tool — це нода у workflow (TELEGRAM_CLIENT_ACTION, etc.)
 * - AiToolExecutionService знаходить NodeExecutor і виконує ноду
 * - LLM отримує результат як tool_result і може вирішити ще раз викликати tool
 */
@Slf4j
@Component
public class AiAgentNodeExecutor extends AbstractNode {

    private final AgentRunner runner;
    private final Clock clock;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AiAgentNodeExecutor(AgentRunner runner) {
        super(NodeDiscriminator.AI_AGENT);
        this.runner = runner;
        this.clock = Clock.systemUTC();
    }

    @Override
    public boolean requiresCredentials() {
        return true; // потребує LLM API key
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) {
        AiAgentNodeParameters params = context.getParameters(AiAgentNodeParameters.class);
        params.validate();

        ToolContext toolContext = ToolContext.builder()
                .workflowId(context.getWorkflowId())
                .agentNodeId(context.getNodeId())
                .executionCorrelationId(UUID.randomUUID().toString())
                .sharedCredentials(context.getCredentials() != null ? context.getCredentials() : Map.of())
                .limits(params.getLimits())
                .clock(clock)
                .build();

        List<WorkflowItem> results = new ArrayList<>(context.getInputItems().size());

        for (WorkflowItem item : context.getInputItems()) {
            AgentState initial = AgentState.initial(
                    params.getModelId(),
                    params.getSystemPrompt(),
                    resolveUserInput(item, params),
                    params.getLimits()
            );

            AgentResult agentResult = runner.run(initial, toolContext, params.getMemory(), item.json());

            results.add(buildOutputItem(item, agentResult, params));
        }

        return NodeResult.success(results);
    }

    private String resolveUserInput(WorkflowItem item, AiAgentNodeParameters params) {
        String field = params.getInputField();
        if (field != null && !field.isBlank()) {
            Object val = item.json().get(field);
            if (val != null) return val.toString();
        }
        return item.json().toString();
    }

    private WorkflowItem buildOutputItem(WorkflowItem source, AgentResult result, AiAgentNodeParameters params) {
        Map<String, Object> output = new HashMap<>(source.json());
        String outputField = params.getOutputField();

        switch (result) {
            case AgentResult.Success s -> {
                // Structured output — parse JSON з відповіді
                if (params.isStructuredOutput()) {
                    try {
                        Map<String, Object> parsed = objectMapper.readValue(s.finalAnswer(), new TypeReference<>() {});
                        output.putAll(parsed);
                    } catch (JsonProcessingException e) {
                        output.put(outputField, s.finalAnswer());
                        output.put("_parseError", e.getMessage());
                    }
                } else {
                    output.put(outputField, s.finalAnswer());
                }

                if (params.isWithSteps()) {
                    output.put("steps", serializeSteps(s.steps()));
                    output.put("tokens", s.totalUsage().total());
                }
                output.put("success", true);
            }
            case AgentResult.Failure f -> {
                if (!params.isContinueOnFail()) {
                    throw new AgentExecutionException("Agent failed: " + f.reason(), f.failureType());
                }
                output.put("error", f.reason());
                output.put("failureType", f.failureType().name());
                output.put("success", false);
            }
        }

        return WorkflowItem.of(output);
    }

    private List<Map<String, Object>> serializeSteps(List<AgentStep> steps) {
        return steps.stream().map(step -> switch (step) {
            case AgentStep.ToolStep t -> Map.<String, Object>of(
                    "type", "tool",
                    "tool", t.result().getToolName(),
                    "success", t.result().getOutput().isSuccess(),
                    "timeMs", t.result().getExecutionTimeMs()
            );
            case AgentStep.LlmStep l -> Map.<String, Object>of(
                    "type", "llm",
                    "tokens", l.usage().total(),
                    "stop", l.stopReason().name()
            );
            case AgentStep.ErrorStep e -> Map.<String, Object>of(
                    "type", "error",
                    "message", e.message(),
                    "fatal", e.fatal()
            );
        }).toList();
    }
}