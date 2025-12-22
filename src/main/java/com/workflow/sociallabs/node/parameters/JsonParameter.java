package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Параметр типу JSON
 */
@Getter
@Setter
public class JsonParameter extends NodeParameter<Map<String, Object>> {

    private String jsonSchema; // JSON Schema для валідації

    @Builder
    public JsonParameter(
            String name,
            String displayName,
            String description,
            Map<String, Object> defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            String jsonSchema
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.jsonSchema = jsonSchema;
    }

    @Override
    public boolean validate(Map<String, Object> value) {
        if (value == null) {
            return !isRequired();
        }

        // Тут може бути валідація через JSON Schema
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof Map) {
            return (Map<String, Object>) rawValue;
        }

        // Якщо це рядок - парсити як JSON
        if (rawValue instanceof String) {
            // Тут має бути парсинг через Jackson
            return new HashMap<>();
        }

        return null;
    }

    @Override
    public String getType() {
        return "json";
    }
}
