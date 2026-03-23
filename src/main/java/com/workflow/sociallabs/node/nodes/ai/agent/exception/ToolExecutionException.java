package com.workflow.sociallabs.node.nodes.ai.agent.exception;

public class ToolExecutionException extends Exception {

    private final String  errorCode;
    private final boolean retryable;

    public ToolExecutionException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ToolExecutionException(String errorCode, String message,
                                  boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String  getErrorCode() { return errorCode; }
    public boolean isRetryable()  { return retryable; }
}
