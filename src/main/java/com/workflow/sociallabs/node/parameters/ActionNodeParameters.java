package com.workflow.sociallabs.node.parameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;


/**
 * Базовий інтерфейс для всіх action node параметрів
 * Використовує Jackson polymorphic serialization
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TelegramBotActionParameters.class, name = NodeDiscriminator.Values.TELEGRAM_BOT_ACTION),
        @JsonSubTypes.Type(value = TelegramClientActionParameters.class, name = NodeDiscriminator.Values.TELEGRAM_CLIENT_ACTION),
})
public interface ActionNodeParameters extends TypedNodeParameters {


}
