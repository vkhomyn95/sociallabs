package com.workflow.sociallabs.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;


/**
 * DTO для повернення визначення ноди на фронтенд
 * Містить всю інформацію необхідну для відображення ноди в UI
 */
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeDefinitionResponse {

    private String type;              // e.g., "telegram.sendMessage"
    private String name;              // "Telegram"
    private String description;       // Опис ноди
    private String category;          // "Communication", "Data", etc.
    private String icon;              // CSS class або URL іконки
    private String color;             // Hex color
    private String nodeType;          // TRIGGER, ACTION, TRANSFORM

    private List<NodeParameterDto> parameters;
    private Map<String, NodeOutputDto> outputs;
    private List<String> credentialTypes;

    /**
     * Параметр ноди для UI
     */
    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeParameterDto {
        private String name;
        private String displayName;
        private String description;
        private String type;          // string, number, boolean, options, json, etc.
        private Object defaultValue;
        private Boolean required;
        private Boolean hidden;

        // Для options/multiOptions
        private List<OptionDto> options;

        // Для number
        private Number min;
        private Number max;
        private Number step;

        // Для string
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private Boolean multiline;
        private String placeholder;

        // Для collection
        private List<NodeParameterDto> fields;

        // Display conditions
        private Map<String, Object> displayOptions;
    }

    /**
     * Опція для dropdown параметрів
     */
    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String value;
        private String name;
        private String description;
    }

    /**
     * Вихід ноди
     */
    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeOutputDto {
        private String name;
        private String displayName;
        private String type;
        private String description;
        private Map<String, String> schema;
    }
}