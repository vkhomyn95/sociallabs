package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.domain.entity.*;
import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.*;

import java.util.*;

/**
 * Граф виконання workflow
 * Оптимізована структура для швидкого виконання без постійних запитів до БД
 */
@Getter
@Builder
public class WorkflowExecutionGraph {

    private final Long workflowId;

    // Усі ноди графа: nodeId -> GraphNode
    private final Map<String, GraphNode> nodes;

    // Тригерні ноди (точки входу в workflow)
    private final List<GraphNode> triggerNodes;

    // Зв'язки між нодами
    private final Map<String, List<GraphNode>> adjacencyList;

    /**
     * Отримати ноду за ID
     */
    public GraphNode getNode(String nodeId) {
        GraphNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found in graph: " + nodeId);
        }
        return node;
    }

    /**
     * Отримати наступні ноди для заданої ноди
     */
    public List<GraphNode> getNextNodes(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * Перевірити чи граф валідний (немає циклів, є хоча б один тригер)
     */
    public boolean isValid() {
        return !triggerNodes.isEmpty() && !hasCycles();
    }

    /**
     * Перевірити наявність циклів (DFS)
     */
    private boolean hasCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (GraphNode triggerNode : triggerNodes) {
            if (hasCycleDFS(triggerNode.getNodeId(), visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * DFS для перевірки циклів
     */
    private boolean hasCycleDFS(String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true; // Цикл знайдено
        }

        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (GraphNode nextNode : getNextNodes(nodeId)) {
            if (hasCycleDFS(nextNode.getNodeId(), visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * Отримати топологічне сортування нод (для оптимізованого виконання)
     */
    public List<GraphNode> getTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();

        // Підрахунок вхідних ребер
        for (GraphNode node : nodes.values()) {
            inDegree.putIfAbsent(node.getNodeId(), 0);
        }

        for (List<GraphNode> neighbors : adjacencyList.values()) {
            for (GraphNode neighbor : neighbors) {
                inDegree.put(neighbor.getNodeId(),
                        inDegree.getOrDefault(neighbor.getNodeId(), 0) + 1);
            }
        }

        // Kahn's algorithm
        Queue<GraphNode> queue = new LinkedList<>();
        for (GraphNode node : triggerNodes) {
            if (inDegree.get(node.getNodeId()) == 0) {
                queue.offer(node);
            }
        }

        List<GraphNode> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            result.add(current);

            for (GraphNode next : getNextNodes(current.getNodeId())) {
                int newDegree = inDegree.get(next.getNodeId()) - 1;
                inDegree.put(next.getNodeId(), newDegree);

                if (newDegree == 0) {
                    queue.offer(next);
                }
            }
        }

        return result;
    }
}

