package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Параметр для посилання на credential
 */
@Getter
@Setter
public class CredentialParameter extends NodeParameter<Long> {

    private List<String> credentialTypes; // Які типи credentials підтримуються

    @Builder
    public CredentialParameter(
            String name,
            String displayName,
            String description,
            Long defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            List<String> credentialTypes
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.credentialTypes = credentialTypes != null ? credentialTypes : new ArrayList<>();
    }

    @Override
    public boolean validate(Long value) {
        if (value == null) {
            return !isRequired();
        }
        return value > 0;
    }

    @Override
    public Long parseValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof Number) {
            return ((Number) rawValue).longValue();
        }

        try {
            return Long.parseLong(rawValue.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getType() {
        return "credential";
    }
}

