package com.workflow.sociallabs.execution.engine;

import com.workflow.sociallabs.domain.converter.NodeParametersConverter;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.exception.NodeExecutionException;
import com.workflow.sociallabs.node.core.*;
import com.workflow.sociallabs.service.WorkflowExecutionCache;
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

    private final NodeExecutorRegistry executorRegistry;
    private final WorkflowExecutionCache graphCache;

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
        WorkflowExecutionGraph graph = graphCache.getGraph(workflow.getId());

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

                WorkflowExecutionGraph graph = graphCache.getGraph(workflowId);

                // Створюємо execution context
                WorkflowExecutionContext ctx = new WorkflowExecutionContext(workflowId, triggerNodeId, triggerData);

                executeGraph(graph, triggerNodeId, ctx);

                return WorkflowExecutionResult.success(ctx);

            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                return WorkflowExecutionResult.error(e);
            }
        }, executorService);
    }

    /**
     * Queue-based engine.
     * ExecutionJob містить: nodeId + inputItems по reference.
     * Engine не знає семантику портів — тільки індекси.
     */
    private void executeGraph(
            WorkflowExecutionGraph graph,
            String startNodeId,
            WorkflowExecutionContext ctx) {

        record ExecutionJob(String nodeId, List<WorkflowItem> inputItems) {}

        Deque<ExecutionJob> queue = new ArrayDeque<>();

        // Стартовий job: trigger отримує triggerData як один item
        queue.add(new ExecutionJob(
                startNodeId,
                WorkflowItem.single(ctx.getTriggerData())
        ));

        while (!queue.isEmpty()) {
            ExecutionJob job = queue.poll();
            String nodeId = job.nodeId();

            // Merge node: чекаємо поки всі вхідні виконані
            if (!areAllIncomingExecuted(nodeId, graph, ctx)) {
                queue.addLast(job); // повернути в кінець черги
                continue;
            }

            // Вже виконана (може прийти кілька разів для merge)
            if (ctx.isExecuted(nodeId)) continue;

            GraphNode node = graph.getNode(nodeId);

            NodeResult result = executeNode(node, job.inputItems(), ctx);
            ctx.setNodeResult(nodeId, result);

            if (!result.isSuccess()) {
                throw new NodeExecutionException("Node execution failed: " + nodeId, result);
            }

            // Для кожного вихідного ребра — додати наступний job
            // items передаються по reference (не копіюємо)
            for (GraphEdge edge : graph.getOutgoing(nodeId)) {
                List<WorkflowItem> portItems = result.getOutputItems(edge.sourceOutputIndex());

                // IF node може мати пустий порт — не додаємо job
                if (portItems.isEmpty()) continue;

                // Для merge нод: збираємо items з усіх вхідних
                List<WorkflowItem> targetInput = collectInputForNode(edge.targetNodeId(), graph, ctx, portItems);

                queue.add(new ExecutionJob(edge.targetNodeId(), targetInput));
            }
        }
    }

    /**
     * Перевірити чи всі вхідні nodes виконані (для merge)
     */
    private boolean areAllIncomingExecuted(String nodeId, WorkflowExecutionGraph graph, WorkflowExecutionContext ctx) {
        return graph.getIncoming(nodeId)
                .stream()
                .allMatch(edge -> ctx.isExecuted(edge.sourceNodeId()));
    }

    /**
     * Зібрати items для node з усіх вхідних ребер по reference
     */
    private List<WorkflowItem> collectInputForNode(
            String nodeId,
            WorkflowExecutionGraph graph,
            WorkflowExecutionContext ctx,
            List<WorkflowItem> currentEdgeItems) {

        List<GraphEdge> incoming = graph.getIncoming(nodeId);

        // Проста node (одне вхідне) — передаємо напряму по reference
        if (incoming.size() == 1) return currentEdgeItems;

        // Merge node — збираємо всі виконані входи
        List<WorkflowItem> merged = new ArrayList<>(currentEdgeItems);
        for (GraphEdge inc : incoming) {
            NodeResult prevResult = ctx.getNodeResult(inc.sourceNodeId());
            if (prevResult != null) {
                merged.addAll(prevResult.getOutputItems(inc.sourceOutputIndex()));
            }
        }
        return merged;
    }

    /**
     * Виконати одну node
     */
    private NodeResult executeNode(
            GraphNode node,
            List<WorkflowItem> inputItems,
            WorkflowExecutionContext ctx) {

        NodeExecutor executor = executorRegistry.getExecutor(node.getDiscriminator());

        ExecutionContext nodeContext = ExecutionContext.builder()
                .nodeId(node.getNodeId())
                .workflowId(ctx.getWorkflowId())
                .inputItems(inputItems)   // по reference
                .parameters(NodeParametersConverter.toTypedParameters(node.getParameters()))
                .credentials(node.getCredentials())
                .build();

        return executor.execute(nodeContext);
    }

    @PreDestroy
    public void cleanup() {
        new ArrayList<>(activeTriggers.keySet()).forEach(this::deactivateWorkflow);
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