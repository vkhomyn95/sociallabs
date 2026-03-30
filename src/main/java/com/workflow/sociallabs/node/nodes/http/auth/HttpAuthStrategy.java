package com.workflow.sociallabs.node.nodes.http.auth;

import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Стратегія авторизації для HTTP Request ноди.
 * Кожна реалізація модифікує WebClient.RequestHeadersSpec
 * додаючи відповідні заголовки або параметри.
 */
public interface HttpAuthStrategy {

    /**
     * @param spec  поточний WebClient request spec
     * @param params параметри ноди
     * @return той самий або новий spec з авторизацією
     */
    WebClient.RequestHeadersSpec<?> apply(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params
    );
}
