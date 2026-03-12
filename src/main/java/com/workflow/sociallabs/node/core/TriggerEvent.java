package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.execution.context.DataMapper;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Event який публікується коли тригер отримує нову подію
 * Використовується для decoupling між TriggerExecutor та WorkflowExecutionService
 */
@Getter
public class TriggerEvent extends ApplicationEvent {

    private final Long workflowId;
    private final String triggerNodeId;
    private final Map<String, Object> event;

    public TriggerEvent(
            Object source,
            Long workflowId,
            String triggerNodeId,
            Map<String, Object> event) {

        super(source);
        this.workflowId = workflowId;
        this.triggerNodeId = triggerNodeId;
        this.event = event;
    }

    public String getName() {
        return event != null ? (String) event.get(DataMapper.NAME): null;
    }
}
