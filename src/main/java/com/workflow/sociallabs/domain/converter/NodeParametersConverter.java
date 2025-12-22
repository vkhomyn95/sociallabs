package com.workflow.sociallabs.domain.converter;

import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Converter для NodeParameters -> JSON
 */
@Slf4j
@Converter
@RequiredArgsConstructor
public class NodeParametersConverter implements AttributeConverter<NodeParameters, String> {

    private static final String field = "@type";
    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(NodeParameters parameters) {
        if (parameters == null) {
            return null;
        }

        try {
            // Якщо є тип - зберігаємо з типом
            if (parameters.hasType()) {
                // Додаємо @type до values
                parameters.getValues().put(field, parameters.getParameterType());
            }

            if (parameters.getValues().isEmpty()) {
                return null;
            }

            return objectMapper.writeValueAsString(parameters.getValues());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert parameters to JSON", e);
        }
    }

    @Override
    public NodeParameters convertToEntityAttribute(String json) {
        if (json == null || json.trim().isEmpty()) {
            return NodeParameters.builder().build();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(json);

            // Перевіряємо чи є @type
            JsonNode typeNode = jsonNode.get(field);

            if (typeNode != null) {
                String type = typeNode.asText();

                // Deserialize в конкретний клас через Jackson polymorphism
                // Зберігаємо також raw values для зворотної сумісності
                return NodeParameters.builder()
                        .parameterType(type)
                        .values(objectMapper.convertValue(jsonNode,
                                objectMapper.getTypeFactory().constructMapType(
                                        java.util.HashMap.class,
                                        String.class,
                                        Object.class)))
                        .build();
            } else {
                // Старий формат без типу - просто Map
                return NodeParameters.from(
                        objectMapper.convertValue(jsonNode,
                                objectMapper.getTypeFactory().constructMapType(
                                        java.util.HashMap.class,
                                        String.class,
                                        Object.class))
                );
            }
        } catch (Exception e) {
            log.error("Failed to convert JSON to NodeParameters: {}", json, e);
            return NodeParameters.builder().build();
        }
    }

    /**
     * Конвертувати NodeParameters в типізований клас
     */
    public <T extends TypedNodeParameters> T toTypedParameters(
            NodeParameters parameters,
            Class<T> targetClass
    ) {
        if (parameters == null || parameters.getValues().isEmpty()) {
            return null;
        }

        try {
            // Додаємо @type якщо його немає
            if (!parameters.getValues().containsKey(field) && parameters.hasType()) {
                parameters.getValues().put(field, parameters.getParameterType());
            }

            return objectMapper.convertValue(parameters.getValues(), targetClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert parameters", e);
        }
    }

    /**
     * Автоматично визначити тип та конвертувати
     */
    public TypedNodeParameters toTypedParameters(NodeParameters parameters) {
        if (parameters == null || !parameters.hasType()) {
            return null;
        }

        try {
            return objectMapper.convertValue(
                    parameters.getValues(),
                    TypedNodeParameters.class
            );
        } catch (Exception e) {
            return null;
        }
    }
}