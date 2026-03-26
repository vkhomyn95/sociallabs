package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolCallRequest;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolResult;
import lombok.NonNull;

import java.util.List;

public sealed interface AgentMessage permits
        AgentMessage.UserMessage,
        AgentMessage.AssistantMessage,
        AgentMessage.ToolResultMessage {

    Role role();

    record UserMessage(
            @NonNull String content
    ) implements AgentMessage {
        public Role role() {
            return Role.USER;
        }
    }

    record AssistantMessage(
            String text,                      // null якщо тільки tool_call
            List<ToolCallRequest> toolCalls   // empty якщо text answer
    ) implements AgentMessage {
        public Role role() {
            return Role.ASSISTANT;
        }

        public boolean isToolCall() {
            return toolCalls != null && !toolCalls.isEmpty();
        }

        public boolean isFinalAnswer() {
            return text != null && !text.isBlank() && !isToolCall();
        }

        @Override
        public String text() {
            return text;
        }

        @Override
        public List<ToolCallRequest> toolCalls() {
            return toolCalls;
        }
    }

    record ToolResultMessage(
            @NonNull String toolCallId,
            @NonNull String toolName,
            @NonNull ToolResult result
            ) implements AgentMessage {
        public Role role() {
            return Role.TOOL;
        }
    }

    enum Role {USER, ASSISTANT, TOOL}
}
