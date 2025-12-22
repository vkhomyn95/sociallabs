package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractTriggerNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.Object;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TelegramClientTriggerNodeExecutor extends AbstractTriggerNode {

    private final TelegramClientManager clientManager;

    // Активні тригери: nodeId -> TriggerState
    private final Map<String, TriggerState> activeTriggers = new ConcurrentHashMap<>();

    public TelegramClientTriggerNodeExecutor(TelegramClientManager clientManager) {
        super(NodeDiscriminator.TELEGRAM_CLIENT_TRIGGER);
        this.clientManager = clientManager;
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    @Override
    public boolean activate(ExecutionContext context) throws Exception {
        String nodeId = context.getNodeId();
        log.info("Activating TDLight trigger for node: {}", nodeId);

        // Отримати credential ID
        Long credentialId = context.getCredential("credentialId", Long.class);
        if (credentialId == null) {
            throw new IllegalStateException("Credential ID not found");
        }

        // Отримати або створити клієнта з пула
        SimpleTelegramClient client = clientManager.getOrCreateClient(credentialId);

        // Створити state для тригера
        TriggerState state = new TriggerState();
        state.nodeId = nodeId;
        state.credentialId = credentialId;
        state.client = client;
        state.context = context;

        activeTriggers.put(nodeId, state);

        // Налаштувати обробник повідомлень
        setupMessageHandler(state);

        log.info("TDLight trigger activated for node: {}", nodeId);
        return true;
    }

    @Override
    public void deactivate(ExecutionContext context) throws Exception {
        String nodeId = context.getNodeId();
        log.info("Deactivating TDLight trigger for node: {}", nodeId);

        TriggerState state = activeTriggers.remove(nodeId);
        if (state != null) {
            state.active = false;
            // НЕ закриваємо клієнта - він може використовуватись іншими тригерами
            log.info("TDLight trigger deactivated for node: {}", nodeId);
        }
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        // Цей метод викликається коли тригер спрацьовує
        Map<String, Object> messageData = context.getFirstInputItem();

        if (messageData == null || messageData.isEmpty()) {
            return NodeResult.error("No message data received", null);
        }

        if (!matchesFilters(messageData, context)) {
            log.debug("Message doesn't match filters, skipping");
            return NodeResult.success(Collections.emptyList());
        }

        Map<String, Object> enrichedData = enrichMessageData(messageData, context);
        return NodeResult.success(enrichedData);
    }

    /**
     * Налаштувати обробник повідомлень
     */
    private void setupMessageHandler(TriggerState state) {
        String triggerOn = state.context.getParameter("triggerOn", String.class, "newMessage");
        String chatIdentifier = state.context.getParameter("chatIdentifier", String.class);

        // Додати обробник updates
        GenericUpdateHandler<Update> updateHandler = (update) -> {
            if (!state.active) return;

            try {
                handleUpdate(state, update, triggerOn, chatIdentifier);
            } catch (Exception e) {
                log.error("Error processing update", e);
            }
        };

        state.client.addUpdatesHandler(updateHandler);
        state.updateHandler = updateHandler;
    }

    /**
     * Обробити update від Telegram
     */
    private void handleUpdate(
            TriggerState state,
            Object update,
            String triggerOn,
            String chatIdentifier
    ) {
        if ("newMessage".equals(triggerOn) && update instanceof UpdateNewMessage) {
            UpdateNewMessage msgUpdate = (UpdateNewMessage) update;
            Message message = msgUpdate.message;

            // Фільтр по чату
            if (chatIdentifier != null && !chatIdentifier.isEmpty()) {
                if (!matchesChatIdentifier(message.chatId, chatIdentifier, state.client)) {
                    return;
                }
            }

            Map<String, Object> messageData = convertMessageToMap(message, state.client);
            triggerWorkflowExecution(state.nodeId, messageData);
        }
        else if ("messageEdited".equals(triggerOn) && update instanceof UpdateMessageEdited) {
            UpdateMessageEdited editUpdate = (UpdateMessageEdited) update;

            state.client.send(new GetMessage(editUpdate.chatId, editUpdate.messageId))
                    .whenComplete((message, error) -> {
                        if (error == null && state.active) {
                            Map<String, Object> messageData = convertMessageToMap(message, state.client);
                            messageData.put("edited", true);
                            triggerWorkflowExecution(state.nodeId, messageData);
                        }
                    });
        }
        else if ("messageDeleted".equals(triggerOn) && update instanceof UpdateDeleteMessages) {
            UpdateDeleteMessages delUpdate = (UpdateDeleteMessages) update;

            Map<String, Object> data = new HashMap<>();
            data.put("chat_id", delUpdate.chatId);
            data.put("message_ids", delUpdate.messageIds);
            data.put("is_permanent", delUpdate.isPermanent);
            data.put("from_cache", delUpdate.fromCache);

            triggerWorkflowExecution(state.nodeId, data);
        }
    }

    /**
     * Конвертувати повідомлення в Map
     */
    private Map<String, Object> convertMessageToMap(Message message, SimpleTelegramClient client) {
        Map<String, Object> data = new HashMap<>();

        data.put("message_id", message.id);
        data.put("chat_id", message.chatId);
        data.put("sender_id", extractSenderId(message.senderId));
        data.put("date", message.date);
        data.put("is_outgoing", message.isOutgoing);
        data.put("is_channel_post", message.isChannelPost);

        // Reply info
        if (message.replyTo instanceof MessageReplyToMessage) {
            MessageReplyToMessage replyTo = (MessageReplyToMessage) message.replyTo;
            data.put("reply_to_message_id", replyTo.messageId);
        }

        // Forward info
        if (message.forwardInfo != null) {
            data.put("forward_info", convertForwardInfo(message.forwardInfo));
        }

        // Content
        Map<String, Object> contentData = convertMessageContent(message.content);
        data.putAll(contentData);

        // Асинхронно отримуємо інфо про чат
        try {
            Chat chat = client.send(new GetChat(message.chatId)).get(5, TimeUnit.SECONDS);
            data.put("chat_title", chat.title);
            data.put("chat_type", chat.type.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("Failed to get chat info: {}", e.getMessage());
        }

        // Асинхронно отримуємо інфо про відправника
        try {
            long senderId = extractSenderId(message.senderId);
            if (senderId > 0) {
                User user = client.send(new GetUser(senderId)).get(5, TimeUnit.SECONDS);
                data.put("sender_name", (user.firstName + " " + user.lastName).trim());

                if (user.usernames != null && user.usernames.activeUsernames.length > 0) {
                    data.put("sender_username", user.usernames.activeUsernames[0]);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get sender info: {}", e.getMessage());
        }

        return data;
    }

    /**
     * Конвертувати content повідомлення
     */
    private Map<String, Object> convertMessageContent(MessageContent content) {
        Map<String, Object> data = new HashMap<>();

        if (content instanceof MessageText) {
            MessageText textContent = (MessageText) content;
            data.put("text", textContent.text.text);
            data.put("media_type", "text");

            if (textContent.text.entities.length > 0) {
                data.put("entities", convertTextEntities(textContent.text.entities));
            }
        }
        else if (content instanceof MessagePhoto) {
            MessagePhoto photoContent = (MessagePhoto) content;
            data.put("media_type", "photo");
            data.put("caption", photoContent.caption.text);
            data.put("photo", convertPhoto(photoContent.photo));
        }
        else if (content instanceof MessageVideo) {
            MessageVideo videoContent = (MessageVideo) content;
            data.put("media_type", "video");
            data.put("caption", videoContent.caption.text);
            data.put("video", convertVideo(videoContent.video));
        }
        else if (content instanceof MessageDocument) {
            MessageDocument docContent = (MessageDocument) content;
            data.put("media_type", "document");
            data.put("caption", docContent.caption.text);
            data.put("document", convertDocument(docContent.document));
        }
        else if (content instanceof MessageVoiceNote) {
            data.put("media_type", "voice");
        }
        else if (content instanceof MessageSticker) {
            data.put("media_type", "sticker");
        }
        else {
            data.put("media_type", "other");
            data.put("content_type", content.getClass().getSimpleName());
        }

        return data;
    }

    private long extractSenderId(MessageSender sender) {
        if (sender instanceof MessageSenderUser) {
            return ((MessageSenderUser) sender).userId;
        } else if (sender instanceof MessageSenderChat) {
            return ((MessageSenderChat) sender).chatId;
        }
        return 0;
    }

    private boolean matchesChatIdentifier(long chatId, String identifier, SimpleTelegramClient client) {
        try {
            long targetChatId = Long.parseLong(identifier);
            return chatId == targetChatId;
        } catch (NumberFormatException e) {
            if (identifier.startsWith("@")) {
                identifier = identifier.substring(1);
            }

            try {
                Chat chat = client.send(new SearchPublicChat(identifier)).get(5, TimeUnit.SECONDS);
                return chat.id == chatId;
            } catch (Exception ex) {
                log.warn("Failed to resolve chat username: {}", identifier);
                return false;
            }
        }
    }

    private boolean matchesFilters(Map<String, Object> messageData, ExecutionContext context) {
        String messageFilter = context.getParameter("messageFilter", String.class, "all");
        if (!"all".equals(messageFilter)) {
            String mediaType = (String) messageData.get("media_type");
            if (!messageFilter.equals(mediaType)) {
                return false;
            }
        }

        String contentFilter = context.getParameter("contentFilter", String.class);
        if (contentFilter != null && !contentFilter.isEmpty()) {
            String text = (String) messageData.get("text");
            if (text == null || !text.matches(".*" + contentFilter + ".*")) {
                return false;
            }
        }

        return true;
    }

    private Map<String, Object> enrichMessageData(Map<String, Object> messageData, ExecutionContext context) {
        Map<String, Object> enriched = new HashMap<>(messageData);
        enriched.put("trigger_time", java.time.Instant.now().toString());
        enriched.put("node_id", context.getNodeId());
        enriched.put("workflow_id", context.getExecutionId());
        return enriched;
    }

    private void triggerWorkflowExecution(String nodeId, Map<String, Object> data) {
        log.info("Triggering workflow execution for node {} with data keys: {}", nodeId, data.keySet());
        // TODO: реалізувати виклик WorkflowEngine для запуску workflow
        // workflowEngine.trigger(nodeId, data);
    }

    // Helper methods для конвертації
    private Map<String, Object> convertForwardInfo(MessageForwardInfo forwardInfo) {
        Map<String, Object> info = new HashMap<>();
        info.put("date", forwardInfo.date);
        if (forwardInfo.origin instanceof MessageOriginUser) {
            info.put("from_user_id", ((MessageOriginUser) forwardInfo.origin).senderUserId);
        } else if (forwardInfo.origin instanceof MessageOriginChannel) {
            MessageOriginChannel channel = (MessageOriginChannel) forwardInfo.origin;
            info.put("from_chat_id", channel.chatId);
            info.put("from_message_id", channel.messageId);
        }
        return info;
    }

    private List<Map<String, Object>> convertTextEntities(TextEntity[] entities) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TextEntity entity : entities) {
            Map<String, Object> e = new HashMap<>();
            e.put("offset", entity.offset);
            e.put("length", entity.length);
            e.put("type", entity.type.getClass().getSimpleName());
            result.add(e);
        }
        return result;
    }

    private Map<String, Object> convertPhoto(Photo photo) {
        Map<String, Object> data = new HashMap<>();
        if (photo.sizes.length > 0) {
            PhotoSize largest = photo.sizes[photo.sizes.length - 1];
            data.put("file_id", largest.photo.id);
            data.put("width", largest.width);
            data.put("height", largest.height);
        }
        return data;
    }

    private Map<String, Object> convertVideo(Video video) {
        Map<String, Object> data = new HashMap<>();
        data.put("file_id", video.video.id);
        data.put("duration", video.duration);
        data.put("width", video.width);
        data.put("height", video.height);
        return data;
    }

    private Map<String, Object> convertDocument(Document document) {
        Map<String, Object> data = new HashMap<>();
        data.put("file_id", document.document.id);
        data.put("file_name", document.fileName);
        data.put("mime_type", document.mimeType);
        return data;
    }

    /**
     * State для активного тригера
     */
    private static class TriggerState {
        String nodeId;
        Long credentialId;
        SimpleTelegramClient client;
        ExecutionContext context;
        GenericUpdateHandler<Update> updateHandler;
        volatile boolean active = true;
    }
}