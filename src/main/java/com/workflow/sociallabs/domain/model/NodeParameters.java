package com.workflow.sociallabs.domain.model;

import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Обгортка для параметрів node з метаданими про тип
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeParameters {

    /**
     * Тип параметрів (дискримінатор) - використовується для deserialization
     * Дивитись: {@link NodeDiscriminator}
     */
    private String parameterType;

    /**
     * Сирі значення параметрів (для зворотної сумісності та гнучкості)
     */
    @Builder.Default
    private Map<String, Object> values = new HashMap<>();

    /**
     * Створити з Map
     */
    public static NodeParameters from(Map<String, Object> values) {
        return NodeParameters.builder()
                .values(values != null ? new HashMap<>(values) : new HashMap<>())
                .build();
    }

    /**
     * Створити з типом
     */
    public static NodeParameters withType(String type, Map<String, Object> values) {
        return NodeParameters.builder()
                .parameterType(type)
                .values(values != null ? new HashMap<>(values) : new HashMap<>())
                .build();
    }

    public Object get(String key) {
        return values.get(key);
    }

    public void set(String key, Object value) {
        values.put(key, value);
    }

    /**
     * Чи є тип параметрів
     */
    public boolean hasType() {
        return parameterType != null && !parameterType.trim().isEmpty();
    }
}