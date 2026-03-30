package com.workflow.sociallabs.node.nodes.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters.*;
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
    public URI buildUri(HttpRequestParameters params, String urlOverride) {
        String baseUrl = urlOverride != null ? urlOverride : params.getUrl();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);

        if (Boolean.TRUE.equals(params.getSendQueryParams())) {
            List<KeyValue> queryParams = resolveKeyValues(
                    params.getQueryParamsInputMode(),
                    params.getQueryParams(),
                    params.getQueryParamsJson()
            );
            for (KeyValue kv : queryParams) {
                builder.queryParam(kv.name(), kv.value());
            }
        }

        // Query auth — append token to URL
        if (params.getAuthType() == HttpAuthType.QUERY
                && params.getQueryAuthName() != null) {
            builder.queryParam(params.getQueryAuthName(), params.getQueryAuthValue());
        }

        return builder.build(true).toUri();
    }

    /**
     * Повернути метод Spring HTTP для params.getMethod()
     */
    public org.springframework.http.HttpMethod resolveMethod(HttpRequestParameters params) {
        return org.springframework.http.HttpMethod.valueOf(params.getMethod().name());
    }

    /**
     * Додати заголовки до spec.
     */
    public WebClient.RequestHeadersSpec<?> applyHeaders(
            WebClient.RequestHeadersSpec<?> spec,
            HttpRequestParameters params
    ) {
        if (!Boolean.TRUE.equals(params.getSendHeaders())) return spec;

        List<KeyValue> headers = resolveKeyValues(
                params.getHeadersInputMode(),
                params.getHeaders(),
                params.getHeadersJson()
        );

        for (KeyValue kv : headers) {
            String name = Boolean.TRUE.equals(params.isLowercaseHeaders())
                    ? kv.name().toLowerCase()
                    : kv.name();
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
            case JSON -> applyJsonBody(bodySpec, params);
            case FORM_URL_ENCODED -> applyFormUrlEncoded(bodySpec, params);
            case FORM_DATA -> applyFormData(bodySpec, params, inputItem);
            case RAW -> bodySpec
                    .contentType(MediaType.parseMediaType(params.getRawContentType()))
                    .bodyValue(params.getRawBody() != null ? params.getRawBody() : "");
            case BINARY -> {
                // Бінарне тіло з inputItem
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
            HttpRequestParameters params
    ) {
        if (params.getBodyInputMode() == InputMode.JSON) {
            String json = params.getBodyJson();
            return spec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(json != null ? json : "{}");
        }

        List<KeyValue> kvs = params.getBodyParams();
        if (kvs == null || kvs.isEmpty()) {
            return spec.contentType(MediaType.APPLICATION_JSON).bodyValue("{}");
        }

        // Build map → serialize
        Map<String, String> map = new java.util.LinkedHashMap<>();
        kvs.forEach(kv -> map.put(kv.name(), kv.value()));

        try {
            String json = objectMapper.writeValueAsString(map);
            return spec.contentType(MediaType.APPLICATION_JSON).bodyValue(json);
        } catch (Exception e) {
            log.warn("Failed to serialize body params to JSON", e);
            return spec;
        }
    }

    private WebClient.RequestHeadersSpec<?> applyFormUrlEncoded(
            WebClient.RequestBodySpec spec,
            HttpRequestParameters params
    ) {
        if (params.getBodyInputMode() == InputMode.JSON && params.getFormSingleField() != null) {
            // single field format: "k1=v1&k2=v2"
            return spec.contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(params.getFormSingleField());
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        List<KeyValue> kvs = resolveKeyValues(
                params.getBodyInputMode(),
                params.getBodyParams(),
                params.getBodyJson()
        );
        kvs.forEach(kv -> form.add(kv.name(), kv.value()));
        return spec.contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form));
    }

    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec<?> applyFormData(
            WebClient.RequestBodySpec spec,
            HttpRequestParameters params,
            Map<String, Object> inputItem
    ) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        List<KeyValue> kvs = resolveKeyValues(
                params.getBodyInputMode(),
                params.getBodyParams(),
                params.getBodyJson()
        );
        kvs.forEach(kv -> formData.add(kv.name(), kv.value()));

        // Attach binary if binaryFieldName provided
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

    /**
     * Уніфікований helper: Key-Value list або JSON string → List<KeyValue>
     */
    @SuppressWarnings("unchecked")
    public List<KeyValue> resolveKeyValues(
            InputMode mode,
            List<KeyValue> kvList,
            String jsonString
    ) {
        if (mode == InputMode.JSON && jsonString != null && !jsonString.isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                return map.entrySet().stream()
                        .map(e -> new KeyValue(e.getKey(), String.valueOf(e.getValue())))
                        .toList();
            } catch (Exception e) {
                log.warn("Failed to parse JSON key-values: {}", e.getMessage());
            }
        }
        return kvList != null ? kvList : List.of();
    }
}
