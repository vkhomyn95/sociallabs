package com.workflow.sociallabs.node.core;

import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NodeResult {

    private boolean success;
    private List<Map<String, Object>> data;
    private Map<String, List<Map<String, Object>>> outputByPort; // For multiple outputs
    private String error;
    private String errorStack;
    private Map<String, Object> metadata;
    private Long executionTimeMs;

    /**
     * Створити успішний результат
     */
    public static NodeResult success(List<Map<String, Object>> data) {
        return NodeResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Створити успішний результат з одним елементом
     */
    public static NodeResult success(Map<String, Object> item) {
        return success(Collections.singletonList(item));
    }

    /**
     * Створити результат з помилкою
     */
    public static NodeResult error(String message, String stack) {
        return NodeResult.builder()
                .success(false)
                .error(message)
                .errorStack(stack)
                .build();
    }

    /**
     * Створити результат з множинними виходами
     */
    public static NodeResult multiOutput(Map<String, List<Map<String, Object>>> outputs) {
        return NodeResult.builder()
                .success(true)
                .outputByPort(outputs)
                .build();
    }
}
