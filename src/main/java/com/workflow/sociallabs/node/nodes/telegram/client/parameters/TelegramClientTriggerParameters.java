package com.workflow.sociallabs.node.nodes.telegram.client.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.domain.enums.NodeTriggerType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.parameters.TriggerNodeParameters;
import lombok.*;

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
     * Якщо null або empty - слухаємо всі події
     */
    private Set<String> events;

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