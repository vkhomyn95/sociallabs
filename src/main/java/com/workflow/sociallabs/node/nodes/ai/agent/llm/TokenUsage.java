package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenUsage {
    int inputTokens;
    int outputTokens;

    public int total() {
        return inputTokens + outputTokens;
    }
}
