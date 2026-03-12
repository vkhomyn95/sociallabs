package com.workflow.sociallabs.service;

import com.workflow.sociallabs.domain.converter.NodeParametersConverter;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.node.base.AbstractTriggerNode;
import com.workflow.sociallabs.node.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manager для управління тригерами одного workflow
 * Відповідає за активацію/деактивацію та обробку подій
 */
@Slf4j
public class WorkflowTriggerManager {

    private final Workflow workflow;
    private final WorkflowExecutionGraph graph;
    private final NodeExecutorRegistry executorRegistry;
    private final TriFunction<Long, String, Map<String, Object>, CompletableFuture<WorkflowExecutionResult>> workflowExecutor;

    // Активовані тригери: nodeId -> TriggerContext
    private final Map<String, TriggerContext> activeTriggers = new HashMap<>();

    public WorkflowTriggerManager(
            Workflow workflow,
            WorkflowExecutionGraph graph,
            NodeExecutorRegistry executorRegistry,
            TriFunction<Long, String, Map<String, Object>, CompletableFuture<WorkflowExecutionResult>> workflowExecutor
    ) {

        this.workflow = workflow;
        this.graph = graph;
        this.executorRegistry = executorRegistry;
        this.workflowExecutor = workflowExecutor;
    }

    /**
     * Активувати всі тригери workflow
     */
    public void activateAllTriggers() {
        List<GraphNode> triggerNodes = graph.getTriggerNodes();

        log.info("Activating {} trigger(s) for workflow {}", triggerNodes.size(), workflow.getId());

        for (GraphNode triggerNode : triggerNodes) {
            try {
                activateTrigger(triggerNode);
            } catch (Exception e) {
                log.error("Failed to activate trigger {}: {}", triggerNode.getNodeId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Активувати окремий тригер
     */
    private void activateTrigger(GraphNode triggerNode) throws Exception {
        String nodeId = triggerNode.getNodeId();

        log.debug("Activating trigger: {}", nodeId);

        // Отримуємо executor для тригера
        AbstractTriggerNode triggerExecutor = (AbstractTriggerNode) executorRegistry.getExecutor(triggerNode.getDiscriminator());

        // Створюємо execution context
        ExecutionContext context = ExecutionContext.builder()
                .nodeId(nodeId)
                .workflowId(workflow.getId())
                .parameters(NodeParametersConverter.toTypedParameters(triggerNode.getParameters()))
                .credentials(triggerNode.getCredentials())
                .build();

        // Активуємо тригер
        boolean activated = triggerExecutor.activate(context);

        if (!activated) {
            throw new IllegalStateException("Failed to activate trigger: " + nodeId);
        }

        // Зберігаємо контекст тригера
        TriggerContext triggerContext = new TriggerContext(
                triggerNode,
                triggerExecutor,
                context
        );

        activeTriggers.put(nodeId, triggerContext);

        log.info("Trigger {} activated successfully", nodeId);
    }

    /**
     * Деактивувати всі тригери
     */
    public void deactivateAllTriggers() {
        log.info("Deactivating {} trigger(s) for workflow {}", activeTriggers.size(), workflow.getId());

        for (Map.Entry<String, TriggerContext> entry : activeTriggers.entrySet()) {
            try {
                deactivateTrigger(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("Failed to deactivate trigger {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }

        activeTriggers.clear();
    }

    /**
     * Деактивувати окремий тригер
     */
    private void deactivateTrigger(String nodeId, TriggerContext triggerContext) throws Exception {
        log.debug("Deactivating trigger: {}", nodeId);

        triggerContext.getTriggerExecutor().deactivate(triggerContext.getContext());

        log.info("Trigger {} deactivated successfully", nodeId);
    }

    /**
     * Обробити подію від тригера
     * Викликається коли тригер отримує нову подію
     */
    public void handleTriggerEvent(String nodeId, Map<String, Object> eventData) {
        log.debug("Handling trigger event for node: {}", nodeId);

        TriggerContext triggerContext = activeTriggers.get(nodeId);

        if (triggerContext == null) {
            log.warn("Received event for inactive trigger: {}", nodeId);
            return;
        }

        // Запускаємо виконання workflow
        workflowExecutor.apply(workflow.getId(), nodeId, eventData)
                .exceptionally(ex -> {
                    log.error("Workflow execution failed for trigger {}: {}",
                            nodeId, ex.getMessage(), ex);
                    return WorkflowExecutionResult.error(ex);
                });
    }

    /**
     * Отримати кількість активних тригерів
     */
    public int getTriggerCount() {
        return activeTriggers.size();
    }

    /**
     * Перевірити чи тригер активний
     */
    public boolean isTriggerActive(String nodeId) {
        return activeTriggers.containsKey(nodeId);
    }

    /**
     * Контекст окремого тригера
     */
    private static class TriggerContext {
        private final GraphNode triggerNode;
        private final AbstractTriggerNode triggerExecutor;
        private final ExecutionContext context;

        public TriggerContext(
                GraphNode triggerNode,
                AbstractTriggerNode triggerExecutor,
                ExecutionContext context) {

            this.triggerNode = triggerNode;
            this.triggerExecutor = triggerExecutor;
            this.context = context;
        }

        public GraphNode getTriggerNode() {
            return triggerNode;
        }

        public AbstractTriggerNode getTriggerExecutor() {
            return triggerExecutor;
        }

        public ExecutionContext getContext() {
            return context;
        }
    }

    /**
     * Функціональний інтерфейс для трьох параметрів
     */
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}