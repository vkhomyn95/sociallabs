package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractActionNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.nodes.telegram.client.handlers.TelegramClientMessageResourceHandler;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import it.tdlight.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.Object;
import java.util.Map;

/**
 * Executor для Telegram Client Action
 * Використовує TDLight для роботи через Telegram Client API
 */
@Slf4j
@Component
public class TelegramClientActionNodeExecutor extends AbstractActionNode {

    private static final String TELEGRAM_CLIENT_SESSION_ID_PARAM = "sessionId";

    @Autowired
    private TelegramClientService sessionManager;

    public TelegramClientActionNodeExecutor() {
        super(NodeDiscriminator.TELEGRAM_CLIENT_ACTION);
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    @Override
    protected Map<String, Object> processItem(Map<String, Object> item, ExecutionContext context) throws Exception {
        TelegramClientActionParameters params = context.getParameters(TelegramClientActionParameters.class);

        // Валідація
        params.validate();

        // Отримати sessionId
        String sessionId = context.getCredential(TELEGRAM_CLIENT_SESSION_ID_PARAM, String.class);
        // Отримати клієнт з сесії
        SimpleTelegramClient client = sessionManager.getSession(sessionId);

        if (client == null) {
            throw new IllegalStateException("Failed to get Telegram client from credentials");
        }

        // Обробка різних resource + operation
        return switch (params.getResource()) {
            case MESSAGE -> TelegramClientMessageResourceHandler.execute(client, params, item);
//            case PHOTO -> TelegramClientPhotoResourceHandler.execute(client, params, item);
//            case VIDEO -> TelegramClientVideoResourceHandler.execute(client, params, item);
//            case DOCUMENT -> TelegramClientDocumentResourceHandler.execute(client, params, item);
//            case AUDIO -> TelegramClientAudioResourceHandler.execute(client, params, item);
//            case VOICE -> TelegramClientVoiceResourceHandler.execute(client, params, item);
//            case VIDEO_NOTE -> TelegramClientVideoNoteResourceHandler.execute(client, params, item);
//            case LOCATION -> TelegramClientLocationResourceHandler.execute(client, params, item);
//            case CONTACT -> TelegramClientContactResourceHandler.execute(client, params, item);
//            case VENUE -> TelegramClientVenueResourceHandler.execute(client, params, item);
//            case POLL -> TelegramClientPollResourceHandler.execute(client, params, item);
//            case STICKER -> TelegramClientStickerResourceHandler.execute(client, params, item);
//            case ANIMATION -> TelegramClientAnimationResourceHandler.execute(client, params, item);
//            case DICE -> TelegramClientDiceResourceHandler.execute(client, params, item);
//            case CHAT -> TelegramClientChatResourceHandler.execute(client, params, item);
//            case CHAT_MEMBER -> TelegramClientChatMemberResourceHandler.execute(client, params, item);
            default -> throw new IllegalArgumentException("Unsupported resource: " + params.getResource());
        };
    }

    @Override
    protected boolean shouldFailOnError(ExecutionContext context) {
        TelegramClientActionParameters params = context.getParameters(TelegramClientActionParameters.class);
        return !params.getContinueOnFail();
    }
}