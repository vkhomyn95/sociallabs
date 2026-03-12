package com.workflow.sociallabs.node.nodes.telegram.client.listeners;

import com.workflow.sociallabs.execution.context.DataMapper;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientTriggerParameters;
import it.tdlight.client.GenericUpdateHandler;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class TelegramClientUpdateListener implements GenericUpdateHandler<TdApi.Update> {

    private final Long workflowId;
    private final String nodeId;
    private final TelegramClientTriggerParameters parameters;
    private final Consumer<Map<String, Object>> callback;

    private volatile boolean active = true;

    public TelegramClientUpdateListener(
            Long workflowId,
            String nodeId,
            TelegramClientTriggerParameters parameters,
            Consumer<Map<String, Object>> callback
    ) {
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.parameters = parameters;
        this.callback = callback;
    }

    @Override
    public void onUpdate(TdApi.Update update) {
        if (!active || parameters == null || !parameters.isActive()) return;

        try {
            Map<String, Object> event = DataMapper.convert(update);

            if (event == null || unavailable(event)) return;

            callback.accept(event);
        } catch (Exception e) {
            log.error("Error processing telegram client trigger nodeId: {} workflowId: {} Telegram update: {}", nodeId, workflowId, e.getMessage(), e);
        }
    }

    private boolean unavailable(Map<String, Object> event) {
        Set<String> events = parameters.getEvents();
        if (events == null || events.isEmpty()) return false;

        String name = (String) event.get(DataMapper.NAME);
        return !events.contains(name);
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
