package com.workflow.sociallabs.service;

import com.workflow.sociallabs.domain.entity.Connection;
import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.domain.repository.WorkflowRepository;
import com.workflow.sociallabs.node.core.GraphEdge;
import com.workflow.sociallabs.node.core.GraphNode;
import com.workflow.sociallabs.node.core.WorkflowExecutionGraph;
import com.workflow.sociallabs.security.CredentialEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кеш для execution графів workflow
 * Зберігає побудовані графи в пам'яті для швидкого доступу
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutionCache {

    private final WorkflowRepository workflowRepository;
    private final CredentialEncryption credentialEncryption;

    // Кеш графів: workflowId -> WorkflowExecutionGraph
    private final Map<Long, WorkflowExecutionGraph> cache = new ConcurrentHashMap<>();

    /**
     * Отримати граф з кешу або побудувати новий
     */
    public WorkflowExecutionGraph getGraph(Long workflowId) {
        return cache.computeIfAbsent(workflowId, this::buildGraph);
    }

    /**
     * Invalidate кеш для workflow
     */
    public void invalidate(Long workflowId) {
        cache.remove(workflowId);
        log.info("Invalidated execution graph cache for workflow: {}", workflowId);
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

        // Завантажуємо workflow з усіма nodes та зв'язками
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        // 1. Будуємо GraphNode-и (пропускаємо disabled)
        Map<String, GraphNode> graphNodes = new HashMap<>();
        List<GraphNode> triggerNodes = new ArrayList<>();

        for (Node node : workflow.getNodes()) {
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

        // 2. Будуємо edges (outgoing + incoming)
        Map<String, List<GraphEdge>> outgoing = new HashMap<>();
        Map<String, List<GraphEdge>> incoming = new HashMap<>();

        buildEdges(workflow.getConnections(), graphNodes, outgoing, incoming);

        // 3. Збираємо граф
        WorkflowExecutionGraph graph = WorkflowExecutionGraph.builder()
                .workflowId(workflowId)
                .nodes(graphNodes)
                .triggerNodes(triggerNodes)
                .outgoingEdges(outgoing)
                .incomingEdges(incoming)
                .build();

        if (!graph.isValid()) {
            throw new IllegalStateException(
                    "Invalid workflow graph for workflow " + workflowId + ": must have at least one trigger node"
            );
        }

        log.info(
                "Built graph for workflow {}: {} nodes, {} triggers, {} edges",
                workflowId,
                graphNodes.size(),
                triggerNodes.size(),
                outgoing.values().stream().mapToInt(List::size).sum()
        );

        return graph;
    }

    /**
     * Створити GraphNode з Node entity
     */
    private GraphNode createGraphNode(Node node) {
        Map<String, Object> credentials = new HashMap<>();

        if (node.getCredential() != null) {
            try {
                String decryptedData = credentialEncryption.decrypt(
                        node.getCredential().getEncryptedData()
                );
                credentials = deserializeCredentials(decryptedData);
            } catch (Exception e) {
                log.error("Failed to decrypt credentials for node {}: {}", node.getNodeId(), e.getMessage());
            }
        }

        return GraphNode.builder()
                .nodeId(node.getNodeId())
                .discriminator(node.getDiscriminator())
                .parameters(node.getParameters())
                .credentials(credentials)
                .build();
    }

    private void buildEdges(
            Set<Connection> connections,
            Map<String, GraphNode> graphNodes,
            Map<String, List<GraphEdge>> outgoing,
            Map<String, List<GraphEdge>> incoming) {

        for (Connection connection : connections) {
            String srcId = connection.getSourceNode().getNodeId();
            String tgtId = connection.getTargetNode().getNodeId();

            if (!graphNodes.containsKey(srcId) || !graphNodes.containsKey(tgtId)) continue;

            GraphEdge edge = new GraphEdge(
                    srcId,
                    connection.getSourceOutputIndex(),
                    tgtId,
                    connection.getTargetInputIndex()
            );

            outgoing.computeIfAbsent(srcId, k -> new ArrayList<>()).add(edge);
            incoming.computeIfAbsent(tgtId, k -> new ArrayList<>()).add(edge);
        }
    }

    private Map<String, Object> deserializeCredentials(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize credentials: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}