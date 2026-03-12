package com.workflow.sociallabs.config;

import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.domain.repository.WorkflowRepository;
import com.workflow.sociallabs.execution.engine.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listener для автоматичної активації workflows при старті додатку
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowApplicationConfiguration {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService executionService;

    /**
     * Активувати всі активні workflows після повного старту додатку
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - activating workflows...");

        try {
            List<Workflow> activeWorkflows = workflowRepository.findByActive(true);

            log.info("Found {} active workflow(s) to activate", activeWorkflows.size());

            int success = 0;
            int failed = 0;

            for (Workflow workflow : activeWorkflows) {
                try {
                    executionService.activateWorkflow(workflow);
                    success++;
                    log.info("Activated workflow: {} (id={})", workflow.getName(), workflow.getId());
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to activate workflow: {} (id={}) - {}", workflow.getName(), workflow.getId(), e.getMessage(), e);
                }
            }

            log.info("Workflow activation complete: {} successful, {} failed", success, failed);

        } catch (Exception e) {
            log.error("Error during workflow activation: {}", e.getMessage(), e);
        }
    }
}
