package com.workflow.sociallabs.dto.request;

import com.workflow.sociallabs.model.NodeDiscriminator;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Request для створення/оновлення node instance
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeRequest {

    /**
     * UUID node (опціонально для нових нод, буде згенеровано автоматично)
     */
    private String nodeId;

    /**
     * Дискримінатор node - визначає конкретний тип (обов'язково)
     * Наприклад: TELEGRAM_BOT_ACTION, TELEGRAM_BOT_TRIGGER, HTTP_REQUEST тощо
     */
    @NotNull(message = "Node discriminator is required")
    private NodeDiscriminator discriminator;

    /**
     * Відображуване ім'я node (опціонально, буде взято з discriminator)
     */
    private String name;

    /**
     * Позиція на canvas
     */
    @Builder.Default
    private NodePositionRequest position = new NodePositionRequest();

    /**
     * Параметри ноди у вигляді Map
     * Структура залежить від типу ноди (discriminator)
     */
    private Map<String, Object> parameters;

    /**
     * ID credential для автентифікації (якщо потрібно)
     */
    private Long credentialId;

    /**
     * Чи вимкнена node
     */
    @Builder.Default
    private Boolean disabled = false;

    /**
     * Нотатки до node
     */
    private String notes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodePositionRequest {

        @Builder.Default
        private Integer x = 0;

        @Builder.Default
        private Integer y = 0;
    }
}