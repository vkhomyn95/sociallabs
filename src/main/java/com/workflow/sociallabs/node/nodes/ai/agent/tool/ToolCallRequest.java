package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import lombok.NonNull;
import com.fasterxml.jackson.databind.JsonNode;

public record ToolCallRequest(
        @NonNull String callId,    // унікальний ID від LLM
        @NonNull String toolName,
        @NonNull JsonNode rawArguments  // Jackson JsonNode — строго структурований
) {}
