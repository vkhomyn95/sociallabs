package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

/**
 * Параметр типу Number
 */
@Getter
@Setter
public class NumberParameter extends NodeParameter<Number> {

    private Number min;
    private Number max;
    private Number step;
    private String numberType; // "integer" or "float"

    @Builder
    public NumberParameter(
            String name,
            String displayName,
            String description,
            Number defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            Number min,
            Number max,
            Number step,
            String numberType
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.min = min;
        this.max = max;
        this.step = step;
        this.numberType = numberType != null ? numberType : "float";
    }

    @Override
    public boolean validate(Number value) {
        if (value == null) {
            return !isRequired();
        }

        double doubleValue = value.doubleValue();

        if (min != null && doubleValue < min.doubleValue()) {
            return false;
        }

        if (max != null && doubleValue > max.doubleValue()) {
            return false;
        }

        return true;
    }

    @Override
    public Number parseValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof Number) {
            return (Number) rawValue;
        }

        try {
            if ("integer".equals(numberType)) {
                return Integer.parseInt(rawValue.toString());
            } else {
                return Double.parseDouble(rawValue.toString());
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getType() {
        return "number";
    }
}