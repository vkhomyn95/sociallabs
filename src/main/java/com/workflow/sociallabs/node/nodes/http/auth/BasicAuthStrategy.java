package com.workflow.sociallabs.node.nodes.http.auth;

import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Base64;
import java.nio.charset.StandardCharsets;


/** Basic Authentication: Authorization: Basic base64(user:pass) */
public class BasicAuthStrategy implements HttpAuthStrategy {

    @Override
    public WebClient.RequestHeadersSpec<?> apply(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params
    ) {
        String credentials = params.getBasicUsername() + ":" + params.getBasicPassword();
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return spec.header("Authorization", "Basic " + encoded);
    }
}
