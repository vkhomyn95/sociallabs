package com.workflow.sociallabs.node.core;


import com.workflow.sociallabs.execution.engine.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener для trigger events
 * Викликає WorkflowExecutionService для виконання workflow
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerEventListener {

    private final WorkflowExecutionService executionService;

    /**
     * Обробити trigger event
     * Асинхронно запускає workflow
     */
    @EventListener
    @Async
    public void handleTriggerEvent(TriggerEvent event) {
        log.info("Handling trigger event for workflow {} node {} type {}", event.getWorkflowId(), event.getTriggerNodeId(), event.getName());

        try {
            // Запускаємо workflow
            executionService.executeWorkflow(
                    event.getWorkflowId(),
                    event.getTriggerNodeId(),
                    event.getEvent()
            ).exceptionally(ex -> {
                log.error("Workflow execution failed for trigger event: {}", ex.getMessage(), ex);
                return WorkflowExecutionResult.error(ex);
            });

        } catch (Exception e) {
            log.error("Error handling trigger event: {}", e.getMessage(), e);
        }
    }
}
