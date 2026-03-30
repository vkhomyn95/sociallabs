package com.workflow.sociallabs.node.nodes.http.auth;

import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import org.springframework.web.reactive.function.client.WebClient;


/** Header Authentication: adds arbitrary header */
public class HeaderAuthStrategy implements HttpAuthStrategy {

    @Override
    public WebClient.RequestHeadersSpec<?> apply(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params
    ) {
        String name = params.getHeaderAuthName();
        String value = params.getHeaderAuthValue();
        if (name != null && !name.isBlank()) {
            return spec.header(params.isLowercaseHeaders() ? name.toLowerCase() : name, value);
        }
        return spec;
    }
}
