package com.workflow.sociallabs.node.nodes.ai.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentLimits {
    @Builder.Default
    int maxIterations = 8;
    @Builder.Default
    int maxToolCalls = 12;
    @Builder.Default
    long maxTotalTokens = 100_000;
    @Builder.Default
    long timeoutMs = 30_000;
    @Builder.Default
    int maxConsecutiveErrors = 2;
}
