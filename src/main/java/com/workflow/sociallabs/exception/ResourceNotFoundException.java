package com.workflow.sociallabs.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends WorkflowException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND.name());
    }
}
