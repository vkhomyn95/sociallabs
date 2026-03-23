package com.workflow.sociallabs.node.nodes.ai.agent.tool;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ToolExecutionResult {
    @NonNull String callId;
    @NonNull String toolName;
    @NonNull JsonNode input;
    @NonNull ToolOutput output;      // повний — для метрик, логів, pipeline
    @NonNull ToolResult toolResult;  // для LLM context (AgentMessage.ToolResultMessage)
    long executionTimeMs;
}
