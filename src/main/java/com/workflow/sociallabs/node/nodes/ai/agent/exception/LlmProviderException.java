package com.workflow.sociallabs.node.nodes.ai.agent.exception;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.ProviderType;

public class LlmProviderException extends RuntimeException {

    private final ProviderType providerType;
    private final int          httpStatus;   // 0 якщо не HTTP помилка
    private final boolean      retryable;

    public LlmProviderException(
            String message,
            ProviderType providerType,
            int httpStatus,
            boolean retryable) {
        super(message);
        this.providerType = providerType;
        this.httpStatus   = httpStatus;
        this.retryable    = retryable;
    }

    public LlmProviderException(
            String message,
            ProviderType providerType,
            int httpStatus,
            boolean retryable,
            Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
        this.httpStatus   = httpStatus;
        this.retryable    = retryable;
    }

    public ProviderType getProviderType() { return providerType; }
    public int          getHttpStatus()   { return httpStatus; }
    public boolean      isRetryable()     { return retryable; }

    // Фабричні методи для типових помилок
    public static LlmProviderException rateLimited(ProviderType type) {
        return new LlmProviderException(
                "Rate limit exceeded", type, 429, true);
    }

    public static LlmProviderException unauthorized(ProviderType type) {
        return new LlmProviderException(
                "Invalid API key or unauthorized", type, 401, false);
    }

    public static LlmProviderException serverError(ProviderType type, int status) {
        return new LlmProviderException(
                "Provider server error: " + status, type, status, status >= 500);
    }

    public static LlmProviderException timeout(ProviderType type) {
        return new LlmProviderException(
                "Request timed out", type, 0, true);
    }
}
