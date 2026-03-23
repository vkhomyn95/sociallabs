package com.workflow.sociallabs.node.nodes.ai.agent.exception;

public class MaxIterationsException extends RuntimeException {

    private final int reachedIterations;

    public MaxIterationsException(int reachedIterations) {
        super("Agent reached maximum iterations: " + reachedIterations);
        this.reachedIterations = reachedIterations;
    }

    public int getReachedIterations() { return reachedIterations; }
}
