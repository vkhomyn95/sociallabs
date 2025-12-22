package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

/**
 * Параметр типу Boolean
 */
@Getter
@Setter
public class BooleanParameter extends NodeParameter<Boolean> {

    @Builder
    public BooleanParameter(
            String name,
            String displayName,
            String description,
            Boolean defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
    }

    @Override
    public boolean validate(Boolean value) {
        if (value == null) {
            return !isRequired();
        }
        return true;
    }

    @Override
    public Boolean parseValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }

        return Boolean.parseBoolean(rawValue.toString());
    }

    @Override
    public String getType() {
        return "boolean";
    }
}
