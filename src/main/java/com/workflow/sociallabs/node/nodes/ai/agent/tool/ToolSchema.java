package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ToolSchema {

    @NonNull String name;
    @NonNull String description;
    @NonNull
    @Singular
    List<ToolParameter> parameters;  // ← @Singular генерує .parameter()
    @NonNull
    @Singular("required")
    List<String> required;

    @Value
    @Builder
    public static class ToolParameter {
        @NonNull String name;
        @NonNull ParameterType type;
        @NonNull String description;
        @Nullable
        Object defaultValue;

        public enum ParameterType {STRING, NUMBER, BOOLEAN, ARRAY, OBJECT}
    }
}