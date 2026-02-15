package com.workflow.sociallabs.node.nodes.telegram.client.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.domain.enums.NodeTriggerType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientChatType;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource;
import com.workflow.sociallabs.node.parameters.TriggerNodeParameters;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.TELEGRAM_CLIENT_TRIGGER)
public class TelegramClientTriggerParameters implements TriggerNodeParameters {

    private boolean active;

    /**
     * Події які потрібно слухати
     * Можливі значення: newMessage, messageEdited, messagesDeleted, chatLastMessage, тощо
     * Якщо null або empty - слухаємо всі події
     */
    private Set<String> events;

    /**
     * Фільтр по чатах (chatId)
     * Якщо вказано - обробляємо тільки події з цих чатів
     */
    private List<Long> chatIds;

    /**
     * Фільтр по типу чату
     */
    private TelegramClientChatType chatType;

    /**
     * Фільтр по типу повідомлення
     */
    private Set<TelegramClientResource> messageResources;

    /**
     * Фільтр по відправнику
     * Якщо true - тільки вхідні повідомлення
     * Якщо false - тільки вихідні
     * Якщо null - всі
     */
    private Boolean incomingOnly;

    /**
     * Чи обробляти редаговані повідомлення
     */
    @Builder.Default
    private Boolean includeEdited = false;

    /**
     * Чи обробляти видалені повідомлення
     */
    @Builder.Default
    private Boolean includeDeleted = false;

    /**
     * Валідація параметрів
     */
    public void validate() {}

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.TELEGRAM_CLIENT_TRIGGER;
    }

    @Override
    public NodeTriggerType getTriggerType() {
        return NodeTriggerType.EVENT;
    }

    @Override
    public Boolean isActive() {
        return active;
    }
}