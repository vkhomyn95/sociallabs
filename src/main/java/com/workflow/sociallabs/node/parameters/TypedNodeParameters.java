package com.workflow.sociallabs.node.parameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.logic.parameters.IfNodeParameters;
import com.workflow.sociallabs.node.nodes.logic.parameters.SwitchNodeParameters;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotTriggerParameters;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientTriggerParameters;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TelegramBotActionParameters.class, name = NodeDiscriminator.Values.TELEGRAM_BOT_ACTION),
        @JsonSubTypes.Type(value = TelegramClientActionParameters.class, name = NodeDiscriminator.Values.TELEGRAM_CLIENT_ACTION),

        @JsonSubTypes.Type(value = TelegramBotTriggerParameters.class, name = NodeDiscriminator.Values.TELEGRAM_BOT_TRIGGER),
        @JsonSubTypes.Type(value = TelegramClientTriggerParameters.class, name = NodeDiscriminator.Values.TELEGRAM_CLIENT_TRIGGER),

        @JsonSubTypes.Type(value = IfNodeParameters.class, name = NodeDiscriminator.Values.IF_LOGIC),
        @JsonSubTypes.Type(value = SwitchNodeParameters.class, name = NodeDiscriminator.Values.SWITCH_LOGIC)
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