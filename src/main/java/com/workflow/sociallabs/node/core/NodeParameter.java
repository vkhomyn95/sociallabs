package com.workflow.sociallabs.node.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import java.util.*;
import java.time.Instant;

// ============== NODE PARAMETER (Generic) ==============
/**
 * Базовий generic клас для параметрів ноди
 * @param <T> тип значення параметра
 */
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class NodeParameter<T> {

    private String name;
    private String displayName;
    private String description;
    private T defaultValue;
    private boolean required;
    private boolean hidden;
    private String displayCondition; // Expression for conditional display

    /**
     * Валідація значення параметра
     */
    public abstract boolean validate(T value);

    /**
     * Конвертація значення з JSON
     */
    public abstract T parseValue(Object rawValue);

    /**
     * Отримання типу параметра
     */
    public abstract String getType();
}
