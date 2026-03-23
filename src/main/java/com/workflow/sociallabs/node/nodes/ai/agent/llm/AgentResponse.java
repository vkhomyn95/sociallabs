package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AgentResponse {
    @NonNull AgentMessage.AssistantMessage message;
    @NonNull  TokenUsage                    usage;
    @NonNull  StopReason                    stopReason;

    public enum StopReason { TOOL_USE, END_TURN, MAX_TOKENS, STOP_SEQUENCE }
}
