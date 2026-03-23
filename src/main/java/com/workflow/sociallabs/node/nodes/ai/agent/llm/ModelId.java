package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import java.util.Objects;

public record ModelId(String value) {

    public static final ModelId CLAUDE_SONNET_4   = new ModelId("claude-sonnet-4-20250514");
    public static final ModelId GPT_4O            = new ModelId("gpt-4o");
    public static final ModelId GEMINI_1_5_PRO    = new ModelId("gemini-1.5-pro");

    public ModelId {
        Objects.requireNonNull(value, "ModelId value must not be null");
        if (value.isBlank()) throw new IllegalArgumentException("ModelId must not be blank");
    }

    public static ModelId of(String value) { return new ModelId(value); }
}
