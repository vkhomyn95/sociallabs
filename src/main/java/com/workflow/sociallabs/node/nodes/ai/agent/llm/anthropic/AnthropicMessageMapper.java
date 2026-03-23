package com.workflow.sociallabs.node.nodes.ai.agent.llm.anthropic;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentRequest;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentResponse;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.TokenUsage;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolCallRequest;
import io.jsonwebtoken.security.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public final class AnthropicMessageMapper {

    private final ObjectMapper objectMapper;

    public CreateMessageParams toParams(AgentRequest req) {
        return CreateMessageParams.builder()
                .model(req.getModelId().value())
                .system(req.getSystemPrompt())
                .maxTokens(req.getMaxTokens())
                .messages(req.getMessages().stream()
                        .filter(m -> m.role() != AgentMessage.Role.TOOL)
                        .map(this::toSdkMessage)
                        .toList())
                .tools(req.getTools().stream().map(this::toSdkTool).toList())
                .build();
    }

    public AgentResponse fromMessage(Message msg) {
        List<ToolCallRequest> calls = msg.content().stream()
                .filter(b -> b instanceof ToolUseBlock)
                .map(b -> (ToolUseBlock) b)
                .map(b -> new ToolCallRequest(
                        b.id(),
                        b.name(),
                        objectMapper.valueToTree(b.input())
                ))
                .toList();

        String text = msg.content().stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).text())
                .collect(Collectors.joining("\n"));

        return AgentResponse.builder()
                .message(new AgentMessage.AssistantMessage(
                        text.isBlank() ? null : text, calls))
                .usage(TokenUsage.builder()
                        .inputTokens(msg.usage().inputTokens())
                        .outputTokens(msg.usage().outputTokens())
                        .build())
                .stopReason(mapStopReason(msg.stopReason()))
                .build();
    }

    // ... toSdkMessage, toSdkTool — маппінг деталей SDK
}
