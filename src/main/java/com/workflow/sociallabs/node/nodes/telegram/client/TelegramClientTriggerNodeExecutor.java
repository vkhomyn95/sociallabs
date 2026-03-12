package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractTriggerNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.TriggerEventPublisher;
import com.workflow.sociallabs.node.nodes.telegram.client.listeners.TelegramClientUpdateListener;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientTriggerParameters;
import it.tdlight.client.SimpleTelegramClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executor для Telegram Client Trigger
 * Слухає події від Telegram та запускає workflow
 */
@Slf4j
@Component
public class TelegramClientTriggerNodeExecutor extends AbstractTriggerNode {

    private static final String SESSION_ID_PARAM = "sessionId";

    @Autowired
    private TelegramClientService telegramService;

    @Autowired
    private TriggerEventPublisher eventPublisher;

    // Активні listeners: sessionId -> UpdateHandler. Один клієнт - один telegram client handler
    private final Map<String, TelegramClientUpdateListener> activeHandlers = new ConcurrentHashMap<>();

    public TelegramClientTriggerNodeExecutor() {
        super(NodeDiscriminator.TELEGRAM_CLIENT_TRIGGER);
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    /**
     * Активувати тригер - почати слухати Telegram updates
     */
    @Override
    public boolean activate(ExecutionContext context) {
        Long workflowId = context.getWorkflowId();
        String nodeId = context.getNodeId();

        log.info("Activating Telegram trigger for node: {}, workflowId: {}", nodeId, workflowId);

        TelegramClientTriggerParameters params = context.getParameters(TelegramClientTriggerParameters.class);
        params.validate();

        String sessionId = context.getCredential(SESSION_ID_PARAM, String.class);

        SimpleTelegramClient client = telegramService.getSession(sessionId);
        if (client == null) throw new IllegalStateException("Telegram client not found for session: " + sessionId);

        if (activeHandlers.containsKey(sessionId)) {
            throw new IllegalStateException("Handlers is already activated for session: " + sessionId);
        }

        // Створюємо та реєструємо update handler
        TelegramClientUpdateListener handler = new TelegramClientUpdateListener(
                workflowId,
                nodeId,
                params,
                event -> dispatchTriggerEvent(workflowId, nodeId, event)
        );
        client.addUpdatesHandler(handler);
        activeHandlers.put(sessionId, handler);

        log.info("Telegram trigger activated for node: {} (events: {})", nodeId, params.getEvents());

        return true;
    }

    /**
     * Dispatch trigger event через Spring Events
     * Викликається коли отримано подію від Telegram
     */
    private void dispatchTriggerEvent(Long workflowId, String nodeId, Map<String, Object> event) {
        if (workflowId == null) {
            log.warn("No workflow mapping found for node: {}", nodeId);
            return;
        }

        log.debug("Dispatching trigger event for node {} in workflow {}: {}", nodeId, workflowId, event.get("type"));

        // Публікуємо event через Spring Event Bus
        eventPublisher.publishTriggerEvent(workflowId, nodeId, event);
    }

    /**
     * Деактивувати тригер - припинити слухати updates
     */
    @Override
    public void deactivate(ExecutionContext context) {
        Long workflowId = context.getWorkflowId();
        String nodeId = context.getNodeId();
        log.info("Deactivating Telegram trigger for node: {} in workflow: {}", nodeId, workflowId);

        String sessionId = context.getCredential(SESSION_ID_PARAM, String.class);

        TelegramClientUpdateListener handler = activeHandlers.get(sessionId);
        Optional.ofNullable(handler).ifPresent(h -> h.setActive(false));
    }
}