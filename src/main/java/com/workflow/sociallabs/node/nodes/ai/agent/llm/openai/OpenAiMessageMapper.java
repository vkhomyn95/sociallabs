package com.workflow.sociallabs.node.nodes.ai.agent.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption.Auto;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentRequest;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentResponse;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.TokenUsage;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolCallRequest;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolDefinition;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public final class OpenAiMessageMapper {

    private final ObjectMapper objectMapper;

    // ── toSdkRequest ────────────────────────────────────────────

    public ChatCompletionCreateParams toSdkRequest(AgentRequest req) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(req.getModelId().value())
                .temperature(req.getTemperature())
                .maxCompletionTokens(req.getMaxTokens())
                .messages(buildMessages(req));

        // tools — тільки якщо є, інакше не передаємо
        List<ChatCompletionTool> tools = buildTools(req.getTools());
        if (!tools.isEmpty()) {
            builder.tools(tools);
            // "auto" — LLM сам вирішує коли викликати tool
            builder.toolChoice(
                    ChatCompletionToolChoiceOption.ofAuto(Auto.AUTO)
            );
        }

        return builder.build();
    }

    // ── Messages ────────────────────────────────────────────────

    private List<ChatCompletionMessageParam> buildMessages(AgentRequest req) {
        List<ChatCompletionMessageParam> result = new ArrayList<>();

        // System message завжди перший
        result.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(req.getSystemPrompt())
                        .build()
        ));

        req.getMessages().forEach(msg -> result.add(toSdkMessage(msg)));
        return result;
    }

    private ChatCompletionMessageParam toSdkMessage(AgentMessage msg) {
        return switch (msg) {

            case AgentMessage.UserMessage u ->
                    ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                    .content(u.content())
                                    .build()
                    );

            case AgentMessage.AssistantMessage a -> {
                ChatCompletionAssistantMessageParam.Builder b =
                        ChatCompletionAssistantMessageParam.builder();

                if (a.text() != null) {
                    b.content(a.text());
                }
                if (a.isToolCall()) {
                    b.toolCalls(a.toolCalls().stream()
                            .map(this::toSdkToolCall)
                            .toList());
                }
                yield ChatCompletionMessageParam.ofAssistant(b.build());
            }

            case AgentMessage.ToolResultMessage t ->
                    ChatCompletionMessageParam.ofTool(
                            ChatCompletionToolMessageParam.builder()
                                    .toolCallId(t.toolCallId())
                                    .content(t.result().toLlmContent())  // ToolResult має toLlmContent()
                                    .build()
                    );
        };
    }

    private ChatCompletionMessageToolCall toSdkToolCall(ToolCallRequest req) {
        return ChatCompletionMessageToolCall.ofFunction(
                ChatCompletionMessageFunctionToolCall.builder()
                        .id(req.callId())
                        .function(
                                ChatCompletionMessageFunctionToolCall.Function.builder()
                                        .name(req.toolName())
                                        .arguments(req.rawArguments().toString())
                                        .build()
                        )
                        .build()
        );
    }

    // ── Tools ───────────────────────────────────────────────────

    private List<ChatCompletionTool> buildTools(List<ToolDefinition> tools) {
        return tools.stream()
                .map(t -> ChatCompletionTool.ofFunction(
                        ChatCompletionFunctionTool.builder()
                                .function(
                                        FunctionDefinition.builder()
                                                .name(t.getName())
                                                .description(t.getDescription())
                                                .parameters(schemaToFunctionParameters(t.getSchema()))
                                                .build()
                                )
                                .build()
                ))
                .toList();
    }

    /**
     * ToolSchema → FunctionParameters (OpenAI SDK тип)
     */
    private FunctionParameters schemaToFunctionParameters(ToolSchema schema) {
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.getParameters().forEach(p -> {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("type",        p.getType().name().toLowerCase());
            prop.put("description", p.getDescription());
            if (p.getDefaultValue() != null) {
                prop.put("default", p.getDefaultValue());
            }
            properties.put(p.getName(), prop);
        });

        return FunctionParameters.builder()
                .putAdditionalProperty("type",       JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required",   JsonValue.from(schema.getRequired()))
                .build();
    }

    // ── fromCompletion ──────────────────────────────────────────

    public AgentResponse fromCompletion(ChatCompletion completion) {
        ChatCompletion.Choice choice  = completion.choices().get(0);
        ChatCompletionMessage message = choice.message();

        List<ToolCallRequest> toolCalls = message.toolCalls()
                .orElse(List.of())
                .stream()
                // Перевіряємо, що це саме виклик функції
                .filter(ChatCompletionMessageToolCall::isFunction)
                .map(tc -> {
                    // Переходимо до конкретного типу ChatCompletionMessageFunctionToolCall
                    var functionCall = tc.asFunction();
                    return new ToolCallRequest(
                            functionCall.id(),
                            functionCall.function().name(),
                            parseArguments(functionCall.function().arguments())
                    );
                })
                .toList();

        String text = message.content().orElse(null);

        return AgentResponse.builder()
                .message(new AgentMessage.AssistantMessage(
                        (text != null && !text.isBlank()) ? text : null,
                        toolCalls
                ))
                .usage(TokenUsage.builder()
                        .inputTokens(completion.usage()
                                .map(u -> (int) u.promptTokens())
                                .orElse(0))
                        .outputTokens(completion.usage()
                                .map(u -> (int) u.completionTokens())
                                .orElse(0))
                        .build())
                .stopReason(mapFinishReason(choice.finishReason()))
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────

    private JsonNode parseArguments(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode().put("_raw", json);
        }
    }

    private AgentResponse.StopReason mapFinishReason(ChatCompletion.Choice.FinishReason reason) {
        return switch (reason) {
            case ChatCompletion.Choice.FinishReason.TOOL_CALLS -> AgentResponse.StopReason.TOOL_USE;
            case ChatCompletion.Choice.FinishReason.LENGTH     -> AgentResponse.StopReason.MAX_TOKENS;
            default         -> AgentResponse.StopReason.END_TURN;
        };
    }
}
