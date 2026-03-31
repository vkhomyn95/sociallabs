//package com.workflow.sociallabs.node.nodes.ai.agent.llm.anthropic;
//
//import com.anthropic.models.messages.*;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
//import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentRequest;
//import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentResponse;
//import com.workflow.sociallabs.node.nodes.ai.agent.llm.TokenUsage;
//import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolCallRequest;
//import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolDefinition;
//import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import static com.anthropic.models.messages.StopReason.MAX_TOKENS;
//import static com.anthropic.models.messages.StopReason.TOOL_USE;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public final class AnthropicMessageMapper {
//
//    private final ObjectMapper objectMapper;
//
//    public MessageCreateParams toParams(AgentRequest req) {
//        MessageCreateParams.Builder builder = MessageCreateParams.builder()
//                .model(req.getModelId().value())
//                .maxTokens(req.getMaxTokens())
//                .temperature(req.getTemperature())
//                .messages(buildMessages(req));
//
//        // System prompt в Anthropic передається окремим полем
//        if (!req.getSystemPrompt().isBlank()) {
//            builder.system(req.getSystemPrompt());
//        }
//
//        List<Tool> tools = buildTools(req);
//        if (!tools.isEmpty()) {
//            builder.tools(tools);
//        }
//
//        return builder.build();
//    }
//
//    private List<MessageParam> buildMessages(AgentRequest req) {
//        return req.getMessages().stream()
//                .map(this::toMessageParam)
//                .toList();
//    }
//
//    private MessageParam toMessageParam(AgentMessage msg) {
//        return switch (msg) {
//
//            case AgentMessage.UserMessage u ->
//                    MessageParam.builder()
//                            .role(MessageParam.Role.USER)
//                            .content(MessageParam.Content.ofString(u.content()))
//                            .build();
//
//            case AgentMessage.AssistantMessage a -> {
//                List<ContentBlockParam> blocks = new ArrayList<>();
//
//                if (a.text() != null) {
//                    blocks.add(
//                            // Виправлено: ofText замість ofTextBlockParam
//                            ContentBlockParam.ofText(
//                                    TextBlockParam.builder()
//                                            .text(a.text())
//                                            .build()
//                            )
//                    );
//                }
//
//                if (a.isToolCall()) {
//                    a.toolCalls().forEach(tc ->
//                            blocks.add(
//                                    // Виправлено: ofToolUse замість ofToolUseBlockParam
//                                    ContentBlockParam.ofToolUse(
//                                            ToolUseBlockParam.builder()
//                                                    .id(tc.callId())
//                                                    .name(tc.toolName())
//                                                    .input(com.anthropic.core.JsonValue.from(tc.rawArguments()))
//                                                    .build()
//                                    )
//                            )
//                    );
//                }
//
//                yield MessageParam.builder()
//                        .role(MessageParam.Role.ASSISTANT)
//                        .content(MessageParam.Content.ofBlockParams(blocks))
//                        .build();
//            }
//
//            case AgentMessage.ToolResultMessage t ->
//                    MessageParam.builder()
//                            .role(MessageParam.Role.USER)
//                            .content(MessageParam.Content.ofBlockParams(List.of(
//                                    // Виправлено: ofToolResult замість ofToolResultBlockParam
//                                    ContentBlockParam.ofToolResult(
//                                            ToolResultBlockParam.builder()
//                                                    .toolUseId(t.toolCallId())
//                                                    .content(
//                                                            ToolResultBlockParam.Content.ofString(
//                                                                    t.result().toLlmContent()
//                                                            )
//                                                    )
//                                                    .build()
//                                    )
//                            )))
//                            .build();
//        };
//    }
//
//    private List<Tool> buildTools(AgentRequest req) {
//        return req.getTools().stream()
//                .map(this::toTool)
//                .toList();
//    }
//
//    private Tool toTool(ToolDefinition def) {
//        return Tool.builder()
//                .name(def.getName())
//                .description(def.getDescription())
//                .inputSchema(buildInputSchema(def.getSchema()))
//                .build();
//    }
//
//    private Tool.InputSchema buildInputSchema(ToolSchema schema) {
//        Map<String, Object> properties = new LinkedHashMap<>();
//        schema.getParameters().forEach(p -> {
//            Map<String, Object> prop = new LinkedHashMap<>();
//            prop.put("type", p.getType().name().toLowerCase());
//            prop.put("description", p.getDescription());
//            if (p.getDefaultValue() != null) {
//                prop.put("default", p.getDefaultValue());
//            }
//            properties.put(p.getName(), prop);
//        });
//
//        // Формуємо JSON-схему через dynamic properties
//        return Tool.InputSchema.builder()
//                .putAdditionalProperty("type", "object")
//                .putAdditionalProperty("properties", properties)
//                .putAdditionalProperty("required", schema.getRequired())
//                .build();
//    }
//
//    public AgentResponse fromMessage(Message msg) {
//        // У версії 2.x msg.content() — це List<ContentBlock>
//        List<ToolCallRequest> toolCalls = msg.content().stream()
//                .filter(ContentBlock::isToolUse)
//                .map(ContentBlock::asToolUse)
//                .map(b -> new ToolCallRequest(
//                        b.id(),
//                        b.name(),
//                        // b.input() повертає JsonValue, перетворюємо на Jackson JsonNode
//                        parseAnthropicJson(b._input())
//                ))
//                .toList();
//
//        String text = msg.content().stream()
//                .filter(ContentBlock::isText)
//                .map(b -> b.asText().text())
//                .collect(Collectors.joining("\n"))
//                .trim();
//
//        return AgentResponse.builder()
//                .message(new AgentMessage.AssistantMessage(
//                        text.isBlank() ? null : text,
//                        toolCalls
//                ))
//                .usage(TokenUsage.builder()
//                        .inputTokens(Math.toIntExact(msg.usage().inputTokens()))
//                        .outputTokens(Math.toIntExact(msg.usage().outputTokens()))
//                        .build())
//                .stopReason(mapStopReason(msg.stopReason().orElse(null)))
//                .build();
//    }
//
//    private JsonNode parseAnthropicJson(com.anthropic.core.JsonValue val) {
//        try {
//            // Anthropic JsonValue має прямий доступ до внутрішнього представлення
//            return objectMapper.readTree(val.toString());
//        } catch (Exception e) {
//            return objectMapper.createObjectNode();
//        }
//    }
//
//    private AgentResponse.StopReason mapStopReason(StopReason reason) {
//        if (reason == null) return AgentResponse.StopReason.END_TURN;
//        return switch (reason) {
//            case TOOL_USE -> AgentResponse.StopReason.TOOL_USE;
//            case MAX_TOKENS -> AgentResponse.StopReason.MAX_TOKENS;
//            default -> AgentResponse.StopReason.END_TURN;
//        };
//    }
//}
