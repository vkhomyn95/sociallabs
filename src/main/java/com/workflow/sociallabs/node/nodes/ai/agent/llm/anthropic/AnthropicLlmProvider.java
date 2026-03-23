package com.workflow.sociallabs.node.nodes.ai.agent.llm.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentRequest;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentResponse;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.LlmProvider;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.ModelId;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.ProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public final class AnthropicLlmProvider implements LlmProvider<CreateMessageParams, Message> {

    private final AnthropicClient client;  // Anthropic Java SDK
    private final AnthropicMessageMapper mapper;

    private static final Set<String> SUPPORTED_PREFIXES = Set.of("claude-");

    @Override
    public ProviderType getProviderType() {
        return ProviderType.ANTHROPIC;
    }

    @Override
    public boolean supports(ModelId modelId) {
        return SUPPORTED_PREFIXES.stream().anyMatch(prefix -> modelId.value().startsWith(prefix));
    }

    @Override
    public AgentResponse complete(AgentRequest request) {
        CreateMessageParams params = mapper.toParams(request);
        Message raw = client.messages().create(params);
        return mapper.fromMessage(raw);
    }
}

