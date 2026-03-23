package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolDefinition;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value   // Lombok immutable
@Builder
public class AgentRequest {
    @NonNull ModelId modelId;
    @NonNull String systemPrompt;
    @NonNull List<AgentMessage> messages;
    @NonNull List<ToolDefinition> tools;
    @Builder.Default
    double temperature = 0.7;
    @Builder.Default
    int maxTokens = 4096;
}
