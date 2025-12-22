package com.workflow.sociallabs.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class ValidationException extends WorkflowException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = Collections.emptyMap();
    }
}