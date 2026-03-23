package com.workflow.sociallabs.node.nodes.ai.agent.llm.openai;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentRequest;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentResponse;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.ModelId;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.ProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public final class OpenAiLlmProvider implements LlmProvider<CreateChatCompletionRequest, ChatCompletion> {

    private final OpenAIClient client;    // OpenAI Java SDK (официальний)
    private final OpenAiMessageMapper mapper;

    private static final Set<String> SUPPORTED_PREFIXES = Set.of("gpt-4", "gpt-3.5", "o1", "o3");

    @Override
    public ProviderType getProviderType() { return ProviderType.OPENAI; }

    @Override
    public boolean supports(ModelId modelId) {
        return SUPPORTED_PREFIXES.stream()
                .anyMatch(p -> modelId.value().startsWith(p));
    }

    @Override
    public AgentResponse complete(AgentRequest request) {
        CreateChatCompletionRequest sdkRequest = mapper.toSdkRequest(request);

        log.debug("OpenAI request model={} tools={} messages={}",
                request.getModelId().value(),
                request.getTools().size(),
                request.getMessages().size());

        ChatCompletion completion = client.chat().completions().create(sdkRequest);

        return mapper.fromCompletion(completion);
    }
}
