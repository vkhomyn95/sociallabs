package com.workflow.sociallabs.dto.request;

import lombok.*;

import java.util.Map;

/**
 * Request для виконання workflow
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowRequest {

    private Long workflowId;
    private Map<String, Object> triggerData;    // Початкові дані
    private String mode;                        // manual, trigger, webhook
}
