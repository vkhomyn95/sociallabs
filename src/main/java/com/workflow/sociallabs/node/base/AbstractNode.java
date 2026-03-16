package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.NodeResult;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

/**
 * Абстрактний базовий клас для всіх нод
 * Реалізує загальну логіку виконання та логування
 */
@Slf4j
public abstract class AbstractNode implements NodeExecutor {

    protected final NodeDiscriminator nodeType;

    protected AbstractNode(NodeDiscriminator nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public NodeDiscriminator getNodeType() {
        return nodeType;
    }

    /**
     * Шаблонний метод для виконання node з обробкою помилок та логуванням
     */
    @Override
    public final NodeResult execute(ExecutionContext context) {
        Instant start = Instant.now();

        try {
            log.debug("Executing node {} ({})", context.getNodeId(), nodeType);

            if (requiresCredentials() && (context.getCredentials() == null || context.getCredentials().isEmpty())) {
                throw new IllegalStateException("Node " + nodeType + " requires credentials but none provided");
            }

            NodeResult result = executeInternal(context);
            result.setExecutionTimeMs(Duration.between(start, Instant.now()).toMillis());

            log.debug("Node {} completed in {}ms", context.getNodeId(), result.getExecutionTimeMs());
            return result;

        } catch (Exception e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            log.error("Node {} failed after {}ms: {}", context.getNodeId(), ms, e.getMessage(), e);

            return NodeResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .errorStack(getStackTrace(e))
                    .executionTimeMs(ms)
                    .build();
        }
    }

    /**
     * Основна логіка виконання node - має бути імплементована в підкласах
     */
    protected abstract NodeResult executeInternal(ExecutionContext context) throws Exception;

    /**
     * Отримати stack trace як рядок
     */
    protected String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
