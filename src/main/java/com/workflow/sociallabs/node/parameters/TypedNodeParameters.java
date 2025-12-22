package com.workflow.sociallabs.node.parameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotTriggerParameters;


/**
 * Базовий інтерфейс для всіх типізованих параметрів
 * Використовує Jackson polymorphic serialization
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TelegramBotActionParameters.class, name = NodeDiscriminator.Values.TELEGRAM_BOT_ACTION),
        @JsonSubTypes.Type(value = TelegramBotTriggerParameters.class, name = NodeDiscriminator.Values.TELEGRAM_BOT_TRIGGER),


})
public interface TypedNodeParameters {

    /**
     * Валідація параметрів
     */
    void validate() throws IllegalArgumentException;

    /**
     * Отримати тип параметрів (для дискримінатора)
     */
    NodeDiscriminator getParameterType();
}