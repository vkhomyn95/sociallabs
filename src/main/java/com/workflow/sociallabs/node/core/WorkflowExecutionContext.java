package com.workflow.sociallabs.node.core;

import lombok.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Контекст виконання всього workflow
 * Зберігає стан виконання та результати нод
 */
@Getter
public class WorkflowExecutionContext {

    private final Long workflowId;
    private final String triggerNodeId;
    private final Map<String, Object> triggerData;
    private final Instant startTime;

    // Результати виконання нод: nodeId -> NodeResult
    private final Map<String, NodeResult> nodeResults = new ConcurrentHashMap<>();

    // Додаткові метадані виконання
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    public WorkflowExecutionContext(
            Long workflowId,
            String triggerNodeId,
            Map<String, Object> triggerData) {

        this.workflowId = workflowId;
        this.triggerNodeId = triggerNodeId;
        this.triggerData = triggerData != null ? triggerData : new HashMap<>();
        this.startTime = Instant.now();
    }

    /**
     * Зберегти результат виконання ноди
     */
    public void setNodeResult(String nodeId, NodeResult result) {
        nodeResults.put(nodeId, result);
    }

    /**
     * Отримати результат виконання ноди
     */
    public NodeResult getNodeResult(String nodeId) {
        return nodeResults.get(nodeId);
    }

    /**
     * Перевірити чи нода була виконана
     */
    public boolean hasNodeResult(String nodeId) {
        return nodeResults.containsKey(nodeId);
    }

    /**
     * Отримати час виконання
     */
    public long getExecutionTimeMs() {
        return java.time.Duration.between(startTime, Instant.now()).toMillis();
    }

    /**
     * Встановити метадані
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Отримати метадані
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
}