package com.workflow.sociallabs.node.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Результат виконання workflow
 */
@Getter
@Builder
@AllArgsConstructor
public class WorkflowExecutionResult {

    private final boolean success;
    private final String error;
    private final WorkflowExecutionContext context;
    private final Instant completedAt;

    /**
     * Створити успішний результат
     */
    public static WorkflowExecutionResult success(WorkflowExecutionContext context) {
        return WorkflowExecutionResult.builder()
                .success(true)
                .context(context)
                .completedAt(Instant.now())
                .build();
    }

    /**
     * Створити результат з помилкою
     */
    public static WorkflowExecutionResult error(Throwable error) {
        return WorkflowExecutionResult.builder()
                .success(false)
                .error(error.getMessage())
                .completedAt(Instant.now())
                .build();
    }
}
