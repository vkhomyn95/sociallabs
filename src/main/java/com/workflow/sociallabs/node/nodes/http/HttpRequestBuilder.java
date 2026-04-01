package com.workflow.sociallabs.node.nodes.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters.*;
import com.workflow.sociallabs.service.ExpressionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Збирає WebClient request spec із параметрів ноди.
 * Відповідає тільки за assembly — не виконує запит.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestBuilder {

    private final ObjectMapper objectMapper;

    /**
     * Зібрати URI з query-параметрами.
     *
     * @param params  параметри ноди
     * @param urlOverride якщо пагінація передала свій URL
     */
    public URI buildUri(
            HttpRequestParameters params,
            String urlOverride,
            Map<String, Object> inputItem
    ) {
        String rawUrl = urlOverride != null ? urlOverride : params.getUrl();
        String resolvedUrl = resolveStr(rawUrl, inputItem);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resolvedUrl);

        if (Boolean.TRUE.equals(params.getSendQueryParams())) {
            for (KeyValue kv : resolveKeyValues(
                    params.getQueryParamsInputMode(),
                    params.getQueryParams(),
                    params.getQueryParamsJson(),
                    inputItem)) {
                builder.queryParam(kv.name(), kv.value());
            }
        }

        // Query auth
        if (params.getAuthType() == HttpAuthType.QUERY
                && params.getQueryAuthName() != null) {
            builder.queryParam(
                    params.getQueryAuthName(),
                    resolveStr(params.getQueryAuthValue(), inputItem)
            );
        }

        return builder.build(true).toUri();
    }

    public org.springframework.http.HttpMethod resolveMethod(HttpRequestParameters params) {
        return org.springframework.http.HttpMethod.valueOf(params.getMethod().name());
    }

    /**
     * Застосувати заголовки до spec.
     * Підтримує lowercaseHeaders і резолвить {{$json.*}} у значеннях.
     */
    public WebClient.RequestHeadersSpec<?> applyHeaders(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        if (!Boolean.TRUE.equals(params.getSendHeaders())) return spec;

        // FIX: використовуємо getLowercaseHeaders() з параметрів
        boolean lowercase = Boolean.TRUE.equals(params.isLowercaseHeaders());

        for (KeyValue kv : resolveKeyValues(
                params.getHeadersInputMode(),
                params.getHeaders(),
                params.getHeadersJson(),
                inputItem)) {
            String name = lowercase ? kv.name().toLowerCase() : kv.name();
            spec = spec.header(name, kv.value());
        }
        return spec;
    }

    /**
     * Додати тіло до request spec.
     * Повертає WebClient.RequestHeadersSpec (може бути RequestBodySpec після body).
     */
    @SuppressWarnings("unchecked")
    public WebClient.RequestHeadersSpec<?> applyBody(
            WebClient.RequestBodySpec bodySpec,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        if (!Boolean.TRUE.equals(params.getSendBody())) return bodySpec;

        return switch (params.getBodyContentType()) {
            case JSON             -> applyJsonBody(bodySpec, params, inputItem);
            case FORM_URL_ENCODED -> applyFormUrlEncoded(bodySpec, params, inputItem);
            case FORM_DATA        -> applyFormData(bodySpec, params, inputItem);
            case RAW -> {
                String ct   = resolveStr(params.getRawContentType(), inputItem);
                String body = resolveStr(params.getRawBody(), inputItem);
                yield bodySpec
                        .contentType(MediaType.parseMediaType(ct != null ? ct : "text/plain"))
                        .bodyValue(body != null ? body : "");
            }
            case BINARY -> {
                String fieldName = params.getBinaryFieldName();
                Object binaryData = fieldName != null ? inputItem.get(fieldName) : null;
                if (binaryData instanceof byte[] bytes) {
                    yield bodySpec
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .bodyValue(bytes);
                }
                yield bodySpec;
            }
        };
    }

    // ── Private helpers ────────────────────────────────────────────

    private WebClient.RequestHeadersSpec<?> applyJsonBody(
            WebClient.RequestBodySpec spec,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        if (params.getBodyInputMode() == InputMode.JSON) {
            // Весь JSON може бути виразом {{$json.payload}}
            String json = resolveStr(params.getBodyJson(), inputItem);
            return spec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(json != null ? json : "{}");
        }

        List<KeyValue> kvs = resolveKeyValues(
                params.getBodyInputMode(),
                params.getBodyParams(),
                params.getBodyJson(),
                inputItem
        );
        if (kvs.isEmpty()) {
            return spec.contentType(MediaType.APPLICATION_JSON).bodyValue("{}");
        }

        Map<String, String> map = new LinkedHashMap<>();
        kvs.forEach(kv -> map.put(kv.name(), kv.value()));
        try {
            return spec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.warn("Failed to serialize body params to JSON", e);
            return spec;
        }
    }

    private WebClient.RequestHeadersSpec<?> applyFormUrlEncoded(
            WebClient.RequestBodySpec spec,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        if (params.getBodyInputMode() == InputMode.JSON
                && params.getFormSingleField() != null) {
            String resolved = resolveStr(params.getFormSingleField(), inputItem);
            return spec.contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(resolved != null ? resolved : "");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        resolveKeyValues(
                params.getBodyInputMode(),
                params.getBodyParams(),
                params.getBodyJson(),
                inputItem
        ).forEach(kv -> form.add(kv.name(), kv.value()));

        return spec.contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form));
    }

    private WebClient.RequestHeadersSpec<?> applyFormData(
            WebClient.RequestBodySpec spec,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        resolveKeyValues(
                params.getBodyInputMode(),
                params.getBodyParams(),
                params.getBodyJson(),
                inputItem
        ).forEach(kv -> formData.add(kv.name(), kv.value()));

        if (params.getBinaryFieldName() != null) {
            Object bin = inputItem.get(params.getBinaryFieldName());
            if (bin instanceof byte[] bytes) {
                formData.add(params.getBinaryFieldName(),
                        new org.springframework.core.io.ByteArrayResource(bytes));
            }
        }

        return spec.contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData));
    }

    // ── Helpers ────────────────────────────────────────────────────

    /**
     * Резолвить один рядок через ExpressionEvaluator.
     * Якщо рядок null або не містить "{{" — повертає як є (без зайвої роботи).
     */
    private String resolveStr(String raw, Map<String, Object> data) {
        if (raw == null) return null;
        if (!raw.contains("{{")) return raw;   // швидкий fast-path
        Object resolved = ExpressionEvaluator.resolveValue(raw, data);
        return resolved != null ? resolved.toString() : raw;
    }

    /**
     * Уніфікований helper: Key-Value list або JSON string → List<KeyValue>.
     * Резолвить {{$json.*}} у кожному name і value.
     */
    @SuppressWarnings("unchecked")
    public List<KeyValue> resolveKeyValues(
            InputMode mode,
            List<KeyValue> kvList,
            String jsonString,
            Map<String, Object> inputItem
    ) {
        List<KeyValue> raw;

        if (mode == InputMode.JSON && jsonString != null && !jsonString.isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                raw = map.entrySet().stream()
                        .map(e -> new KeyValue(e.getKey(), String.valueOf(e.getValue())))
                        .toList();
            } catch (Exception e) {
                log.warn("Failed to parse JSON key-values: {}", e.getMessage());
                raw = List.of();
            }
        } else {
            raw = kvList != null ? kvList : List.of();
        }

        return raw.stream()
                .map(kv -> new KeyValue(
                        resolveStr(kv.name(), inputItem),
                        resolveStr(kv.value(), inputItem)
                ))
                .toList();
    }
}
