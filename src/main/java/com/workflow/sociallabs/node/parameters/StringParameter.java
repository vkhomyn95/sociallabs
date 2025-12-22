package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

/**
 * Параметр типу String
 */
@Getter
@Setter
public class StringParameter extends NodeParameter<String> {

    private Integer minLength;
    private Integer maxLength;
    private String pattern; // Regex pattern
    private String placeholder;
    private boolean multiline;

    @Builder
    public StringParameter(
            String name,
            String displayName,
            String description,
            String defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            Integer minLength,
            Integer maxLength,
            String pattern,
            String placeholder,
            boolean multiline
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.pattern = pattern;
        this.placeholder = placeholder;
        this.multiline = multiline;
    }

    @Override
    public boolean validate(String value) {
        if (value == null) {
            return !isRequired();
        }

        if (minLength != null && value.length() < minLength) {
            return false;
        }

        if (maxLength != null && value.length() > maxLength) {
            return false;
        }

        if (pattern != null && !value.matches(pattern)) {
            return false;
        }

        return true;
    }

    @Override
    public String parseValue(Object rawValue) {
        return rawValue != null ? rawValue.toString() : null;
    }

    @Override
    public String getType() {
        return "string";
    }
}
