package com.workflow.sociallabs.node.core;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Граф виконання workflow
 * Оптимізована структура для швидкого виконання без постійних запитів до БД
 */
@Builder
@Getter
public class WorkflowExecutionGraph {

    private Long workflowId;
    private Map<String, GraphNode> nodes;
    private List<GraphNode> triggerNodes;

    // nodeId → список вихідних ребер
    private Map<String, List<GraphEdge>> outgoingEdges;
    // nodeId → список вхідних ребер
    private Map<String, List<GraphEdge>> incomingEdges;

    public GraphNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<GraphEdge> getOutgoing(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, List.of());
    }

    public List<GraphEdge> getIncoming(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, List.of());
    }

    public boolean isValid() {
        return !triggerNodes.isEmpty();
    }
}