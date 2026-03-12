package com.workflow.sociallabs.execution.engine;

import com.workflow.sociallabs.domain.converter.NodeParametersConverter;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.domain.repository.WorkflowRepository;
import com.workflow.sociallabs.exception.NodeExecutionException;
import com.workflow.sociallabs.node.core.*;
import com.workflow.sociallabs.service.WorkflowExecutionGraphCache;
import com.workflow.sociallabs.service.WorkflowTriggerManager;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

/**
 * Сервіс для управління виконанням workflow
 * Відповідає за lifecycle тригерів та orchestration виконання нод
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final WorkflowRepository workflowRepository;
    private final NodeExecutorRegistry executorRegistry;
    private final WorkflowExecutionGraphCache graphCache;

    // Thread pool для асинхронного виконання workflow
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Активні тригери: workflowId -> TriggerManager
    private final Map<Long, WorkflowTriggerManager> activeTriggers = new ConcurrentHashMap<>();

    /**
     * Активувати всі тригери для workflow
     */
    @Transactional
    public void activateWorkflow(Workflow workflow) {
        log.info("Activating workflow: {}", workflow.getId());

        if (!workflow.getActive()) {
            throw new IllegalStateException("Workflow is not active: " + workflow.getId());
        }

        // Якщо вже активований - деактивуємо старий
        if (activeTriggers.containsKey(workflow.getId())) {
            deactivateWorkflow(workflow.getId());
        }

        // Завантажуємо execution graph в кеш
        WorkflowExecutionGraph graph = graphCache.getOrBuildGraph(workflow.getId());

        // Створюємо manager для тригерів
        WorkflowTriggerManager triggerManager = new WorkflowTriggerManager(
                workflow,
                graph,
                executorRegistry,
                this::executeWorkflow
        );

        // Активуємо всі тригери. Зараз in executor беруться дані з сервісів, які виконуються
        triggerManager.activateAllTriggers();

        activeTriggers.put(workflow.getId(), triggerManager);

        log.info("Workflow {} activated with {} trigger(s)", workflow.getId(), triggerManager.getTriggerCount());
    }

    /**
     * Деактивувати всі тригери для workflow
     */
    public void deactivateWorkflow(Long workflowId) {
        log.info("Deactivating workflow: {}", workflowId);

        WorkflowTriggerManager triggerManager = activeTriggers.remove(workflowId);

        if (triggerManager != null) {
            triggerManager.deactivateAllTriggers();
            log.info("Workflow {} deactivated", workflowId);
        }
    }

    /**
     * Виконати workflow починаючи з тригера
     * Викликається коли тригер отримує подію
     */
    public CompletableFuture<WorkflowExecutionResult> executeWorkflow(
            Long workflowId,
            String triggerNodeId,
            Map<String, Object> triggerData) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing workflow {} triggered by node {}", workflowId, triggerNodeId);

                WorkflowExecutionGraph graph = graphCache.getOrBuildGraph(workflowId);

                // Створюємо execution context
                WorkflowExecutionContext executionContext = new WorkflowExecutionContext(
                        workflowId,
                        triggerNodeId,
                        triggerData
                );

                // Виконуємо граф починаючи з тригера
                executeGraph(graph, triggerNodeId, executionContext);

                return WorkflowExecutionResult.success(executionContext);

            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                return WorkflowExecutionResult.error(e);
            }
        }, executorService);
    }

    /**
     * Виконати граф нод починаючи з заданої ноди
     */
    private void executeGraph(
            WorkflowExecutionGraph graph,
            String startNodeId,
            WorkflowExecutionContext executionContext) throws Exception {

        Queue<GraphNode> executionQueue = new LinkedList<>();
        Set<String> executed = new HashSet<>();

        // Додаємо стартову ноду
        GraphNode startNode = graph.getNode(startNodeId);
        executionQueue.offer(startNode);

        while (!executionQueue.isEmpty()) {
            GraphNode currentNode = executionQueue.poll();

            if (executed.contains(currentNode.getNodeId())) {
                continue;
            }

            // Перевіряємо чи всі залежності виконані
            if (!areAllDependenciesExecuted(currentNode, executed)) {
                // Повертаємо в чергу для повторної обробки
                executionQueue.offer(currentNode);
                continue;
            }

            // Виконуємо ноду
            executeNode(currentNode, executionContext);
            executed.add(currentNode.getNodeId());

            // Додаємо наступні ноди в чергу
            for (GraphNode nextNode : currentNode.getNextNodes()) {
                if (!executed.contains(nextNode.getNodeId())) {
                    executionQueue.offer(nextNode);
                }
            }
        }
    }

    /**
     * Перевірити чи всі залежності виконані
     */
    private boolean areAllDependenciesExecuted(GraphNode node, Set<String> executed) {
        return node.getPreviousNodes().stream()
                .allMatch(prev -> executed.contains(prev.getNodeId()));
    }

    /**
     * Виконати одну ноду
     */
    private void executeNode(GraphNode graphNode, WorkflowExecutionContext executionContext) throws Exception {

        String nodeId = graphNode.getNodeId();
        log.debug("Executing node: {}", nodeId);

        // Отримуємо executor для ноди
        NodeExecutor executor = executorRegistry.getExecutor(graphNode.getDiscriminator());

        // Підготовка input data з попередніх нод
        List<Map<String, Object>> inputData = collectInputData(graphNode, executionContext);

        // Створення execution context для ноди
        ExecutionContext nodeContext = ExecutionContext.builder()
                .nodeId(nodeId)
                .workflowId(executionContext.getWorkflowId())
                .inputData(inputData)
                .parameters(NodeParametersConverter.toTypedParameters(graphNode.getParameters()))
                .credentials(graphNode.getCredentials())
                .build();

        // Виконання ноди
        NodeResult result = executor.execute(nodeContext);

        // Збереження результату
        executionContext.setNodeResult(nodeId, result);

        if (!result.isSuccess()) {
            log.error("Node {} failed: {}", nodeId, result.getError());
            throw new NodeExecutionException("Node execution failed: " + nodeId, result);
        }

        log.debug("Node {} completed successfully", nodeId);
    }

    /**
     * Зібрати input data з попередніх нод
     */
    private List<Map<String, Object>> collectInputData(
            GraphNode node,
            WorkflowExecutionContext executionContext) {

        if (node.getPreviousNodes().isEmpty()) {
            // Для тригера використовуємо початкові дані
            return Collections.singletonList(executionContext.getTriggerData());
        }

        // Збираємо дані з усіх попередніх нод
        List<Map<String, Object>> allData = new ArrayList<>();

        for (GraphNode prevNode : node.getPreviousNodes()) {
            NodeResult prevResult = executionContext.getNodeResult(prevNode.getNodeId());
            if (prevResult != null && prevResult.getData() != null) {
                allData.addAll(prevResult.getData());
            }
        }

        return allData;
    }

    /**
     * Перезавантажити workflow (reload після змін)
     */
//    public void reloadWorkflow(Long workflowId) {
//        log.info("Reloading workflow: {}", workflowId);
//
//        // Видаляємо з кешу
//        graphCache.invalidate(workflowId);
//
//        // Якщо активний - перезапускаємо
//        if (activeTriggers.containsKey(workflowId)) {
//            deactivateWorkflow(workflowId);
//            activateWorkflow(workflowId);
//        }
//    }

    /**
     * Отримати статус workflow
     */
    public WorkflowStatus getWorkflowStatus(Long workflowId) {
        WorkflowTriggerManager manager = activeTriggers.get(workflowId);

        return WorkflowStatus.builder()
                .workflowId(workflowId)
                .active(manager != null)
                .triggerCount(manager != null ? manager.getTriggerCount() : 0)
                .build();
    }

    /**
     * Cleanup при завершенні роботи
     */
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down workflow execution service");

        // Деактивуємо всі тригери
        new ArrayList<>(activeTriggers.keySet()).forEach(this::deactivateWorkflow);

        // Завершуємо executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}