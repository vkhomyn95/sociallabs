package com.workflow.sociallabs.exception;

import com.workflow.sociallabs.node.core.NodeResult;
import lombok.Getter;

@Getter
public class NodeExecutionException extends WorkflowException {

    private final NodeResult nodeResult;

    public NodeExecutionException(String message, NodeResult nodeResult) {
        super(message);
        this.nodeResult = nodeResult;
    }

    public NodeResult getNodeResult() {
        return nodeResult;
    }
}
