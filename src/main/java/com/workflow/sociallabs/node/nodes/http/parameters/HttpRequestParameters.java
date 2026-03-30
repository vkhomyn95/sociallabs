package com.workflow.sociallabs.node.nodes.http.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Параметри HTTP Request ноди.
 * Підтримує всі методи, типи авторизації, тіло, заголовки,
 * пагінацію та batching.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.HTTP_REQUEST)
public class HttpRequestParameters implements TypedNodeParameters {

    // ── Method & URL ──────────────────────────────────────────────
    @Builder.Default
    private HttpMethod method = HttpMethod.GET;

    private String url;

    // ── Authentication ─────────────────────────────────────────────
    @Builder.Default
    private HttpAuthType authType = HttpAuthType.NONE;

    // Basic auth
    private String basicUsername;
    private String basicPassword;

    // Header auth
    private String headerAuthName;
    private String headerAuthValue;

    // Query auth
    private String queryAuthName;
    private String queryAuthValue;

    // Bearer / custom token
    private String bearerToken;

    // ── Query Parameters ───────────────────────────────────────────
    @Builder.Default
    private Boolean sendQueryParams = false;

    /** Using fields (list) or JSON string */
    @Builder.Default
    private InputMode queryParamsInputMode = InputMode.KEY_VALUE;

    private List<KeyValue> queryParams;
    private String queryParamsJson;

    @Builder.Default
    private ArrayFormat queryArrayFormat = ArrayFormat.NO_BRACKETS;

    // ── Headers ────────────────────────────────────────────────────
    @Builder.Default
    private Boolean sendHeaders = false;

    @Builder.Default
    private InputMode headersInputMode = InputMode.KEY_VALUE;

    private List<KeyValue> headers;
    private String headersJson;

    @Builder.Default
    private boolean lowercaseHeaders = true;

    // ── Body ───────────────────────────────────────────────────────
    @Builder.Default
    private Boolean sendBody = false;

    @Builder.Default
    private BodyContentType bodyContentType = BodyContentType.JSON;

    // JSON body
    @Builder.Default
    private InputMode bodyInputMode = InputMode.KEY_VALUE;

    private List<KeyValue> bodyParams;
    private String bodyJson;
    private String rawBody;
    private String rawContentType;

    // Form URL-encoded single field
    private String formSingleField;

    // Binary body
    private String binaryFieldName;

    // ── Options ────────────────────────────────────────────────────
    @Builder.Default
    private Boolean ignoreSslIssues = false;

    @Builder.Default
    private Boolean followRedirects = true;

    @Builder.Default
    private Integer maxRedirects = 21;

    /** Include status code and headers in output */
    @Builder.Default
    private Boolean includeResponseMetadata = false;

    /** Never return error even on 4xx/5xx */
    @Builder.Default
    private Boolean neverError = false;

    @Builder.Default
    private ResponseFormat responseFormat = ResponseFormat.AUTODETECT;

    /** Field name when responseFormat = FILE or TEXT */
    @Builder.Default
    private String responseOutputField = "data";

    /** Timeout in ms */
    @Builder.Default
    private Integer timeout = 30_000;

    /** Proxy URL, e.g. http://proxy:3128 */
    private String proxy;

    // ── Pagination ─────────────────────────────────────────────────
    @Builder.Default
    private PaginationMode paginationMode = PaginationMode.OFF;

    /** Expression resolving to next URL (for RESPONSE_URL mode) */
    private String nextUrlExpression;

    /** Parameter name to update (for UPDATE_PARAM mode) */
    private String paginationParamName;

    /** Starting value */
    @Builder.Default
    private Integer paginationStartValue = 0;

    /** Increment per request */
    @Builder.Default
    private Integer paginationIncrement = 1;

    /** Stop when response is empty array / null */
    @Builder.Default
    private Boolean paginationStopOnEmpty = true;

    @Builder.Default
    private Integer paginationMaxRequests = 100;

    // ── Batching ───────────────────────────────────────────────────
    @Builder.Default
    private Integer batchSize = 1;

    @Builder.Default
    private Integer batchInterval = 0;

    // ── Error handling ─────────────────────────────────────────────
    @Builder.Default
    private Boolean continueOnFail = false;

    // ── Inner types ────────────────────────────────────────────────

    public record KeyValue(String name, String value) {}

    public enum HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

    public enum HttpAuthType { NONE, BASIC, HEADER, QUERY, BEARER, DIGEST, OAUTH2 }

    public enum InputMode { KEY_VALUE, JSON }

    public enum BodyContentType { JSON, FORM_URL_ENCODED, FORM_DATA, BINARY, RAW }

    public enum ArrayFormat { NO_BRACKETS, BRACKETS_ONLY, BRACKETS_WITH_INDICES }

    public enum ResponseFormat { AUTODETECT, JSON, TEXT, FILE }

    public enum PaginationMode { OFF, UPDATE_PARAM, RESPONSE_URL }

    // ── TypedNodeParameters ────────────────────────────────────────

    @Override
    public void validate() throws IllegalArgumentException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")
                && !url.startsWith("{{")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        if (sendBody && bodyContentType == BodyContentType.RAW
                && (rawContentType == null || rawContentType.isBlank())) {
            throw new IllegalArgumentException("Content-Type is required for raw body");
        }
    }

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.HTTP_REQUEST;
    }
}
