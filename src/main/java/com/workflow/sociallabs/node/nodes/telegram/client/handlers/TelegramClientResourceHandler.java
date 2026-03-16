package com.workflow.sociallabs.node.nodes.telegram.client.handlers;

import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import it.tdlight.client.SimpleTelegramClient;

import java.util.Map;

/**
 * Кожен resource (MESSAGE, PHOTO, VIDEO...) реалізує цей інтерфейс.
 * Додати новий resource = створити новий клас + зареєструвати в HANDLERS map.
 */
public interface TelegramClientResourceHandler {
    Map<String, Object> execute(
            SimpleTelegramClient client,
            TelegramClientActionParameters params,
            Map<String, Object> item
    ) throws Exception;
}