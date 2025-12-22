package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

import java.util.*;

/**
 * Параметр типу Options (dropdown select)
 */
@Getter
@Setter
public class OptionsParameter extends NodeParameter<String> {

    private List<OptionValue> options;

    @Builder
    public OptionsParameter(
            String name,
            String displayName,
            String description,
            String defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            List<OptionValue> options
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.options = options != null ? options : new ArrayList<>();
    }

    @Override
    public boolean validate(String value) {
        if (value == null) {
            return !isRequired();
        }

        return options.stream()
                .anyMatch(opt -> opt.getValue().equals(value));
    }

    @Override
    public String parseValue(Object rawValue) {
        return rawValue != null ? rawValue.toString() : null;
    }

    @Override
    public String getType() {
        return "options";
    }

    /**
     * Додати опцію
     */
    public OptionsParameter addOption(String value, String name) {
        options.add(new OptionValue(value, name));
        return this;
    }

    /**
     * Додати опцію з описом
     */
    public OptionsParameter addOption(String value, String name, String description) {
        options.add(new OptionValue(value, name, description));
        return this;
    }

    @Getter @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptionValue {
        private String value;
        private String name;
        private String description;

        public OptionValue(String value, String name) {
            this.value = value;
            this.name = name;
        }
    }
}
