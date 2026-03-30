package com.workflow.sociallabs.node.nodes.http.auth;

import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import org.springframework.web.reactive.function.client.WebClient;


/** No-op: no authentication */
public class NoAuthStrategy implements HttpAuthStrategy {

    @Override
    public WebClient.RequestHeadersSpec<?> apply(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params
    ) {
        return spec;
    }
}
