package com.workflow.sociallabs.node.nodes.ai.agent.llm.openai;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class OpenAiMessageMapper {

    private final ObjectMapper objectMapper;

    public CreateChatCompletionRequest toSdkRequest(AgentRequest req) {
        return CreateChatCompletionRequest.builder()
                .model(req.getModelId().value())
                .temperature(req.getTemperature())
                .maxTokens(req.getMaxTokens())
                .messages(buildMessages(req))
                .tools(buildTools(req.getTools()))
                .toolChoice(req.getTools().isEmpty()
                        ? ToolChoiceOption.ofAuto()    // none якщо немає tools
                        : ToolChoiceOption.ofAuto())
                .build();
    }

    private List<ChatCompletionMessageParam> buildMessages(AgentRequest req) {
        // System message — окремий в OpenAI
        List<ChatCompletionMessageParam> result = new ArrayList<>();
        result.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(req.getSystemPrompt())
                        .build()
        ));

        for (AgentMessage msg : req.getMessages()) {
            result.add(toSdkMessage(msg));
        }
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
                var builder = ChatCompletionAssistantMessageParam.builder();
                if (a.getText() != null) builder.content(a.getText());
                if (a.isToolCall()) {
                    builder.toolCalls(a.getToolCalls().stream()
                            .map(this::toSdkToolCall)
                            .toList());
                }
                yield ChatCompletionMessageParam.ofAssistant(builder.build());
            }

            case AgentMessage.ToolResultMessage t ->
                    ChatCompletionMessageParam.ofTool(
                            ChatCompletionToolMessageParam.builder()
                                    .toolCallId(t.toolCallId())
                                    .content(serializeToolOutput(t.result()))
                                    .build()
                    );
        };
    }

    private ChatCompletionMessageToolCall toSdkToolCall(ToolCallRequest req) {
        return ChatCompletionMessageToolCall.builder()
                .id(req.callId())
                .type(ChatCompletionMessageToolCall.Type.FUNCTION)
                .function(ChatCompletionMessageToolCall.Function.builder()
                        .name(req.toolName())
                        .arguments(req.rawArguments().toString())  // JsonNode → String
                        .build())
                .build();
    }

    private List<ChatCompletionTool> buildTools(List<ToolDefinition> tools) {
        return tools.stream()
                .map(t -> ChatCompletionTool.builder()
                        .type(ChatCompletionTool.Type.FUNCTION)
                        .function(FunctionDefinition.builder()
                                .name(t.name())
                                .description(t.description())
                                .parameters(schemaToJsonNode(t.schema()))
                                .build())
                        .build())
                .toList();
    }

    // ── fromCompletion ─────────────────────────────────────────

    public AgentResponse fromCompletion(ChatCompletion completion) {
        ChatCompletionChoice choice = completion.choices().get(0);
        ChatCompletionMessage message = choice.message();

        // Tool calls
        List<ToolCallRequest> toolCalls = Optional
                .ofNullable(message.toolCalls())
                .orElse(List.of())
                .stream()
                .map(tc -> new ToolCallRequest(
                        tc.id(),
                        tc.function().name(),
                        parseArguments(tc.function().arguments())  // String → JsonNode
                ))
                .toList();

        // Text content
        String text = message.content().orElse(null);

        return AgentResponse.builder()
                .message(new AgentMessage.AssistantMessage(
                        (text != null && !text.isBlank()) ? text : null,
                        toolCalls
                ))
                .usage(TokenUsage.builder()
                        .inputTokens((int) completion.usage().promptTokens())
                        .outputTokens((int) completion.usage().completionTokens())
                        .build())
                .stopReason(mapFinishReason(choice.finishReason()))
                .build();
    }

    private JsonNode parseArguments(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            // Fallback — обгортаємо як raw string
            return objectMapper.createObjectNode().put("_raw", json);
        }
    }

    private AgentResponse.StopReason mapFinishReason(
            ChatCompletionChoice.FinishReason reason) {
        return switch (reason) {
            case TOOL_CALLS  -> AgentResponse.StopReason.TOOL_USE;
            case STOP        -> AgentResponse.StopReason.END_TURN;
            case LENGTH      -> AgentResponse.StopReason.MAX_TOKENS;
            default          -> AgentResponse.StopReason.END_TURN;
        };
    }

    private String serializeToolOutput(ToolOutput output) {
        return switch (output) {
            case ToolOutput.Success s ->  {
                try { yield objectMapper.writeValueAsString(s.data()); }
                catch (JsonProcessingException e) { yield s.data().toString(); }
            }
            case ToolOutput.Failure f ->
                    "{\"error\": \"" + f.errorCode() + "\", \"message\": \"" + f.errorMessage() + "\"}";
        };
    }

    private JsonNode schemaToJsonNode(ToolSchema schema) {
        // Конвертуємо ToolSchema → OpenAI JSON Schema format
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");

        ObjectNode props = objectMapper.createObjectNode();
        for (ToolSchema.ToolParameter param : schema.getParameters()) {
            ObjectNode p = objectMapper.createObjectNode();
            p.put("type", param.getType().name().toLowerCase());
            p.put("description", param.getDescription());
            if (param.getDefaultValue() != null) {
                p.put("default", param.getDefaultValue().toString());
            }
            props.set(param.getName(), p);
        }
        root.set("properties", props);

        ArrayNode required = objectMapper.createArrayNode();
        schema.getRequired().forEach(required::add);
        root.set("required", required);

        return root;
    }
}
