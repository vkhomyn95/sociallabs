package com.workflow.sociallabs.node.nodes.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters.ResponseFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Парсить відповідь сервера у Map<String, Object> (WorkflowItem).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * @param response  відповідь з byte[] тілом
     * @param params    параметри ноди
     * @return          один item для workflow
     */
    public Map<String, Object> parse(
            ResponseEntity<byte[]> response,
            HttpRequestParameters params
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Metadata якщо увімкнено
        if (Boolean.TRUE.equals(params.getIncludeResponseMetadata())) {
            result.put("statusCode", response.getStatusCode().value());
            result.put("headers", flattenHeaders(response.getHeaders()));
        }

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            result.put("body", null);
            return result;
        }

        ResponseFormat format = resolveFormat(params.getResponseFormat(), response.getHeaders());

        switch (format) {
            case JSON -> result.put("body", parseJson(body));
            case TEXT -> result.put(
                    params.getResponseOutputField() != null ? params.getResponseOutputField() : "body",
                    new String(body)
            );
            case FILE -> result.put(
                    params.getResponseOutputField() != null ? params.getResponseOutputField() : "data",
                    body   // binary — downstream nodes handle this
            );
            default -> result.put("body", parseJson(body)); // autodetect → json fallback
        }

        // Check for error status
        HttpStatusCode status = response.getStatusCode();
        if (!status.is2xxSuccessful() && !Boolean.TRUE.equals(params.getNeverError())) {
            result.put("_httpError", true);
            result.put("_statusCode", status.value());
        }

        return result;
    }

    // ── Private helpers ────────────────────────────────────────────

    /**
     * Autodetect: content-type header → best format
     */
    private ResponseFormat resolveFormat(ResponseFormat requested, HttpHeaders headers) {
        if (requested != ResponseFormat.AUTODETECT) return requested;

        MediaType ct = headers.getContentType();
        if (ct == null) return ResponseFormat.TEXT;

        if (ct.isCompatibleWith(MediaType.APPLICATION_JSON)
                || ct.getSubtype().contains("json")) {
            return ResponseFormat.JSON;
        }
        if (ct.getType().equals("image")
                || ct.getType().equals("video")
                || ct.getType().equals("audio")
                || ct.getSubtype().contains("octet-stream")
                || ct.getSubtype().contains("pdf")) {
            return ResponseFormat.FILE;
        }
        return ResponseFormat.TEXT;
    }

    @SuppressWarnings("unchecked")
    private Object parseJson(byte[] body) {
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (Exception e) {
            log.debug("Could not parse as JSON, returning as text: {}", e.getMessage());
            return new String(body);
        }
    }

    private Map<String, Object> flattenHeaders(HttpHeaders headers) {
        Map<String, Object> flat = new LinkedHashMap<>();
        headers.forEach((k, v) -> flat.put(k, v.size() == 1 ? v.get(0) : v));
        return flat;
    }
}
