package com.workflow.sociallabs.service;

import com.workflow.sociallabs.domain.entity.Connection;
import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.domain.repository.WorkflowRepository;
import com.workflow.sociallabs.node.core.GraphNode;
import com.workflow.sociallabs.node.core.WorkflowExecutionGraph;
import com.workflow.sociallabs.security.CredentialEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кеш для execution графів workflow
 * Зберігає побудовані графи в пам'яті для швидкого доступу
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutionGraphCache {

    private final WorkflowRepository workflowRepository;
    private final CredentialEncryption credentialEncryption;

    // Кеш графів: workflowId -> WorkflowExecutionGraph
    private final Map<Long, WorkflowExecutionGraph> cache = new ConcurrentHashMap<>();

    /**
     * Отримати граф з кешу або побудувати новий
     */
    public WorkflowExecutionGraph getOrBuildGraph(Long workflowId) {
        return cache.computeIfAbsent(workflowId, this::buildGraph);
    }

    /**
     * Invalidate кеш для workflow
     */
    public void invalidate(Long workflowId) {
        cache.remove(workflowId);
        log.debug("Invalidated execution graph cache for workflow: {}", workflowId);
    }

    /**
     * Очистити весь кеш
     */
    public void invalidateAll() {
        cache.clear();
        log.info("Cleared all execution graph cache");
    }

    /**
     * Побудувати execution граф для workflow
     */
    private WorkflowExecutionGraph buildGraph(Long workflowId) {
        log.info("Building execution graph for workflow: {}", workflowId);

        // Завантажуємо workflow з усіма нодами та зв'язками
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        List<Node> nodes = workflow.getNodes();
        List<Connection> connections = workflow.getConnections();

        // Створюємо graph nodes
        Map<String, GraphNode> graphNodes = new HashMap<>();
        List<GraphNode> triggerNodes = new ArrayList<>();

        for (Node node : nodes) {
            if (node.getDisabled()) {
                log.debug("Skipping disabled node: {}", node.getNodeId());
                continue;
            }

            GraphNode graphNode = createGraphNode(node);
            graphNodes.put(node.getNodeId(), graphNode);

            if (node.isTrigger()) {
                triggerNodes.add(graphNode);
            }
        }

        // Будуємо зв'язки між нодами
        Map<String, List<GraphNode>> adjacencyList = buildAdjacencyList(
                connections, graphNodes
        );

        // Створюємо граф
        WorkflowExecutionGraph graph = WorkflowExecutionGraph.builder()
                .workflowId(workflowId)
                .nodes(graphNodes)
                .triggerNodes(triggerNodes)
                .adjacencyList(adjacencyList)
                .build();

        // Валідація
        if (!graph.isValid()) {
            throw new IllegalStateException(
                    "Invalid workflow graph: workflow must have at least one trigger and no cycles"
            );
        }

        log.info("Built execution graph for workflow {}: {} nodes, {} triggers",
                workflowId, graphNodes.size(), triggerNodes.size());

        return graph;
    }

    /**
     * Створити GraphNode з Node entity
     */
    private GraphNode createGraphNode(Node node) {
        // Підготовка credentials
        Map<String, Object> credentials = new HashMap<>();

        if (node.getCredential() != null) {
            try {
                String decryptedData = credentialEncryption.decrypt(
                        node.getCredential().getEncryptedData()
                );
                credentials = deserializeCredentials(decryptedData);
            } catch (Exception e) {
                log.error("Failed to decrypt credentials for node {}: {}",
                        node.getNodeId(), e.getMessage());
            }
        }

        return GraphNode.builder()
                .nodeId(node.getNodeId())
                .discriminator(node.getDiscriminator())
                .parameters(node.getParameters())
                .credentials(credentials)
                .build();
    }

    /**
     * Побудувати adjacency list для графа
     */
    private Map<String, List<GraphNode>> buildAdjacencyList(
            List<Connection> connections,
            Map<String, GraphNode> graphNodes) {

        Map<String, List<GraphNode>> adjacencyList = new HashMap<>();

        for (Connection connection : connections) {
            String sourceId = connection.getSourceNode().getNodeId();
            String targetId = connection.getTargetNode().getNodeId();

            GraphNode sourceNode = graphNodes.get(sourceId);
            GraphNode targetNode = graphNodes.get(targetId);

            // Пропускаємо disabled ноди
            if (sourceNode == null || targetNode == null) {
                continue;
            }

            // Додаємо в adjacency list
            adjacencyList.computeIfAbsent(sourceId, k -> new ArrayList<>())
                    .add(targetNode);

            // Оновлюємо зв'язки в GraphNode
            sourceNode.addNextNode(targetNode);
            targetNode.addPreviousNode(sourceNode);
        }

        return adjacencyList;
    }

    /**
     * Десеріалізувати credentials з JSON
     */
    private Map<String, Object> deserializeCredentials(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize credentials: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Отримати статистику кешу
     */
    public CacheStatistics getStatistics() {
        return CacheStatistics.builder()
                .cachedWorkflows(cache.size())
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class CacheStatistics {
        private final int cachedWorkflows;
    }
}