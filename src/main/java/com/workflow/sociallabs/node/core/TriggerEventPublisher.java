package com.workflow.sociallabs.node.core;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

/**
 * Publisher для trigger events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Опублікувати trigger event
     */
    public void publishTriggerEvent(
            Long workflowId,
            String triggerNodeId,
            Map<String, Object> event) {

        log.debug("Publishing trigger event for workflow {} node {}", workflowId, triggerNodeId);

        TriggerEvent triggerEvent = new TriggerEvent(
                this,
                workflowId,
                triggerNodeId,
                event
        );

        eventPublisher.publishEvent(triggerEvent);
    }
}
