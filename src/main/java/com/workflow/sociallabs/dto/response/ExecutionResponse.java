package com.workflow.sociallabs.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для результату виконання workflow
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponse {

    private Long id;
    private Long workflowId;
    private String status;            // PENDING, RUNNING, SUCCESS, ERROR
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private Map<String, Object> triggerData;
    private String errorMessage;
    private List<NodeExecutionLogDto> logs;

    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeExecutionLogDto {
        private String nodeId;
        private String nodeName;
        private String status;
        private LocalDateTime executedAt;
        private Long durationMs;
        private Map<String, Object> inputData;
        private Map<String, Object> outputData;
        private String errorMessage;
    }
}