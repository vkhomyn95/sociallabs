package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.node.nodes.ai.agent.exception.ToolExecutionException;
import com.workflow.sociallabs.node.nodes.ai.agent.exception.ToolNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public final class ToolExecutionPipeline {

    private final ToolRegistry registry;
    private final ObjectMapper objectMapper;
//    private final MeterRegistry meterRegistry;

    // ValidatorFactory — створюємо один раз, не в кожному виклику
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    public ToolExecutionResult execute(
            @NonNull ToolCallRequest call,
            @NonNull ToolContext context
    ) {

        String toolName = call.toolName();
        Instant start = Instant.now(context.getClock());

        log.debug("Executing tool '{}' callId={}", toolName, call.callId());

        AgentTool<ToolInput, ToolOutput> tool;
        try {
            tool = registry.get(toolName);
        } catch (ToolNotFoundException e) {
            // Tool не знайдено — одразу failure, не retryable
            return failureResult(call, start, context, "TOOL_NOT_FOUND", e.getMessage(), false);
        }

        try {
            // 1. Deserialize rawArguments (JsonNode) → строго-типізований ToolInput
            ToolInput input = objectMapper.treeToValue(
                    call.rawArguments(),
                    tool.getInputType()
            );

            // 2. JSR-380 bean validation
            validateInput(input);

            // 3. Виконання tool
            ToolOutput output = tool.execute(input, context);

            long ms = elapsed(start, context);

            // 4. Metrics — success
            recordTimer(toolName, "true", ms);

            log.debug("Tool '{}' completed in {}ms success={}", toolName, ms, output.isSuccess());

            return buildResult(call, output, ms);

        } catch (JsonProcessingException e) {
            log.warn("Tool '{}': failed to deserialize arguments: {}", toolName, e.getMessage());
            return failureResult(call, start, context, "INVALID_ARGUMENTS", "Failed to parse tool arguments: " + e.getMessage(), false);

        } catch (ConstraintViolationException e) {
            log.warn("Tool '{}': validation failed: {}", toolName, e.getMessage());
            return failureResult(call, start, context, "VALIDATION_FAILED", buildViolationMessage(e), false);

        } catch (ToolExecutionException e) {
            log.warn("Tool '{}': execution failed code={} retryable={}: {}", toolName, e.getErrorCode(), e.isRetryable(), e.getMessage());
//            meterRegistry.counter("agent.tool.error",
//                    "tool", toolName, "code", e.getErrorCode()).increment();
            return failureResult(call, start, context, e.getErrorCode(), e.getMessage(), e.isRetryable());

        } catch (Exception e) {
            // Непередбачена помилка — логуємо з stack trace
            log.error("Tool '{}': unexpected error: {}", toolName, e.getMessage(), e);
            return failureResult(call, start, context, "UNEXPECTED_ERROR", "Unexpected error: " + e.getMessage(), true);
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void validateInput(ToolInput input) {
        Set<ConstraintViolation<ToolInput>> violations =
                (Set<ConstraintViolation<ToolInput>>) (Set<?>)
                        VALIDATOR_FACTORY.getValidator().validate(input);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private ToolExecutionResult buildResult(
            ToolCallRequest callRequest,
            ToolOutput output,
            long ms) {

        ToolResult toolResult = toToolResult(output);

        return ToolExecutionResult.builder()
                .callId(callRequest.callId())
                .toolName(callRequest.toolName())
                .input(callRequest.rawArguments())
                .output(output)
                .toolResult(toolResult)
                .executionTimeMs(ms)
                .build();
    }

    private ToolExecutionResult failureResult(
            ToolCallRequest callRequest,
            Instant start,
            ToolContext context,
            String errorCode,
            String errorMessage,
            boolean retryable) {

        long ms = elapsed(start, context);
        recordTimer(callRequest.toolName(), "false", ms);

        ToolOutput.Failure failure = new ToolOutput.Failure(errorCode, errorMessage, retryable);

        return ToolExecutionResult.builder()
                .callId(callRequest.callId())
                .toolName(callRequest.toolName())
                .input(callRequest.rawArguments())
                .output(failure)
                .toolResult(toToolResult(failure))
                .executionTimeMs(ms)
                .build();
    }

    private ToolResult toToolResult(ToolOutput output) {
        return switch (output) {
            case ToolOutput.Success s -> ToolResult.Ok.of(s.data());
            case ToolOutput.Failure f -> new ToolResult.Err(f.errorCode(), f.errorMessage(), f.retryable());
        };
    }

    private long elapsed(Instant start, ToolContext context) {
        return Duration.between(start, Instant.now(context.getClock())).toMillis();
    }

    private void recordTimer(String toolName, String success, long ms) {
//        meterRegistry.timer("agent.tool.execution",
//                "tool", toolName, "success", success
//        ).record(ms, TimeUnit.MILLISECONDS);
    }

    private String buildViolationMessage(ConstraintViolationException e) {
        StringBuilder sb = new StringBuilder("Validation failed: ");
        e.getConstraintViolations().forEach(v ->
                sb.append(v.getPropertyPath())
                        .append(" ")
                        .append(v.getMessage())
                        .append("; ")
        );
        return sb.toString();
    }
}