package com.workflow.sociallabs.node.nodes.telegram.client.handlers;

import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@Slf4j
public class TelegramClientMessageResourceHandler extends TelegramClientResourceHandler {

    public static Map<String, Object> execute(
            SimpleTelegramClient client,
            TelegramClientActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendMessage(client, params, item);
//            case EDIT -> executeEditMessage(client, params, item);
//            case DELETE -> executeDeleteMessage(client, params, item);
//            case FORWARD -> executeForwardMessages(client, params, item);
//            case PIN -> executePinMessage(client, params, item);
//            case UNPIN -> executeUnpinMessage(client, params, item);
//            case READ -> executeReadMessages(client, params, item);
//            case GET -> executeGetMessage(client, params, item);
//            case GET_HISTORY -> executeGetHistory(client, params, item);
//            case SEND_TYPING -> executeSendTyping(client, params, item);
            default -> throw new IllegalArgumentException("Unsupported operation for message: " + params.getOperation());
        };
    }

    private static Map<String, Object> executeSendMessage(
            SimpleTelegramClient client,
            TelegramClientActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        long chatId = parseChatId(params.getChatId(), item);

        TdApi.InputMessageText inputMessage = createInputMessageText(params.getText(), params, item);
        TdApi.MessageSendOptions sendOptions = createSendOptions(params);
        TdApi.ReplyMarkup replyMarkup = createReplyMarkup(params);

        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.messageThreadId = params.getMessageThreadId() != null ? params.getMessageThreadId() : 0;
//        request.replyTo = params.getReplyToMessageId() != null
//                ? new TdApi.InputMessageReplyToMessage(chatId, params.getReplyToMessageId(), null)
//                : null;
        request.options = sendOptions;
        request.replyMarkup = replyMarkup;
        request.inputMessageContent = inputMessage;

        TdApi.Message response = sendTelegramRequest(client, request, params);
        return messageToMap(response);
    }

//    private static Map<String, Object> executeEditMessage(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//        long messageId = params.getMessageId();
//
//        TdApi.InputMessageText inputMessage = createInputMessageText(params.getText(), params, item);
//        TdApi.ReplyMarkup replyMarkup = createReplyMarkup(params);
//
//        TdApi.EditMessageText request = new TdApi.EditMessageText();
//        request.chatId = chatId;
//        request.messageId = messageId;
//        request.replyMarkup = replyMarkup;
//        request.inputMessageContent = inputMessage;
//
//        TdApi.Message response = sendTelegramRequest(client, request, params);
//        return messageToMap(response);
//    }
//
//    private static Map<String, Object> executeDeleteMessage(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//        long messageId = params.getMessageId();
//
//        TdApi.DeleteMessages request = new TdApi.DeleteMessages();
//        request.chatId = chatId;
//        request.messageIds = new long[]{messageId};
//        request.revoke = params.getRevoke();
//
//        TdApi.Ok response = sendTelegramRequest(client, request, params);
//        return Map.of("deleted", true, "message_id", messageId);
//    }
//
//    private static Map<String, Object> executeForwardMessages(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//        long fromChatId = parseChatId(params.getFromChatId(), item);
//
//        long[] messageIds = params.getMessageIds().stream()
//                .mapToLong(Long::longValue)
//                .toArray();
//
//        TdApi.MessageSendOptions sendOptions = createSendOptions(params);
//
//        TdApi.ForwardMessages request = new TdApi.ForwardMessages();
//        request.chatId = chatId;
//        request.messageThreadId = params.getMessageThreadId() != null ? params.getMessageThreadId() : 0;
//        request.fromChatId = fromChatId;
//        request.messageIds = messageIds;
//        request.options = sendOptions;
//        request.sendCopy = params.getSendCopy();
//        request.removeCaption = params.getRemoveCaption();
//
//        TdApi.Messages response = sendTelegramRequest(client, request, params);
//        return Map.of("forwarded", true, "count", response.messages.length);
//    }
//
//    private static Map<String, Object> executePinMessage(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//        long messageId = params.getMessageId();
//
//        TdApi.PinChatMessage request = new TdApi.PinChatMessage();
//        request.chatId = chatId;
//        request.messageId = messageId;
//        request.disableNotification = params.getDisableNotification();
//        request.onlyForSelf = false;
//
//        TdApi.Ok response = sendTelegramRequest(client, request, params);
//        return Map.of("pinned", true, "message_id", messageId);
//    }
//
//    private static Map<String, Object> executeUnpinMessage(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//
//        if (params.getMessageId() != null) {
//            TdApi.UnpinChatMessage request = new TdApi.UnpinChatMessage();
//            request.chatId = chatId;
//            request.messageId = params.getMessageId();
//
//            TdApi.Ok response = sendTelegramRequest(client, request, params);
//            return Map.of("unpinned", true, "message_id", params.getMessageId());
//        } else {
//            TdApi.UnpinAllChatMessages request = new TdApi.UnpinAllChatMessages(chatId);
//            TdApi.Ok response = sendTelegramRequest(client, request, params);
//            return Map.of("unpinned_all", true);
//        }
//    }
//
//    private static Map<String, Object> executeReadMessages(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//
//        TdApi.ViewMessages request = new TdApi.ViewMessages();
//        request.chatId = chatId;
//        request.messageThreadId = params.getMessageThreadId() != null ? params.getMessageThreadId() : 0;
//        request.messageIds = params.getMessageIds() != null
//                ? params.getMessageIds().stream().mapToLong(Long::longValue).toArray()
//                : new long[]{params.getMessageId()};
//        request.forceRead = params.getMarkAsRead();
//
//        TdApi.Ok response = sendTelegramRequest(client, request, params);
//        return Map.of("read", true);
//    }
//
//    private static Map<String, Object> executeGetMessage(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//        long messageId = params.getMessageId();
//
//        TdApi.GetMessage request = new TdApi.GetMessage(chatId, messageId);
//        TdApi.Message response = sendTelegramRequest(client, request, params);
//
//        return messageToMap(response);
//    }
//
//    private static Map<String, Object> executeGetHistory(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//
//        TdApi.GetChatHistory request = new TdApi.GetChatHistory();
//        request.chatId = chatId;
//        request.fromMessageId = params.getMessageId() != null ? params.getMessageId() : 0;
//        request.offset = 0;
//        request.limit = 100;
//        request.onlyLocal = false;
//
//        TdApi.Messages response = sendTelegramRequest(client, request, params);
//        return Map.of("messages", response.messages.length, "total_count", response.totalCount);
//    }
//
//    private static Map<String, Object> executeSendTyping(
//            SimpleTelegramClient client,
//            TelegramClientActionParameters params,
//            Map<String, Object> item
//    ) throws Exception {
//        long chatId = parseChatId(params.getChatId(), item);
//
//        TdApi.SendChatAction request = new TdApi.SendChatAction();
//        request.chatId = chatId;
//        request.messageThreadId = params.getMessageThreadId() != null ? params.getMessageThreadId() : 0;
//        request.action = params.getChatAction().toTdApi();
//
//        TdApi.Ok response = sendTelegramRequest(client, request, params);
//        return Map.of("action_sent", true, "action", params.getChatAction());
//    }
}
