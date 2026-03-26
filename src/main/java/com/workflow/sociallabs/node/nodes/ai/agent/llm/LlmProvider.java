package com.workflow.sociallabs.node.nodes.ai.agent.llm;

/**
 * Строго-параметризований провайдер LLM.
 * REQ / RESP — нативні типи конкретного SDK (не Map<String,Object>).
 * Конвертація з/до AgentMessage відбувається в реалізації.
 */
public interface LlmProvider<REQ, RESP> {

    /** Ідентифікатор провайдера (anthropic, openai, google) */
    ProviderType getProviderType();

    /** Чи підтримує даний modelId цей провайдер */
    boolean supports(ModelId modelId);

    /** Основний виклик — повертає строго-типізований AgentResponse */
    AgentResponse complete(AgentRequest request, String apiKey);

    /** Стрімінг (опційно) */
//    default Flux<AgentResponseChunk> stream(AgentRequest request) {
//        throw new UnsupportedOperationException("Streaming not supported by " + getProviderType());
//    }
}
