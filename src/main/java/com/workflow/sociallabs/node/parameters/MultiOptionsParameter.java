package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

import java.util.*;

/**
 * Параметр типу Multi-Options (multi-select)
 */
@Getter
@Setter
public class MultiOptionsParameter extends NodeParameter<List<String>> {

    private List<OptionsParameter.OptionValue> options;
    private Integer maxValues;

    @Builder
    public MultiOptionsParameter(
            String name,
            String displayName,
            String description,
            List<String> defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            List<OptionsParameter.OptionValue> options,
            Integer maxValues
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.options = options != null ? options : new ArrayList<>();
        this.maxValues = maxValues;
    }

    @Override
    public boolean validate(List<String> value) {
        if (value == null || value.isEmpty()) {
            return !isRequired();
        }

        if (maxValues != null && value.size() > maxValues) {
            return false;
        }

        // Перевірити що всі значення є в options
        Set<String> validValues = new HashSet<>();
        options.forEach(opt -> validValues.add(opt.getValue()));

        return value.stream().allMatch(validValues::contains);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> parseValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof List) {
            List<?> list = (List<?>) rawValue;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item.toString());
            }
            return result;
        }

        return Collections.singletonList(rawValue.toString());
    }

    @Override
    public String getType() {
        return "multiOptions";
    }
}
