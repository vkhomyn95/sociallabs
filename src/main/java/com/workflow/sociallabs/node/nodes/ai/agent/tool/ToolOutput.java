package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import jakarta.annotation.Nullable;
import lombok.NonNull;

import java.util.Map;

public sealed interface ToolOutput permits ToolOutput.Success, ToolOutput.Failure {

    boolean isSuccess();

    record Success(
            @NonNull Map<String, Object> data,
            @Nullable String humanReadableSummary
    ) implements ToolOutput {
        public boolean isSuccess() { return true; }

        public static Success of(Map<String, Object> data) {
            return new Success(data, null);
        }
    }

    record Failure(
            @NonNull String errorCode,
            @NonNull String errorMessage,
            boolean retryable
    ) implements ToolOutput {
        public boolean isSuccess() { return false; }
    }
}
