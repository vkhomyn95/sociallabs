package com.workflow.sociallabs.execution.engine;

import com.workflow.sociallabs.domain.entity.Connection;
import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.domain.entity.WorkflowExecution;
import com.workflow.sociallabs.domain.enums.ExecutionStatus;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.domain.repository.WorkflowRepository;
import com.workflow.sociallabs.exception.NodeExecutionException;
import com.workflow.sociallabs.node.core.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Головний engine для виконання workflows
 * Керує процесом виконання, порядком нод, передачею даних
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final NodeExecutionService nodeExecutionService;
    private final WorkflowRepository workflowRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<Long, WorkflowExecution> runningExecutions = new ConcurrentHashMap<>();

    /**
     * Виконати workflow
     */
    @Transactional
    public WorkflowExecution executeWorkflow(
            Workflow workflow,
            Map<String, Object> triggerData,
            String mode
    ) {
        log.info("Starting workflow execution: {} (id={})", workflow.getName(), workflow.getId());

        // Створити запис виконання
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflow(workflow)
                .status(ExecutionStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .triggerData(serializeToJson(triggerData))
                .mode(mode)
                .build();

        runningExecutions.put(execution.getId(), execution);

        try {
            // Побудувати граф виконання
            ExecutionGraph graph = buildExecutionGraph(workflow);

            // Знайти start nodes (ноди без вхідних connections або тригери)
            List<Node> startNodes = findStartNodes(workflow, graph);

            if (startNodes.isEmpty()) {
                throw new IllegalStateException("No start nodes found in workflow");
            }

            // Контекст виконання workflow
            Map<String, Object> workflowContext = new HashMap<>();
            workflowContext.put("triggerData", triggerData);
            workflowContext.put("startTime", Instant.now());

            // Результати виконання нод
            Map<String, NodeResult> nodeResults = new ConcurrentHashMap<>();

            // Виконати start nodes
            for (Node startNode : startNodes) {
                executeNodeRecursively(
                        startNode,
                        graph,
                        Collections.singletonList(triggerData),
                        workflowContext,
                        nodeResults,
                        execution
                );
            }

            // Завершити виконання
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setFinishedAt(LocalDateTime.now());

            log.info("Workflow execution completed successfully: {}", execution.getId());

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage(), e);
            execution.setStatus(ExecutionStatus.ERROR);
            execution.setErrorMessage(e.getMessage());
            execution.setFinishedAt(LocalDateTime.now());
        } finally {
            runningExecutions.remove(execution.getId());
        }

        return execution;
    }

    /**
     * Виконати ноду рекурсивно з обробкою наступних нод
     */
    private void executeNodeRecursively(
            Node nodeInstance,
            ExecutionGraph graph,
            List<Map<String, Object>> inputData,
            Map<String, Object> workflowContext,
            Map<String, NodeResult> nodeResults,
            WorkflowExecution execution
    ) throws Exception {

        // Пропустити якщо disabled
        if (nodeInstance.getDisabled()) {
            log.debug("Skipping disabled node: {}", nodeInstance.getNodeId());
            NodeResult skipResult = NodeResult.builder()
                    .success(true)
                    .data(inputData)
                    .build();
            nodeResults.put(nodeInstance.getNodeId(), skipResult);

            // Продовжити з наступними нодами
            executeNextNodes(nodeInstance, graph, inputData, workflowContext, nodeResults, execution);
            return;
        }

//        log.info("Executing node: {} ({})", nodeInstance.getName(), nodeInstance.getNode().getType());

        // Виконати ноду
        NodeResult result = nodeExecutionService.executeNode(
                nodeInstance,
                inputData,
                workflowContext,
                execution
        );

        // Зберегти результат
        nodeResults.put(nodeInstance.getNodeId(), result);

        if (!result.isSuccess()) {
            throw new NodeExecutionException(
                    "Node execution failed: " + nodeInstance.getName(),
                    result
            );
        }

        // Виконати наступні ноди
        executeNextNodes(
                nodeInstance,
                graph,
                result.getData(),
                workflowContext,
                nodeResults,
                execution
        );
    }

    /**
     * Виконати наступні ноди після поточної
     */
    private void executeNextNodes(
            Node currentNode,
            ExecutionGraph graph,
            List<Map<String, Object>> outputData,
            Map<String, Object> workflowContext,
            Map<String, NodeResult> nodeResults,
            WorkflowExecution execution
    ) throws Exception {

        // Знайти всі ноди, які з'єднані з поточною
        List<Connection> outgoingConnections = graph.getOutgoingConnections(currentNode);

        if (outgoingConnections.isEmpty()) {
            log.debug("Node {} has no outgoing connections", currentNode.getNodeId());
            return;
        }

        // Виконати кожну наступну ноду
        for (Connection connection : outgoingConnections) {
            Node nextNode = connection.getTargetNode();

            // Перевірити чи всі вхідні ноди виконані (для merge scenarios)
            if (!areAllInputNodesExecuted(nextNode, graph, nodeResults)) {
                log.debug("Not all input nodes executed for {}, skipping for now",
                        nextNode.getNodeId());
                continue;
            }

            // Зібрати дані з усіх вхідних нод
            List<Map<String, Object>> mergedInputData = mergeInputData(
                    nextNode,
                    graph,
                    nodeResults
            );

            // Виконати рекурсивно
            executeNodeRecursively(
                    nextNode,
                    graph,
                    mergedInputData,
                    workflowContext,
                    nodeResults,
                    execution
            );
        }
    }

    /**
     * Побудувати граф виконання з workflow
     */
    private ExecutionGraph buildExecutionGraph(Workflow workflow) {
        ExecutionGraph graph = new ExecutionGraph();

        for (Connection connection : workflow.getConnections()) {
            graph.addConnection(connection);
        }

        return graph;
    }

    /**
     * Знайти стартові ноди
     */
    private List<Node> findStartNodes(Workflow workflow, ExecutionGraph graph) {
        return workflow.getNodes().stream()
                .filter(node -> {
                    // Тригери завжди є start nodes
                    if (node.getType() == NodeType.TRIGGER) {
                        return true;
                    }
                    // Ноди без вхідних з'єднань
                    return graph.getIncomingConnections(node).isEmpty();
                })
                .collect(Collectors.toList());
    }

    /**
     * Перевірити чи всі вхідні ноди виконані
     */
    private boolean areAllInputNodesExecuted(
            Node node,
            ExecutionGraph graph,
            Map<String, NodeResult> nodeResults
    ) {
        List<Connection> incomingConnections = graph.getIncomingConnections(node);

        return incomingConnections.stream()
                .allMatch(conn -> nodeResults.containsKey(conn.getSourceNode().getNodeId()));
    }

    /**
     * Об'єднати дані з усіх вхідних нод
     */
    private List<Map<String, Object>> mergeInputData(
            Node node,
            ExecutionGraph graph,
            Map<String, NodeResult> nodeResults
    ) {
        List<Connection> incomingConnections = graph.getIncomingConnections(node);

        // Якщо одне вхідне з'єднання - повернути його дані
        if (incomingConnections.size() == 1) {
            NodeResult result = nodeResults.get(
                    incomingConnections.get(0).getSourceNode().getNodeId()
            );
            return result != null ? result.getData() : Collections.emptyList();
        }

        // Якщо декілька - об'єднати всі
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Connection connection : incomingConnections) {
            NodeResult result = nodeResults.get(connection.getSourceNode().getNodeId());
            if (result != null && result.getData() != null) {
                merged.addAll(result.getData());
            }
        }

        return merged;
    }

    /**
     * Активувати workflow (активувати тригери)
     */
    public void activateWorkflow(Workflow workflow) {
        log.info("Activating workflow: {}", workflow.getName());

        // Знайти всі тригер ноди
        List<Node> triggerNodes = workflow.getNodes().stream()
                .filter(node -> node.getType() == NodeType.TRIGGER)
                .collect(Collectors.toList());

        // Активувати кожен тригер
        for (Node triggerNode : triggerNodes) {
            try {
                nodeExecutionService.activateTrigger(triggerNode, workflow);
            } catch (Exception e) {
                log.error("Failed to activate trigger: {}", triggerNode.getName(), e);
            }
        }
    }

    /**
     * Деактивувати workflow
     */
    public void deactivateWorkflow(Workflow workflow) {
        log.info("Deactivating workflow: {}", workflow.getName());

        List<Node> triggerNodes = workflow.getNodes().stream()
                .filter(node -> node.getType() == NodeType.TRIGGER)
                .collect(Collectors.toList());

        for (Node triggerNode : triggerNodes) {
            try {
                nodeExecutionService.deactivateTrigger(triggerNode);
            } catch (Exception e) {
                log.error("Failed to deactivate trigger: {}", triggerNode.getName(), e);
            }
        }
    }

    private String serializeToJson(Object obj) {
        return "{}"; // Simplified
    }
}
