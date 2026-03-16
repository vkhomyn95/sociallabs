package com.workflow.sociallabs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller для роботи з Executions
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExecutionController {

//    private final ExecutionService executionService;

//    /**
//     * Отримати execution за ID
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<ExecutionResponse> getExecution(@PathVariable Long id) {
//        log.info("GET /api/v1/executions/{} - Fetching execution", id);
//        ExecutionResponse execution = executionService.getExecutionById(id);
//        return ResponseEntity.ok(execution);
//    }
//
//    /**
//     * Отримати всі executions (з пагінацією)
//     */
//    @GetMapping
//    public ResponseEntity<List<ExecutionResponse>> getAllExecutions(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(required = false) String status
//    ) {
//        log.info("GET /api/v1/executions - Fetching executions (page={}, size={}, status={})",
//                page, size, status);
//        List<ExecutionResponse> executions = executionService.getAllExecutions(page, size, status);
//        return ResponseEntity.ok(executions);
//    }
//
//    /**
//     * Зупинити виконання
//     */
//    @PostMapping("/{id}/stop")
//    public ResponseEntity<ExecutionResponse> stopExecution(@PathVariable Long id) {
//        log.info("POST /api/v1/executions/{}/stop - Stopping execution", id);
//        ExecutionResponse execution = executionService.stopExecution(id);
//        return ResponseEntity.ok(execution);
//    }
//
//    /**
//     * Повторити execution
//     */
//    @PostMapping("/{id}/retry")
//    public ResponseEntity<ExecutionResponse> retryExecution(@PathVariable Long id) {
//        log.info("POST /api/v1/executions/{}/retry - Retrying execution", id);
//        ExecutionResponse execution = executionService.retryExecution(id);
//        return ResponseEntity.ok(execution);
//    }
//
//    /**
//     * Видалити execution
//     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteExecution(@PathVariable Long id) {
//        log.info("DELETE /api/v1/executions/{} - Deleting execution", id);
//        executionService.deleteExecution(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    /**
//     * Отримати логи для конкретної ноди в execution
//     */
//    @GetMapping("/{id}/nodes/{nodeId}/logs")
//    public ResponseEntity<Map<String, Object>> getNodeLogs(
//            @PathVariable Long id,
//            @PathVariable String nodeId
//    ) {
//        log.info("GET /api/v1/executions/{}/nodes/{}/logs - Fetching node logs", id, nodeId);
//        Map<String, Object> logs = executionService.getNodeLogs(id, nodeId);
//        return ResponseEntity.ok(logs);
//    }
}
