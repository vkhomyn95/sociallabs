package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;


@Builder
@Getter
@Setter
public class ExecutionContext {

    private Long executionId;
    private Long workflowId;
    private String nodeId;

    private List<WorkflowItem> inputItems;

    private TypedNodeParameters parameters;
    private Map<String, Object> credentials;

    public List<WorkflowItem> getInputItems() {
        return inputItems != null ? inputItems : List.of();
    }

    public Map<String, Object> getFirstJson() {
        return getInputItems().isEmpty()
                ? Map.of()
                : getInputItems().get(0).json();
    }

    // Типізовані параметри
    @SuppressWarnings("unchecked")
    public <T extends TypedNodeParameters> T getParameters(Class<T> cls) {
        if (parameters == null) {
            throw new IllegalStateException("Parameters not initialized");
        }
        if (!cls.isInstance(parameters)) {
            throw new IllegalStateException("Expected " + cls.getName() + " but got " + parameters.getClass().getName());
        }
        return (T) parameters;
    }

    public <T> T getCredential(String key, Class<T> type) {
        if (credentials == null) return null;

        Object val = credentials.get(key);
        return val != null ? type.cast(val) : null;
    }
}