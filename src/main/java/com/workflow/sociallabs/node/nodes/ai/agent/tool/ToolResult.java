package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;

import java.util.Map;

public sealed interface ToolResult
        permits ToolResult.Ok, ToolResult.Err {

    boolean isSuccess();

    /** Те що LLM побачить як tool_result content */
    String toLlmContent();

    record Ok(
            @NonNull Map<String, Object> data
    ) implements ToolResult {
        public boolean isSuccess() { return true; }

        public String toLlmContent() {
            try {
                return new ObjectMapper().writeValueAsString(data);
            } catch (JsonProcessingException e) {
                return data.toString();
            }
        }

        public static Ok of(Map<String, Object> data) { return new Ok(data); }
    }

    record Err(
            @NonNull String errorCode,
            @NonNull String errorMessage,
            boolean retryable
    ) implements ToolResult {
        public boolean isSuccess() { return false; }

        public String toLlmContent() {
            return "{\"error\":\"" + errorCode + "\",\"message\":\"" + errorMessage + "\"}";
        }
    }
}
