//package com.workflow.sociallabs.node.nodes.ai.agent.llm.anthropic;
//
//import com.anthropic.client.AnthropicClient;
//import com.anthropic.client.okhttp.AnthropicOkHttpClient;
//import com.anthropic.models.messages.Message;
//import com.anthropic.models.messages.MessageCreateParams;
//import com.workflow.sociallabs.node.nodes.ai.agent.llm.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.Set;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public final class AnthropicLlmProvider implements LlmProvider<MessageCreateParams, Message> {
//
//    private final LlmClientCache cache;
//    private final AnthropicMessageMapper mapper;
//
//    private static final Set<String> SUPPORTED_PREFIXES = Set.of("claude-");
//
//    @Override
//    public ProviderType getProviderType() {
//        return ProviderType.ANTHROPIC;
//    }
//
//    @Override
//    public boolean supports(ModelId modelId) {
//        return SUPPORTED_PREFIXES.stream()
//                .anyMatch(prefix -> modelId.value().startsWith(prefix));
//    }
//// todo реалізувати інтерфейс для маперів щоб узагальнити тільки два методи
//    @Override
//    public AgentResponse complete(AgentRequest request, String apiKey) {
//        AnthropicClient client = cache.getOrCreate(
//                ProviderType.ANTHROPIC,
//                apiKey,
//                credential -> AnthropicOkHttpClient.builder().apiKey(credential).build(),
//                AnthropicClient.class
//        );
//
//        MessageCreateParams params = mapper.toParams(request);
//        Message raw = client.messages().create(params);
//        return mapper.fromMessage(raw);
//    }
//}
//
