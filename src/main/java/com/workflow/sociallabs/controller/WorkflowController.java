package com.workflow.sociallabs.controller;

import com.workflow.sociallabs.dto.request.WorkflowRequest;
import com.workflow.sociallabs.dto.response.ExecutionResponse;
import com.workflow.sociallabs.dto.response.WorkflowResponse;
import com.workflow.sociallabs.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller для роботи з Workflows
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * Отримати всі workflows
     */
    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        log.info("GET /api/v1/workflows - Fetching all workflows");
        List<WorkflowResponse> workflows = workflowService.getAllWorkflows();
        return ResponseEntity.ok(workflows);
    }

    /**
     * Отримати workflow за ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable Long id) {
        log.info("GET /api/v1/workflows/{} - Fetching workflow", id);
        WorkflowResponse workflow = workflowService.getWorkflowById(id);
        return ResponseEntity.ok(workflow);
    }

    /**
     * Створити новий workflow
     */
    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody WorkflowRequest request
    ) {
        log.info("POST /api/v1/workflows - Creating new workflow: {}", request.getName());
        WorkflowResponse workflow = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflow);
    }

    /**
     * Оновити workflow
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowRequest request
    ) {
        log.info("PUT /api/v1/workflows/{} - Updating workflow", id);
        WorkflowResponse workflow = workflowService.updateWorkflow(id, request);
        return ResponseEntity.ok(workflow);
    }

    /**
     * Видалити workflow
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        log.info("DELETE /api/v1/workflows/{} - Deleting workflow", id);
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Активувати/деактивувати workflow
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<WorkflowResponse> toggleWorkflow(@PathVariable Long id) {
        log.info("POST /api/v1/workflows/{}/toggle - Toggling workflow", id);
        WorkflowResponse workflow = workflowService.toggleWorkflow(id);
        return ResponseEntity.ok(workflow);
    }

    /**
     * Виконати workflow вручну
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ExecutionResponse> executeWorkflow(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> triggerData
    ) {
        log.info("POST /api/v1/workflows/{}/execute - Executing workflow manually", id);
        ExecutionResponse execution = workflowService.executeWorkflow(id, triggerData);
        return ResponseEntity.ok(execution);
    }

    /**
     * Отримати історію виконань workflow
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<ExecutionResponse>> getWorkflowExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/v1/workflows/{}/executions - Fetching executions", id);
        List<ExecutionResponse> executions = workflowService.getWorkflowExecutions(id, page, size);
        return ResponseEntity.ok(executions);
    }
}
