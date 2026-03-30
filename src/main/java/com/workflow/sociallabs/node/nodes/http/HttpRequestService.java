package com.workflow.sociallabs.node.nodes.http;

import com.workflow.sociallabs.node.nodes.http.auth.*;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * Основний сервіс для виконання HTTP-запитів.
 * Використовує WebClient (Spring WebFlux) — non-blocking.
 * Підтримує всі методи, авторизацію, тіло, пагінацію.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpRequestService {

    private final WebClient webClient;
    private final HttpRequestBuilder requestBuilder;
    private final HttpResponseParser responseParser;
    private final PaginationHandler paginationHandler;

    private static final Map<HttpAuthType, HttpAuthStrategy> AUTH_STRATEGIES = Map.of(
            HttpAuthType.NONE,   new NoAuthStrategy(),
            HttpAuthType.BASIC,  new BasicAuthStrategy(),
            HttpAuthType.HEADER, new HeaderAuthStrategy(),
            HttpAuthType.BEARER, new BearerAuthStrategy(),
            HttpAuthType.QUERY,  new NoAuthStrategy()  // query params handled in URI
    );

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Виконати один HTTP-запит для одного input item.
     * Якщо пагінація увімкнена — повертає кілька items.
     */
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

    /**
     * Виконати один запит (одну сторінку).
     *
     * @param urlOverride  URL для пагінації (може бути null)
     * @param paramValue   значення параметра пагінації (може бути null)
     */
    private List<Map<String, Object>> executeOnePage(
            HttpRequestParameters params,
            Map<String, Object> inputItem,
            String urlOverride,
            Object paramValue
    ) throws Exception {

        // Якщо UPDATE_PARAM пагінація — додати param до URL
        String effectiveUrl = urlOverride;
        if (paramValue != null && params.getPaginationParamName() != null) {
            // Append/replace param in URL
            String base = params.getUrl();
            effectiveUrl = appendOrReplaceParam(base,
                    params.getPaginationParamName(),
                    paramValue.toString());
        }

        URI uri = requestBuilder.buildUri(params, effectiveUrl);
        HttpMethod method = requestBuilder.resolveMethod(params);

        log.debug("HTTP {} {}", method, uri);

        ResponseEntity<byte[]> response = doRequest(method, uri, params, inputItem);
        Map<String, Object> parsed = responseParser.parse(response, params);

        // Якщо тіло — список, розгортаємо
        Object body = parsed.get("body");
        if (body instanceof List<?> list) {
            return list.stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new LinkedHashMap<>();
                        if (Boolean.TRUE.equals(params.getIncludeResponseMetadata())) {
                            itemMap.put("statusCode", parsed.get("statusCode"));
                            itemMap.put("headers", parsed.get("headers"));
                        }
                        itemMap.put("body", item);
                        return itemMap;
                    })
                    .toList();
        }

        return List.of(parsed);
    }

    /**
     * Виконати фактичний WebClient запит.
     */
    private ResponseEntity<byte[]> doRequest(
            HttpMethod method,
            URI uri,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        boolean hasBody = Boolean.TRUE.equals(params.getSendBody())
                && method != HttpMethod.GET
                && method != HttpMethod.HEAD
                && method != HttpMethod.DELETE;

        try {
            WebClient.RequestHeadersSpec<?> spec;

            if (hasBody) {
                WebClient.RequestBodySpec bodySpec = webClient
                        .method(method)
                        .uri(uri);

                // Apply headers before body
                spec = requestBuilder.applyHeaders(bodySpec, params);
                // Body — must cast back to RequestBodySpec
                spec = requestBuilder.applyBody((WebClient.RequestBodySpec) spec, params, inputItem);
            } else {
                spec = webClient.method(method).uri(uri);
                spec = requestBuilder.applyHeaders(spec, params);
            }

            // Apply auth
            HttpAuthStrategy authStrategy = AUTH_STRATEGIES.getOrDefault(
                    params.getAuthType(), new NoAuthStrategy());
            spec = authStrategy.apply(spec, params);

            // Execute + retrieve
            int timeoutMs = params.getTimeout() != null ? params.getTimeout() : 30_000;

            Mono<ResponseEntity<byte[]>> responseMono = spec
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful()
                                    && !Boolean.TRUE.equals(params.getNeverError()),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new WebClientResponseException(
                                                    clientResponse.statusCode().value(),
                                                    "HTTP Error " + clientResponse.statusCode().value(),
                                                    clientResponse.headers().asHttpHeaders(),
                                                    body.getBytes(),
                                                    null
                                            )
                                    ))
                    )
                    .toEntity(byte[].class);

            if (Boolean.TRUE.equals(params.getNeverError())) {
                // Never error: catch 4xx/5xx and return as-is
                responseMono = webClient.method(method).uri(uri)
                        .exchangeToMono(response ->
                                response.toEntity(byte[].class));
            }

            return responseMono
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

        } catch (WebClientResponseException e) {
            log.warn("HTTP {} {} → {} {}", method, uri, e.getStatusCode(), e.getMessage());
            throw e;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private String appendOrReplaceParam(String url, String paramName, String value) {
        if (url.contains(paramName + "=")) {
            return url.replaceAll(paramName + "=[^&]*", paramName + "=" + value);
        }
        return url + (url.contains("?") ? "&" : "?") + paramName + "=" + value;
    }
}
