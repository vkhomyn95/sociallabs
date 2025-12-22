package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Базовий клас для HTTP-нод
 * Надає спільну функціональність для роботи з HTTP
 */
@Slf4j
public abstract class AbstractHttpNode extends AbstractActionNode {

    protected AbstractHttpNode(NodeDiscriminator nodeType) {
        super(nodeType);
    }

    /**
     * Виконати HTTP запит
     */
    protected abstract Map<String, Object> executeHttpRequest(
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception;

    @Override
    protected Map<String, Object> processItem(Map<String, Object> item, ExecutionContext context) throws Exception {
        return executeHttpRequest(item, context);
    }

    /**
     * Побудувати URL з параметрів
     */
    protected String buildUrl(String baseUrl, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder url = new StringBuilder(baseUrl);
        url.append(baseUrl.contains("?") ? "&" : "?");

        queryParams.forEach((key, value) -> {
            url.append(key).append("=").append(urlEncode(value)).append("&");
        });

        // Remove trailing &
        url.setLength(url.length() - 1);
        return url.toString();
    }

    /**
     * URL encode значення
     */
    protected String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Обробити відповідь від API
     */
    protected Map<String, Object> processResponse(
            int statusCode,
            String responseBody,
            Map<String, String> headers
    ) throws Exception {

        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", statusCode);
        result.put("headers", headers);

        // Парсинг JSON якщо можливо
        if (headers.getOrDefault("content-type", "").contains("application/json")) {
            try {
                // Тут має бути парсинг JSON через Jackson або інший парсер
                result.put("body", responseBody); // Simplified
                result.put("json", parseJson(responseBody));
            } catch (Exception e) {
                result.put("body", responseBody);
            }
        } else {
            result.put("body", responseBody);
        }

        // Перевірка на помилки
        if (statusCode >= 400) {
            throw new RuntimeException("HTTP request failed with status " + statusCode + ": " + responseBody);
        }

        return result;
    }

    /**
     * Парсинг JSON - має використовувати Jackson
     */
    protected Map<String, Object> parseJson(String json) throws Exception {
        // Simplified - should use Jackson ObjectMapper
        return new HashMap<>();
    }
}