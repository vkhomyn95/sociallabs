package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.node.core.NodeParameter;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Параметр для collection (масив об'єктів з фіксованою структурою)
 */
@Getter
@Setter
public class CollectionParameter extends NodeParameter<List<Map<String, Object>>> {

    private List<NodeParameter<?>> fields; // Поля кожного елемента

    @Builder
    public CollectionParameter(
            String name,
            String displayName,
            String description,
            List<Map<String, Object>> defaultValue,
            boolean required,
            boolean hidden,
            String displayCondition,
            List<NodeParameter<?>> fields
    ) {
        super(name, displayName, description, defaultValue, required, hidden, displayCondition);
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    @Override
    public boolean validate(List<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            return !isRequired();
        }

        // Валідація кожного елемента
        for (Map<String, Object> item : value) {
            for (NodeParameter<?> field : fields) {
                Object fieldValue = item.get(field.getName());
                // Спрощена валідація - має бути типізована
                if (field.isRequired() && fieldValue == null) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof List) {
            return (List<Map<String, Object>>) rawValue;
        }

        return null;
    }

    @Override
    public String getType() {
        return "collection";
    }
}