package com.workflow.sociallabs.node.nodes.http;

import com.workflow.sociallabs.node.nodes.http.auth.*;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class HttpRequestService {

    private final WebClient webClient;
    private final WebClient insecureWebClient;
    private final HttpRequestBuilder requestBuilder;
    private final HttpResponseParser responseParser;
    private final PaginationHandler paginationHandler;

    private static final Map<HttpAuthType, HttpAuthStrategy> AUTH_STRATEGIES = Map.of(
            HttpAuthType.NONE,   new NoAuthStrategy(),
            HttpAuthType.BASIC,  new BasicAuthStrategy(),
            HttpAuthType.HEADER, new HeaderAuthStrategy(),
            HttpAuthType.BEARER, new BearerAuthStrategy(),
            HttpAuthType.QUERY,  new NoAuthStrategy()
    );

    // Явний конструктор з @Qualifier — єдиний спосіб без @Primary
    public HttpRequestService(
            @Qualifier("httpNodeWebClient")       WebClient webClient,
            @Qualifier("insecureHttpNodeWebClient") WebClient insecureWebClient,
            HttpRequestBuilder requestBuilder,
            HttpResponseParser responseParser,
            PaginationHandler paginationHandler
    ) {
        this.webClient        = webClient;
        this.insecureWebClient = insecureWebClient;
        this.requestBuilder   = requestBuilder;
        this.responseParser   = responseParser;
        this.paginationHandler = paginationHandler;
    }

    // ── Public API ──────────────────────────────────────────────────

    public List<Map<String, Object>> execute(
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) throws Exception {
        params.validate();

        if (params.getPaginationMode() != PaginationMode.OFF) {
            return paginationHandler.paginate(params, (urlOverride, paramValue) ->
                    executeOnePage(params, inputItem, urlOverride, paramValue)
            );
        }

        return executeOnePage(params, inputItem, null, null);
    }

    // ── Private ────────────────────────────────────────────────────

    private List<Map<String, Object>> executeOnePage(
            HttpRequestParameters params,
            Map<String, Object> inputItem,
            String urlOverride,
            Object paramValue
    ) throws Exception {

        String effectiveUrl = urlOverride;
        if (paramValue != null && params.getPaginationParamName() != null) {
            effectiveUrl = appendOrReplaceParam(
                    params.getUrl(), params.getPaginationParamName(), paramValue.toString());
        }

        URI uri        = requestBuilder.buildUri(params, effectiveUrl, inputItem);
        HttpMethod method = requestBuilder.resolveMethod(params);

        log.debug("HTTP {} {}", method, uri);

        ResponseEntity<byte[]> response = doRequest(method, uri, params, inputItem);
        Map<String, Object> parsed = responseParser.parse(response, params);

        Object body = parsed.get("body");
        if (body instanceof List<?> list) {
            return list.stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new LinkedHashMap<>();
                        if (Boolean.TRUE.equals(params.getIncludeResponseMetadata())) {
                            itemMap.put("statusCode", parsed.get("statusCode"));
                            itemMap.put("headers",    parsed.get("headers"));
                        }
                        itemMap.put("body", item);
                        return itemMap;
                    })
                    .toList();
        }

        return List.of(parsed);
    }

    private ResponseEntity<byte[]> doRequest(
            HttpMethod method,
            URI uri,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        WebClient client = Boolean.TRUE.equals(params.getIgnoreSslIssues())
                ? insecureWebClient
                : webClient;

        boolean hasBody = Boolean.TRUE.equals(params.getSendBody())
                && method != HttpMethod.GET
                && method != HttpMethod.HEAD
                && method != HttpMethod.DELETE;

        int timeoutMs = params.getTimeout() != null ? params.getTimeout() : 30_000;

        try {
            WebClient.RequestHeadersSpec<?> spec;

            if (hasBody) {
                WebClient.RequestBodySpec bodySpec = client.method(method).uri(uri);
                spec = requestBuilder.applyHeaders(bodySpec, params, inputItem);
                spec = requestBuilder.applyBody((WebClient.RequestBodySpec) spec, params, inputItem);
            } else {
                spec = client.method(method).uri(uri);
                spec = requestBuilder.applyHeaders(spec, params, inputItem);
            }

            spec = AUTH_STRATEGIES
                    .getOrDefault(params.getAuthType(), new NoAuthStrategy())
                    .apply(spec, params);

            if (Boolean.TRUE.equals(params.getNeverError())) {
                return spec.exchangeToMono(r -> r.toEntity(byte[].class))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();
            }

            return spec.retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            cr -> cr.bodyToMono(String.class).flatMap(errBody ->
                                    Mono.error(new WebClientResponseException(
                                            cr.statusCode().value(),
                                            "HTTP Error " + cr.statusCode().value(),
                                            cr.headers().asHttpHeaders(),
                                            errBody.getBytes(), null)))
                    )
                    .toEntity(byte[].class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

        } catch (WebClientResponseException e) {
            log.warn("HTTP {} {} → {}", method, uri, e.getStatusCode());
            throw e;
        }
    }

    private String appendOrReplaceParam(String url, String paramName, String value) {
        if (url.contains(paramName + "=")) {
            return url.replaceAll(paramName + "=[^&]*", paramName + "=" + value);
        }
        return url + (url.contains("?") ? "&" : "?") + paramName + "=" + value;
    }
}
