package com.workflow.sociallabs.node.core;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Контекст виконання всього workflow
 * Зберігає стан виконання та результати нод
 */
@Getter
public class WorkflowExecutionContext {

    private final Long workflowId;
    private final String triggerNodeId;
    private final Map<String, Object> triggerData;

    // nodeId → NodeResult (зберігаємо по reference)
    private final Map<String, NodeResult> nodeResults = new HashMap<>();

    public WorkflowExecutionContext(
            Long workflowId,
            String triggerNodeId,
            Map<String, Object> triggerData
    ) {
        this.workflowId = workflowId;
        this.triggerNodeId = triggerNodeId;
        this.triggerData = triggerData;
    }

    public void setNodeResult(String nodeId, NodeResult result) {
        nodeResults.put(nodeId, result);
    }

    public NodeResult getNodeResult(String nodeId) {
        return nodeResults.get(nodeId);
    }

    public boolean isExecuted(String nodeId) {
        return nodeResults.containsKey(nodeId);
    }
}