package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.domain.enums.NodeTriggerType;

/**
 * Базовий інтерфейс для trigger параметрів
 */
public interface TriggerNodeParameters extends TypedNodeParameters {

    /**
     * Тип тригера (webhook, polling, schedule, event)
     */
    NodeTriggerType getTriggerType();

    /**
     * Чи активний тригер
     */
    Boolean isActive();
}
