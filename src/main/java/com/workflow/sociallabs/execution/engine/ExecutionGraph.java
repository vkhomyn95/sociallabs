package com.workflow.sociallabs.execution.engine;

import com.workflow.sociallabs.domain.entity.Connection;
import com.workflow.sociallabs.domain.entity.Node;

import java.util.*;

/**
 * Граф виконання workflow
 * Допоміжна структура для навігації по нодам
 */
public class ExecutionGraph {

    private final Map<String, List<Connection>> outgoingMap = new HashMap<>();
    private final Map<String, List<Connection>> incomingMap = new HashMap<>();

    public void addConnection(Connection connection) {
        String sourceId = connection.getSourceNode().getNodeId();
        String targetId = connection.getTargetNode().getNodeId();

        outgoingMap.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(connection);
        incomingMap.computeIfAbsent(targetId, k -> new ArrayList<>()).add(connection);
    }

    public List<Connection> getOutgoingConnections(Node node) {
        return outgoingMap.getOrDefault(node.getNodeId(), Collections.emptyList());
    }

    public List<Connection> getIncomingConnections(Node node) {
        return incomingMap.getOrDefault(node.getNodeId(), Collections.emptyList());
    }
}
