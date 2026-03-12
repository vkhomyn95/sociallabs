package com.workflow.sociallabs.node.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Статус workflow
 */
@Getter
@Builder
@AllArgsConstructor
public class WorkflowStatus {

    private final Long workflowId;
    private final boolean active;
    private final int triggerCount;
}
