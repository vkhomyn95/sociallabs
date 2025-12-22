package com.workflow.sociallabs.node.nodes.telegram.bot;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractActionNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.nodes.telegram.bot.handler.*;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Executor для Telegram Bot Action
 * Виконує фактичні запити до Telegram API
 */
@Slf4j
@Component
public class TelegramBotActionNodeExecutor extends AbstractActionNode {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/";
    private static final String TELEGRAM_BOT_TOKEN_PARAM = "api_key";

    private final WebClient client;

    public TelegramBotActionNodeExecutor() {
        super(NodeDiscriminator.TELEGRAM_BOT_ACTION);

        this.client = WebClient.builder().baseUrl(TELEGRAM_API_URL).build();
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    @Override
    protected Map<String, Object> processItem(Map<String, Object> item, ExecutionContext context) throws Exception {
        TelegramBotActionParameters params = context.getTypedParameters(TelegramBotActionParameters.class);

        // Валідація
        params.validate();

        // Отримати bot token
        String botToken = context.getCredential(TELEGRAM_BOT_TOKEN_PARAM, String.class);

        if (botToken == null) throw new IllegalStateException("Telegram bot token not found in credentials");

        // Обробка різних resource + operation
        return switch (params.getResource()) {
            case MESSAGE -> TelegramBotMessageResourceHandler.execute(client, botToken, params, item);
            case PHOTO -> TelegramBotPhotoResourceHandler.execute(client, botToken, params, item);
            case VIDEO -> TelegramBotVideoResourceHandler.execute(client, botToken, params, item);
            case DOCUMENT -> TelegramBotDocumentResourceHandler.execute(client, botToken, params, item);
            case AUDIO -> TelegramBotAudioResourceHandler.execute(client, botToken, params, item);
            case VOICE -> TelegramBotVoiceResourceHandler.execute(client, botToken, params, item);
            case LOCATION -> TelegramBotLocationResourceHandler.execute(client, botToken, params, item);
            case CONTACT -> TelegramBotContactResourceHandler.execute(client, botToken, params, item);
            case VENUE -> TelegramBotVenueResourceHandler.execute(client, botToken, params, item);
            case POLL -> TelegramBotPollResourceHandler.execute(client, botToken, params, item);
            case STICKER -> TelegramBotStickerResourceHandler.execute(client, botToken, params, item);
            case ANIMATION -> TelegramBotAnimationResourceHandler.execute(client, botToken, params, item);

            default -> throw new IllegalArgumentException("Unsupported resource: " + params.getResource());
        };
    }

    /**
     * Чи повинна node зупинитися при помилці
     */
    @Override
    protected boolean shouldFailOnError(ExecutionContext context) {
        TelegramBotActionParameters params = context.getTypedParameters(TelegramBotActionParameters.class);

        return params.getContinueOnFail();
    }
}
