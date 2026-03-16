package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractActionNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource;
import com.workflow.sociallabs.node.nodes.telegram.client.handlers.TelegramClientMessageResourceHandler;
import com.workflow.sociallabs.node.nodes.telegram.client.handlers.TelegramClientResourceHandler;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import it.tdlight.client.SimpleTelegramClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executor для Telegram Client Action
 * Використовує TDLight для роботи через Telegram Client API
 */
@Slf4j
@Component
public class TelegramClientActionNodeExecutor extends AbstractActionNode {

    private static final String SESSION_ID_PARAM = "sessionId";

    @Autowired
    private TelegramClientService telegramService;

    public TelegramClientActionNodeExecutor() {
        super(NodeDiscriminator.TELEGRAM_CLIENT_ACTION);
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    // Registry handler-ів по resource типу
    private static final Map<TelegramClientResource, TelegramClientResourceHandler> HANDLERS =
            Map.of(
                    TelegramClientResource.MESSAGE, new TelegramClientMessageResourceHandler()
                    // TelegramClientResource.PHOTO, new TelegramClientPhotoResourceHandler(),
                    // TelegramClientResource.VIDEO,  new TelegramClientVideoResourceHandler(),
            );

    @Override
    protected Map<String, Object> processItem(Map<String, Object> item, ExecutionContext context) throws Exception {
        TelegramClientActionParameters params = context.getParameters(TelegramClientActionParameters.class);
        params.validate();

        String sessionId = context.getCredential(SESSION_ID_PARAM, String.class);
        SimpleTelegramClient client = telegramService.getSession(sessionId);

        if (client == null) {
            throw new IllegalStateException("Telegram client not found for session: " + sessionId);
        }

        TelegramClientResourceHandler handler = HANDLERS.get(params.getResource());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported resource: " + params.getResource());
        }

        return handler.execute(client, params, item);
    }

    @Override
    protected boolean shouldFailOnError(ExecutionContext context) {
        TelegramClientActionParameters params = context.getParameters(TelegramClientActionParameters.class);
        return !params.getContinueOnFail();
    }
}