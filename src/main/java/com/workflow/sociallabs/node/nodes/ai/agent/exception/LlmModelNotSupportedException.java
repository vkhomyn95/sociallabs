package com.workflow.sociallabs.node.nodes.ai.agent.exception;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.ModelId;

public class LlmModelNotSupportedException extends RuntimeException {

    private final ModelId modelId;

    public LlmModelNotSupportedException(ModelId modelId) {
        super("No LLM provider supports model: '" + modelId.value() + "'. Register a LlmProvider bean that handles this model.");
        this.modelId = modelId;
    }

    public ModelId getModelId() { return modelId; }
}
