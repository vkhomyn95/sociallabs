package com.workflow.sociallabs.node.nodes.http.auth;

import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import org.springframework.web.reactive.function.client.WebClient;


/** Bearer Token: Authorization: Bearer <token> */
public class BearerAuthStrategy implements HttpAuthStrategy {

    @Override
    public WebClient.RequestHeadersSpec<?> apply(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params
    ) {
        if (params.getBearerToken() != null && !params.getBearerToken().isBlank()) {
            return spec.header("Authorization", "Bearer " + params.getBearerToken());
        }
        return spec;
    }
}
