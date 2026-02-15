package com.workflow.sociallabs.node.parameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.workflow.sociallabs.domain.enums.NodeTriggerType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotTriggerParameters;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientTriggerParameters;

/**
 * Базовий інтерфейс для trigger параметрів
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TelegramBotTriggerParameters.class, name = NodeDiscriminator.Values.TELEGRAM_BOT_TRIGGER),
        @JsonSubTypes.Type(value = TelegramClientTriggerParameters.class, name = NodeDiscriminator.Values.TELEGRAM_CLIENT_TRIGGER),
})
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
