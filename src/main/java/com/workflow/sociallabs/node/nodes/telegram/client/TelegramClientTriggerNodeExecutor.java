package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractTriggerNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.nodes.telegram.client.listeners.TelegramClientUpdateListener;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientTriggerParameters;
import it.tdlight.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.Object;
import java.util.*;
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
    public boolean activate(ExecutionContext context) throws Exception {
        String nodeId = context.getNodeId();
        log.info("Activating Telegram trigger for node: {}", nodeId);

        TelegramClientTriggerParameters params = context.getTypedParameters(TelegramClientTriggerParameters.class);
        params.validate();

        String sessionId = context.getCredential(SESSION_ID_PARAM, String.class);

        SimpleTelegramClient client = telegramService.getSession(sessionId);

        if (client == null) throw new IllegalStateException("Telegram client not found for session: " + sessionId);

        if (activeHandlers.containsKey(sessionId)) {
            throw new IllegalStateException("Handlers is already activated for session: " + sessionId);
        }

        // Створюємо та реєструємо update handler
//        TelegramClientUpdateListener handler = new TelegramClientUpdateListener(nodeId, params, this::handleTelegramUpdate);

//        client.addUpdatesHandler(handler);
//        activeHandlers.put(sessionId, handler);

        log.info("Telegram trigger activated for node: {} (events: {})", nodeId, params.getEvents());

        return true;
    }

    /**
     * Деактивувати тригер - припинити слухати updates
     */
    @Override
    public void deactivate(ExecutionContext context) throws Exception {
        String nodeId = context.getNodeId();
        log.info("Deactivating Telegram trigger for node: {}", nodeId);

//        TelegramUpdateHandler handler = activeHandlers.remove(nodeId);
//
//        if (handler != null) {
//             TDLight не має методу removeUpdatesHandler, тому просто маркуємо як inactive
//            handler.setActive(false);
//            log.info("Telegram trigger deactivated for node: {}", nodeId);
//        }
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        // Цей метод викликається коли тригер спрацьовує
        Map<String, Object> messageData = context.getFirstInputItem();

        if (messageData == null || messageData.isEmpty()) {
            return NodeResult.error("No message data received", null);
        }

       return null;
    }
}