package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.domain.enums.CredentialType;
import com.workflow.sociallabs.domain.enums.NodeCategory;
import com.workflow.sociallabs.domain.enums.NodeExecutorDefinition;
import com.workflow.sociallabs.domain.enums.NodeType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class NodeDefinition {

    protected NodeType type;
    protected NodeExecutorDefinition executor;
    protected NodeCategory category;
    protected String name;
    protected String description;
    protected String icon;
    protected String color;
    protected List<NodeParameter<?>> parameters = new ArrayList<>();
    protected CredentialType supportedCredentialType;
    protected Map<String, OutputDefinition> outputs = new LinkedHashMap<>();

    /**
     * Ініціалізація параметрів ноди
     * Має бути імплементована в кожній конкретній ноді
     */
    protected abstract void defineParameters();

    /**
     * Ініціалізація виходів ноди
     */
    protected abstract void defineOutputs();

    /**
     * Отримати клас executor для цієї ноди
     */
    public abstract Class<? extends NodeExecutor> getExecutorClass();

    /**
     * Додати параметр до ноди
     */
    protected <T> void addParameter(NodeParameter<T> parameter) {
        parameters.add(parameter);
    }

    /**
     * Додати вихід до ноди
     */
    protected void addOutput(String name, OutputDefinition output) {
        outputs.put(name, output);
    }

    /**
     * Отримати всі параметри
     */
    public List<NodeParameter<?>> getParameters() {
        if (parameters.isEmpty()) {
            defineParameters();
        }
        return parameters;
    }

    /**
     * Конвертувати в JSON схему для фронтенду
     */
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", executor);
        schema.put("name", name);
        schema.put("description", description);
        schema.put("category", category);
        schema.put("icon", icon);
        schema.put("color", color);
        schema.put("nodeType", type.name());

        List<Map<String, Object>> paramsSchema = new ArrayList<>();
        for (NodeParameter<?> param : getParameters()) {
            paramsSchema.add(parameterToSchema(param));
        }
        schema.put("parameters", paramsSchema);

        schema.put("outputs", outputs);
        schema.put("credentials", supportedCredentialType);

        return schema;
    }

    /**
     * Конвертувати параметр в JSON схему
     */
    protected Map<String, Object> parameterToSchema(NodeParameter<?> param) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("name", param.getName());
        schema.put("displayName", param.getDisplayName());
        schema.put("description", param.getDescription());
        schema.put("type", param.getType());
        schema.put("required", param.isRequired());
        schema.put("default", param.getDefaultValue());

        if (param.isHidden()) {
            schema.put("hidden", true);
        }

        if (param.getDisplayCondition() != null) {
            schema.put("displayOptions", Map.of("show", param.getDisplayCondition()));
        }

        return schema;
    }
}
