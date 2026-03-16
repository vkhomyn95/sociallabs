package com.workflow.sociallabs.node.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record WorkflowItem(
        Map<String, Object> json,
        Map<String, Object> binary
) {
    public static WorkflowItem of(Map<String, Object> json) {
        return new WorkflowItem(
                json != null ? json : Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    public static List<WorkflowItem> single(Map<String, Object> json) {
        return List.of(WorkflowItem.of(json));
    }
}