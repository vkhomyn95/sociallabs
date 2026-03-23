package com.workflow.sociallabs.node.nodes.ai.agent.exception;

import com.workflow.sociallabs.node.nodes.ai.agent.AgentResult;

public class AgentExecutionException extends RuntimeException {

    private final AgentResult.FailureType failureType;

    public AgentExecutionException(String message, AgentResult.FailureType failureType) {
        super(message);
        this.failureType = failureType;
    }

    public AgentResult.FailureType getFailureType() { return failureType; }
}
