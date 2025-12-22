package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.domain.enums.*;
import com.workflow.sociallabs.node.core.NodeDefinition;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.OutputDefinition;
import com.workflow.sociallabs.node.parameters.BooleanParameter;
import com.workflow.sociallabs.node.parameters.NumberParameter;
import com.workflow.sociallabs.node.parameters.OptionsParameter;
import com.workflow.sociallabs.node.parameters.StringParameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Відправка повідомлень через Telegram Client API (TDLight)
 */
public class TelegramClientActionNodeDefinition extends NodeDefinition {

    public TelegramClientActionNodeDefinition() {
        this.executor = NodeExecutorDefinition.TELEGRAM_CLIENT;
        this.name = "Telegram client action";
        this.description = "Send messages, photos, and perform actions via Telegram Client";
        this.category = NodeCategory.COMMUNICATION;
        this.icon = NodeIcon.TELEGRAM_NODE_ICON;
        this.color = NodeColor.TELEGRAM_NODE_COLOR;
        this.type = NodeType.ACTION;

        this.supportedCredentialType = CredentialType.TELEGRAM_CLIENT;

        defineParameters();
        defineOutputs();
    }

    @Override
    protected void defineParameters() {
        addParameter(OptionsParameter.builder()
                .name("operation")
                .displayName("Operation")
                .description("What action to perform")
                .defaultValue("sendMessage")
                .required(true)
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("sendMessage", "Send Message"),
                        new OptionsParameter.OptionValue("sendPhoto", "Send Photo"),
                        new OptionsParameter.OptionValue("sendVideo", "Send Video"),
                        new OptionsParameter.OptionValue("sendDocument", "Send Document"),
                        new OptionsParameter.OptionValue("forwardMessage", "Forward Message"),
                        new OptionsParameter.OptionValue("editMessage", "Edit Message"),
                        new OptionsParameter.OptionValue("deleteMessage", "Delete Message"),
                        new OptionsParameter.OptionValue("sendReaction", "Send Reaction"),
                        new OptionsParameter.OptionValue("readMessages", "Mark as Read"),
                        new OptionsParameter.OptionValue("getHistory", "Get Chat History")
                ))
                .build());

        addParameter(StringParameter.builder()
                .name("chatId")
                .displayName("Chat ID or Username")
                .description("Target chat (@username, user ID, or chat ID)")
                .required(true)
                .placeholder("@username or 123456789")
                .displayCondition("operation=sendMessage,sendPhoto,sendVideo,sendDocument,forwardMessage,readMessages,getHistory")
                .build());

        addParameter(StringParameter.builder()
                .name("text")
                .displayName("Message Text")
                .description("Text to send")
                .required(true)
                .multiline(true)
                .placeholder("Your message...")
                .displayCondition("operation=sendMessage")
                .build());

        addParameter(StringParameter.builder()
                .name("caption")
                .displayName("Caption")
                .description("Media caption")
                .multiline(true)
                .displayCondition("operation=sendPhoto,sendVideo,sendDocument")
                .build());

        addParameter(StringParameter.builder()
                .name("filePath")
                .displayName("File Path or URL")
                .description("Local file path or HTTP URL")
                .required(true)
                .placeholder("/path/to/file.jpg or https://example.com/image.jpg")
                .displayCondition("operation=sendPhoto,sendVideo,sendDocument")
                .build());

        addParameter(StringParameter.builder()
                .name("messageId")
                .displayName("Message ID")
                .description("ID of message to edit/delete/forward")
                .required(true)
                .displayCondition("operation=editMessage,deleteMessage,forwardMessage,sendReaction")
                .build());

        addParameter(StringParameter.builder()
                .name("newText")
                .displayName("New Text")
                .description("Updated message text")
                .required(true)
                .multiline(true)
                .displayCondition("operation=editMessage")
                .build());

        addParameter(StringParameter.builder()
                .name("fromChatId")
                .displayName("From Chat ID")
                .description("Source chat to forward from")
                .required(true)
                .displayCondition("operation=forwardMessage")
                .build());

        addParameter(StringParameter.builder()
                .name("reaction")
                .displayName("Reaction")
                .description("Emoji reaction (👍, ❤️, 🔥, etc.)")
                .required(true)
                .placeholder("👍")
                .displayCondition("operation=sendReaction")
                .build());

        addParameter(NumberParameter.builder()
                .name("limit")
                .displayName("Messages Limit")
                .description("Number of messages to retrieve")
                .defaultValue(100)
                .min(1)
                .max(1000)
                .displayCondition("operation=getHistory")
                .build());

        addParameter(OptionsParameter.builder()
                .name("parseMode")
                .displayName("Parse Mode")
                .description("Text formatting mode")
                .defaultValue("none")
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("none", "None"),
                        new OptionsParameter.OptionValue("markdown", "Markdown"),
                        new OptionsParameter.OptionValue("html", "HTML")
                ))
                .displayCondition("operation=sendMessage,editMessage")
                .build());

        addParameter(BooleanParameter.builder()
                .name("disableNotification")
                .displayName("Silent")
                .description("Send message silently")
                .defaultValue(false)
                .displayCondition("operation=sendMessage,sendPhoto,sendVideo,sendDocument")
                .build());

        addParameter(StringParameter.builder()
                .name("replyToMessageId")
                .displayName("Reply to Message ID")
                .description("Reply to specific message")
                .displayCondition("operation=sendMessage,sendPhoto,sendVideo,sendDocument")
                .build());
    }

    @Override
    protected void defineOutputs() {
        Map<String, String> schema = new HashMap<>();
        schema.put("message_id", "number");
        schema.put("chat_id", "number");
        schema.put("success", "boolean");
        schema.put("result", "object");

        addOutput("main", OutputDefinition.builder()
                .name("main")
                .displayName("Main Output")
                .type("main")
                .description("Operation result")
                .schema(schema)
                .build());
    }

    @Override
    public Class<? extends NodeExecutor> getExecutorClass() {
        return TelegramClientActionNodeExecutor.class;
    }
}