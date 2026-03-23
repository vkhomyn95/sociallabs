package com.workflow.sociallabs.node.nodes.ai.agent.tool.builtin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.sociallabs.node.nodes.ai.agent.exception.ToolExecutionException;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.*;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.OBJECT;
import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.STRING;

@Component
@RequiredArgsConstructor
public final class HttpRequestTool
        implements AgentTool<HttpRequestTool.Input, ToolOutput> {

    private final RestClient restClient;

    @Override
    public String getName() {
        return "http_request";
    }

    @Override
    public String getDescription() {
        return "Make an HTTP request to any URL";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName())
                .description(getDescription())
                .parameter(ToolSchema.ToolParameter.builder()
                        .name("url").type(STRING).description("Target URL").build())
                .parameter(ToolSchema.ToolParameter.builder()
                        .name("method").type(STRING).description("GET|POST|PUT|DELETE").build())
                .parameter(ToolSchema.ToolParameter.builder()
                        .name("body").type(OBJECT).description("Request body (optional)").build())
                .required(List.of("url", "method"))
                .build();
    }

    @Override
    public ToolOutput execute(Input input, ToolContext ctx) throws ToolExecutionException {
        try {
            ResponseEntity<String> resp = restClient.method(HttpMethod.valueOf(input.method()))
                    .uri(input.url())
                    .body(input.body())
                    .retrieve()
                    .toEntity(String.class);

            return new ToolOutput.Success(
                    Map.of("status", resp.getStatusCode().value(), "body", resp.getBody()), "HTTP " + resp.getStatusCode()
            );
        } catch (Exception e) {
            throw new ToolExecutionException("HTTP_ERROR", e.getMessage(), true);
        }
    }

    // Строго-типізовані input/output — десеріалізуються Jackson автоматично
    public record Input(
            @JsonProperty(required = true) @NotBlank String url,
            @JsonProperty(required = true) @NotBlank String method,
            @JsonProperty @Nullable Map<String, Object> body,
            @JsonProperty @Nullable Map<String, String> headers
    ) implements ToolInput {
    }
}
