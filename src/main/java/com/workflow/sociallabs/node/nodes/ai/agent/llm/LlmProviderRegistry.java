package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class LlmProviderRegistry {

    private final List<LlmProvider<?, ?>> providers;

    public LlmProviderRegistry(List<LlmProvider<?, ?>> providers) {
        this.providers = List.copyOf(providers);
    }

    public LlmProvider<?, ?> forModel(ModelId modelId) {
        return providers.stream()
                .filter(p -> p.supports(modelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No LLM provider supports model: " + modelId.value()));
    }
}
