package com.workflow.sociallabs.node.nodes.ai.agent.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Типобезпечний кеш LLM клієнтів.
 *
 * Ключ кешу: ProviderType + apiKey — гарантує що один apiKey
 * не переплутається між різними провайдерами.
 */
@Component
@Slf4j
public final class LlmClientCache {

    /**
     * Складений ключ: провайдер + apiKey.
     * record гарантує коректні equals/hashCode автоматично.
     */
    private record CacheKey(ProviderType provider, String apiKey) {

        CacheKey {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("apiKey must not be blank");
            }
        }

        @Override
        public String toString() {
            // Не логуємо повний ключ з міркувань безпеки
            return provider + ":" + apiKey.substring(0, Math.min(8, apiKey.length())) + "...";
        }
    }

    // Єдина мапа для всіх провайдерів — Object як значення,
    // але cast типобезпечний через LlmClientFactory
    private final ConcurrentHashMap<CacheKey, Object> cache = new ConcurrentHashMap<>();

    /**
     * Отримати або створити клієнт.
     *
     * @param provider тип провайдера
     * @param apiKey   API ключ
     * @param factory  функція створення нового клієнта (викликається один раз)
     * @param type     клас клієнта — для типобезпечного cast
     */
    public <C> C getOrCreate(
            ProviderType provider,
            String apiKey,
            Function<String, C> factory,
            Class<C> type) {

        CacheKey key = new CacheKey(provider, apiKey);

        Object client = cache.computeIfAbsent(key, k -> {
            log.info("*** Creating new LLM client for {}", k);
            return factory.apply(apiKey);
        });

        return type.cast(client);
    }

    /**
     * Інвалідувати конкретний клієнт — наприклад якщо ключ змінився.
     */
    public void invalidate(ProviderType provider, String apiKey) {
        CacheKey key = new CacheKey(provider, apiKey);
        Object removed = cache.remove(key);
        if (removed != null) {
            log.info("*** Invalidated LLM client for {}", key);
        }
    }

    /**
     * Інвалідувати всіх клієнтів конкретного провайдера.
     */
    public void invalidateAll(ProviderType provider) {
        cache.keySet().removeIf(k -> k.provider() == provider);
        log.info("*** Invalidated all LLM clients for provider {}", provider);
    }

    /**
     * Кількість закешованих клієнтів — для моніторингу.
     */
    public int size() {
        return cache.size();
    }
}
