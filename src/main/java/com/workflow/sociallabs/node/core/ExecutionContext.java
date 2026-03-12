package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Контекст виконання node з повною підтримкою типізованих параметрів
 */
@Builder
@Getter
@Setter
public class ExecutionContext {

    // ========== Основні поля ==========

    private Long executionId;
    private Long workflowId;
    private String nodeId;
    private Node node;

    // ==================================

    private List<Map<String, Object>> inputData;
    @Builder.Default
    private Map<String, Object> workflowVariables = new HashMap<>();
    private TypedNodeParameters parameters;


    // TODO: 12.12.25 Think to remove
//    private Map<String, Object> parameters;
    private Map<String, Object> credentials;
    private ExecutionContext previousNode;
    private Map<String, Object> workflowData; // Shared workflow data
    private Instant startTime;

    /**
     * Отримати типізовані параметри
     */
    @SuppressWarnings("unchecked")
    public <T extends TypedNodeParameters> T getParameters(Class<T> parametersClass) {
        if (parameters == null) {
            throw new IllegalStateException("Typed parameters not initialized");
        }

        if (!parametersClass.isInstance(parameters)) {
            throw new IllegalStateException(
                    "Expected " + parametersClass.getName() + " but got " + parameters.getClass().getName()
            );
        }

        return (T) parameters;
    }

    /**
     * Legacy метод для сумісності
     * @deprecated Використовуйте getTypedParameters() замість цього
     */
    @Deprecated
    public <T> T getParameter(String key, Class<T> type) {
        Object value = node.getParameters().get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * Legacy метод для сумісності
     * @deprecated Використовуйте getTypedParameters() замість цього
     */
    @Deprecated
    public <T> T getParameter(String key, Class<T> type, T defaultValue) {
        T value = getParameter(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Отримати credential значення
     */
    @SuppressWarnings("unchecked")
    public <T> T getCredential(String key, Class<T> type) {
        Object value = credentials.get(key);
        return value != null ? type.cast(value) : null;
    }

    /**
     * Отримати дані з попередньої ноди
     */
    public List<Map<String, Object>> getInputData() {
        return inputData != null ? inputData : Collections.emptyList();
    }

    /**
     * Отримати перший елемент вхідних даних
     */
    public Map<String, Object> getFirstInputItem() {
        List<Map<String, Object>> input = getInputData();
        return !input.isEmpty() ? input.get(0) : Collections.emptyMap();
    }
}
