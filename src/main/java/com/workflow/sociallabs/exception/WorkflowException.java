package com.workflow.sociallabs.exception;

public class WorkflowException extends RuntimeException {

    private final String errorCode;

    public WorkflowException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}