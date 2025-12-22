package com.workflow.sociallabs.exception;

import lombok.Getter;

@Getter
public class NodeExecutionException extends WorkflowException {
    private final String nodeId;

    public NodeExecutionException(String message, String nodeId) {
        super(message, "NODE_EXECUTION_ERROR");
        this.nodeId = nodeId;
    }
}
