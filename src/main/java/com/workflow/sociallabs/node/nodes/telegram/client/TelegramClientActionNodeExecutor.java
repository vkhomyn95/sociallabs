package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractActionNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.Object;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TelegramClientActionNodeExecutor extends AbstractActionNode {

    private final TelegramClientManager clientManager;

    public TelegramClientActionNodeExecutor(TelegramClientManager clientManager) {
        super(NodeDiscriminator.TELEGRAM_CLIENT_ACTION);
        this.clientManager = clientManager;
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    @Override
    protected Map<String, Object> processItem(
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        // Отримати credential ID
        Long credentialId = context.getCredential("credentialId", Long.class);
        if (credentialId == null) {
            throw new IllegalStateException("Credential ID not found");
        }

        // Отримати клієнта з пула
        SimpleTelegramClient client = clientManager.getOrCreateClient(credentialId);

        String operation = context.getParameter("operation", String.class, "sendMessage");

        return switch (operation) {
            case "sendMessage" -> sendMessage(client, item, context);
            case "sendPhoto" -> sendPhoto(client, item, context);
            case "sendVideo" -> sendVideo(client, item, context);
            case "sendDocument" -> sendDocument(client, item, context);
            case "forwardMessage" -> forwardMessage(client, item, context);
            case "editMessage" -> editMessage(client, item, context);
            case "deleteMessage" -> deleteMessage(client, item, context);
            case "sendReaction" -> sendReaction(client, item, context);
            case "readMessages" -> markAsRead(client, item, context);
            case "getHistory" -> getHistory(client, item, context);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    /**
     * Відправити текстове повідомлення
     */
    private Map<String, Object> sendMessage(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        String text = replacePlaceholders(
                context.getParameter("text", String.class),
                item
        );

        String parseMode = context.getParameter("parseMode", String.class, "none");
        Boolean disableNotification = context.getParameter("disableNotification", Boolean.class, false);
        String replyToMessageIdStr = context.getParameter("replyToMessageId", String.class);

        // Створити formatted text
        FormattedText formattedText = createFormattedText(text, parseMode);

        // Створити message content через сетери
        InputMessageText messageContent = new InputMessageText();
        messageContent.text = formattedText;
        messageContent.linkPreviewOptions = null;
        messageContent.clearDraft = false;

        // Створити send options через сетери
        MessageSendOptions sendOptions = new MessageSendOptions();
        sendOptions.disableNotification = disableNotification;
        sendOptions.fromBackground = false;
        sendOptions.protectContent = false;
        sendOptions.updateOrderOfInstalledStickerSets = false;
        sendOptions.schedulingState = null;
        sendOptions.effectId = 0;
        sendOptions.onlyPreview = false;

        // Reply to
        InputMessageReplyTo replyTo = null;
        if (replyToMessageIdStr != null && !replyToMessageIdStr.isEmpty()) {
            long replyMsgId = Long.parseLong(replacePlaceholders(replyToMessageIdStr, item));
            InputMessageReplyToMessage replyToMsg = new InputMessageReplyToMessage();
            replyToMsg.messageId = replyMsgId;
            replyToMsg.quote = null;
            replyTo = replyToMsg;
        }

        // Створити SendMessage request
        SendMessage sendMessage = new SendMessage();
        sendMessage.chatId = chatId;
        sendMessage.messageThreadId = 0;
        sendMessage.replyTo = replyTo;
        sendMessage.options = sendOptions;
        sendMessage.replyMarkup = null;
        sendMessage.inputMessageContent = messageContent;

        Message sentMessage = client.send(sendMessage).get(30, TimeUnit.SECONDS);

        return convertMessageToResult(sentMessage);
    }

    /**
     * Відправити фото
     */
    private Map<String, Object> sendPhoto(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        String filePath = replacePlaceholders(
                context.getParameter("filePath", String.class),
                item
        );

        String caption = replacePlaceholders(
                context.getParameter("caption", String.class, ""),
                item
        );

        InputFile inputFile = uploadFile(filePath);
        FormattedText formattedCaption = new FormattedText(caption, new TextEntity[0]);

        InputMessagePhoto photoContent = new InputMessagePhoto();
        photoContent.photo = inputFile;
        photoContent.thumbnail = null;
        photoContent.addedStickerFileIds = new int[0];
        photoContent.width = 0;
        photoContent.height = 0;
        photoContent.caption = formattedCaption;
        photoContent.showCaptionAboveMedia = false;
        photoContent.selfDestructType = null;
        photoContent.hasSpoiler = false;

        SendMessage sendMessage = new SendMessage();
        sendMessage.chatId = chatId;
        sendMessage.messageThreadId = 0;
        sendMessage.replyTo = null;
        sendMessage.options = createDefaultSendOptions();
        sendMessage.replyMarkup = null;
        sendMessage.inputMessageContent = photoContent;

        Message sentMessage = client.send(sendMessage).get(60, TimeUnit.SECONDS);

        return convertMessageToResult(sentMessage);
    }

    /**
     * Відправити відео
     */
    private Map<String, Object> sendVideo(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        String filePath = replacePlaceholders(
                context.getParameter("filePath", String.class),
                item
        );

        String caption = replacePlaceholders(
                context.getParameter("caption", String.class, ""),
                item
        );

        InputFile inputFile = uploadFile(filePath);
        FormattedText formattedCaption = new FormattedText(caption, new TextEntity[0]);

        InputMessageVideo videoContent = new InputMessageVideo();
        videoContent.video = inputFile;
        videoContent.thumbnail = null;
        videoContent.addedStickerFileIds = new int[0];
        videoContent.duration = 0;
        videoContent.width = 0;
        videoContent.height = 0;
        videoContent.supportsStreaming = true;
        videoContent.caption = formattedCaption;
        videoContent.showCaptionAboveMedia = false;
        videoContent.selfDestructType = null;
        videoContent.hasSpoiler = false;

        SendMessage sendMessage = new SendMessage();
        sendMessage.chatId = chatId;
        sendMessage.messageThreadId = 0;
        sendMessage.replyTo = null;
        sendMessage.options = createDefaultSendOptions();
        sendMessage.replyMarkup = null;
        sendMessage.inputMessageContent = videoContent;

        Message sentMessage = client.send(sendMessage).get(120, TimeUnit.SECONDS);

        return convertMessageToResult(sentMessage);
    }

    /**
     * Відправити документ
     */
    private Map<String, Object> sendDocument(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        String filePath = replacePlaceholders(
                context.getParameter("filePath", String.class),
                item
        );

        String caption = replacePlaceholders(
                context.getParameter("caption", String.class, ""),
                item
        );

        InputFile inputFile = uploadFile(filePath);
        FormattedText formattedCaption = new FormattedText(caption, new TextEntity[0]);

        InputMessageDocument documentContent = new InputMessageDocument();
        documentContent.document = inputFile;
        documentContent.thumbnail = null;
        documentContent.disableContentTypeDetection = false;
        documentContent.caption = formattedCaption;

        SendMessage sendMessage = new SendMessage();
        sendMessage.chatId = chatId;
        sendMessage.messageThreadId = 0;
        sendMessage.replyTo = null;
        sendMessage.options = createDefaultSendOptions();
        sendMessage.replyMarkup = null;
        sendMessage.inputMessageContent = documentContent;

        Message sentMessage = client.send(sendMessage).get(120, TimeUnit.SECONDS);

        return convertMessageToResult(sentMessage);
    }

    /**
     * Переслати повідомлення
     */
    private Map<String, Object> forwardMessage(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long toChatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        long fromChatId = resolveChatId(
                context.getParameter("fromChatId", String.class),
                client,
                item
        );

        long messageId = Long.parseLong(replacePlaceholders(
                context.getParameter("messageId", String.class),
                item
        ));

        ForwardMessages forwardMessages = new ForwardMessages();
        forwardMessages.chatId = toChatId;
        forwardMessages.messageThreadId = 0;
        forwardMessages.fromChatId = fromChatId;
        forwardMessages.messageIds = new long[]{messageId};
        forwardMessages.options = createDefaultSendOptions();
        forwardMessages.sendCopy = false;
        forwardMessages.removeCaption = false;
//        forwardMessages.onlyPreview = false;

        Messages forwarded = client.send(forwardMessages).get(30, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message_id", forwarded.messages[0].id);
        result.put("chat_id", toChatId);
        return result;
    }

    /**
     * Редагувати повідомлення
     */
    private Map<String, Object> editMessage(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        long messageId = Long.parseLong(replacePlaceholders(
                context.getParameter("messageId", String.class),
                item
        ));

        String newText = replacePlaceholders(
                context.getParameter("newText", String.class),
                item
        );

        String parseMode = context.getParameter("parseMode", String.class, "none");
        FormattedText formattedText = createFormattedText(newText, parseMode);

        InputMessageText messageContent = new InputMessageText();
        messageContent.text = formattedText;
        messageContent.linkPreviewOptions = null;
        messageContent.clearDraft = false;

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.chatId = chatId;
        editMessageText.messageId = messageId;
        editMessageText.replyMarkup = null;
        editMessageText.inputMessageContent = messageContent;

        Message edited = client.send(editMessageText).get(30, TimeUnit.SECONDS);

        return convertMessageToResult(edited);
    }

    /**
     * Видалити повідомлення
     */
    private Map<String, Object> deleteMessage(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        long messageId = Long.parseLong(replacePlaceholders(
                context.getParameter("messageId", String.class),
                item
        ));

        DeleteMessages deleteMessages = new DeleteMessages();
        deleteMessages.chatId = chatId;
        deleteMessages.messageIds = new long[]{messageId};
        deleteMessages.revoke = true;

        client.send(deleteMessages).get(30, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message_id", messageId);
        result.put("chat_id", chatId);
        result.put("deleted", true);
        return result;
    }

    /**
     * Додати реакцію
     */
    private Map<String, Object> sendReaction(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        long messageId = Long.parseLong(replacePlaceholders(
                context.getParameter("messageId", String.class),
                item
        ));

        String reaction = context.getParameter("reaction", String.class);

        ReactionTypeEmoji reactionType = new ReactionTypeEmoji();
        reactionType.emoji = reaction;

        AddMessageReaction addReaction = new AddMessageReaction();
        addReaction.chatId = chatId;
        addReaction.messageId = messageId;
        addReaction.reactionType = reactionType;
        addReaction.isBig = false;
        addReaction.updateRecentReactions = true;

        client.send(addReaction).get(30, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message_id", messageId);
        result.put("reaction", reaction);
        return result;
    }

    /**
     * Позначити як прочитане
     */
    private Map<String, Object> markAsRead(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        ViewMessages viewMessages = new ViewMessages();
        viewMessages.chatId = chatId;
        viewMessages.messageIds = new long[0];
        viewMessages.source = null;
        viewMessages.forceRead = true;

        client.send(viewMessages).get(30, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("chat_id", chatId);
        return result;
    }

    /**
     * Отримати історію
     */
    private Map<String, Object> getHistory(
            SimpleTelegramClient client,
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception {

        long chatId = resolveChatId(
                context.getParameter("chatId", String.class),
                client,
                item
        );

        int limit = context.getParameter("limit", Integer.class, 100);

        GetChatHistory getChatHistory = new GetChatHistory();
        getChatHistory.chatId = chatId;
        getChatHistory.fromMessageId = 0;
        getChatHistory.offset = 0;
        getChatHistory.limit = limit;
        getChatHistory.onlyLocal = false;

        Messages messages = client.send(getChatHistory).get(30, TimeUnit.SECONDS);

        List<Map<String, Object>> messagesList = new ArrayList<>();
        for (Message msg : messages.messages) {
            messagesList.add(convertMessageToResult(msg));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("chat_id", chatId);
        result.put("messages", messagesList);
        result.put("count", messagesList.size());
        return result;
    }

    // ========== Helper Methods ==========

    private long resolveChatId(
            String identifier,
            SimpleTelegramClient client,
            Map<String, Object> item
    ) throws Exception {

        identifier = replacePlaceholders(identifier, item);

        try {
            return Long.parseLong(identifier);
        } catch (NumberFormatException e) {
            // Username
        }

        if (identifier.startsWith("@")) {
            identifier = identifier.substring(1);
        }

        SearchPublicChat searchPublicChat = new SearchPublicChat();
        searchPublicChat.username = identifier;

        Chat chat = client.send(searchPublicChat).get(10, TimeUnit.SECONDS);

        return chat.id;
    }

    private InputFile uploadFile(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            InputFileRemote remoteFile = new InputFileRemote();
            remoteFile.id = path;
            return remoteFile;
        } else {
            InputFileLocal localFile = new InputFileLocal();
            localFile.path = path;
            return localFile;
        }
    }

    private FormattedText createFormattedText(String text, String parseMode) {
        // TODO: реалізувати парсинг markdown/html
        return new FormattedText(text, new TextEntity[0]);
    }

    private MessageSendOptions createDefaultSendOptions() {
        MessageSendOptions options = new MessageSendOptions();
        options.disableNotification = false;
        options.fromBackground = false;
        options.protectContent = false;
        options.updateOrderOfInstalledStickerSets = false;
        options.schedulingState = null;
        options.effectId = 0;
        options.onlyPreview = false;
        return options;
    }

    private Map<String, Object> convertMessageToResult(Message message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message_id", message.id);
        result.put("chat_id", message.chatId);
        result.put("date", message.date);
        result.put("is_outgoing", message.isOutgoing);

        if (message.content instanceof MessageText) {
            result.put("text", ((MessageText) message.content).text.text);
        }

        return result;
    }

    private String replacePlaceholders(String text, Map<String, Object> item) {
        if (text == null || item == null) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, Object> entry : item.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }
}