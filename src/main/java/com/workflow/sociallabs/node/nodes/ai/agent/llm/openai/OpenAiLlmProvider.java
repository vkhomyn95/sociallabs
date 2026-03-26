package com.workflow.sociallabs.node.nodes.ai.agent.llm.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public final class OpenAiLlmProvider implements LlmProvider<ChatCompletionCreateParams, ChatCompletion> {

    private final LlmClientCache cache;
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
    public AgentResponse complete(AgentRequest request, String apiKey) {
        OpenAIClient client = cache.getOrCreate(
                ProviderType.OPENAI,
                apiKey,
                credential -> OpenAIOkHttpClient.builder().apiKey(credential).build(),
                OpenAIClient.class
        );

        ChatCompletionCreateParams params = mapper.toSdkRequest(request);
        ChatCompletion completion = client.chat().completions().create(params);
        return mapper.fromCompletion(completion);
    }
}
