package com.workflow.sociallabs.node.nodes.telegram.bot;

import com.workflow.sociallabs.domain.enums.*;
import com.workflow.sociallabs.node.core.NodeDefinition;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.OutputDefinition;
import com.workflow.sociallabs.node.parameters.*;

import java.util.Arrays;
import java.util.Map;

/**
 * Визначення Telegram ноди
 * Містить метадані та параметри для UI
 */
public class TelegramBotActionNodeDefinition extends NodeDefinition {

    public TelegramBotActionNodeDefinition() {
        this.executor = NodeExecutorDefinition.TELEGRAM_BOT;
        this.name = "Telegram bot action";
        this.description = "Send messages, photos, and files via Telegram Bot";
        this.category = NodeCategory.COMMUNICATION;
        this.icon = NodeIcon.TELEGRAM_NODE_ICON;
        this.color = NodeColor.TELEGRAM_NODE_COLOR;
        this.type = NodeType.ACTION;

        this.supportedCredentialType = CredentialType.TELEGRAM_BOT;

        defineParameters();
        defineOutputs();
    }

    @Override
    protected void defineParameters() {
        // Resource (що відправляти)
        addParameter(OptionsParameter.builder()
                .name("resource")
                .displayName("Resource")
                .description("The resource to operate on")
                .defaultValue("message")
                .required(true)
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("message", "Message", "Send a text message"),
                        new OptionsParameter.OptionValue("photo", "Photo", "Send a photo"),
                        new OptionsParameter.OptionValue("document", "Document", "Send a document"),
                        new OptionsParameter.OptionValue("location", "Location", "Send a location")
                ))
                .build());

        // Operation (що робити)
        addParameter(OptionsParameter.builder()
                .name("operation")
                .displayName("Operation")
                .description("The operation to perform")
                .defaultValue("send")
                .required(true)
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("send", "Send", "Send a message"),
                        new OptionsParameter.OptionValue("edit", "Edit", "Edit a message"),
                        new OptionsParameter.OptionValue("delete", "Delete", "Delete a message")
                ))
                .displayCondition("resource=message")
                .build());

        // Chat ID
        addParameter(StringParameter.builder()
                .name("chatId")
                .displayName("Chat ID")
                .description("Unique identifier for the target chat or username")
                .required(true)
                .placeholder("123456789 or @channel_name")
                .build());

        // Message Text
        addParameter(StringParameter.builder()
                .name("text")
                .displayName("Message")
                .description("Text of the message to be sent")
                .required(true)
                .multiline(true)
                .placeholder("Your message here...")
                .displayCondition("resource=message")
                .build());

        // Parse Mode
        addParameter(OptionsParameter.builder()
                .name("parseMode")
                .displayName("Parse Mode")
                .description("Mode for parsing entities in the message text")
                .defaultValue("none")
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("none", "None"),
                        new OptionsParameter.OptionValue("Markdown", "Markdown"),
                        new OptionsParameter.OptionValue("MarkdownV2", "Markdown V2"),
                        new OptionsParameter.OptionValue("HTML", "HTML")
                ))
                .displayCondition("resource=message")
                .build());

        // Disable Web Page Preview
        addParameter(BooleanParameter.builder()
                .name("disableWebPagePreview")
                .displayName("Disable Web Page Preview")
                .description("Disables link previews for links in this message")
                .defaultValue(false)
                .displayCondition("resource=message")
                .build());

        // Disable Notification
        addParameter(BooleanParameter.builder()
                .name("disableNotification")
                .displayName("Disable Notification")
                .description("Sends the message silently")
                .defaultValue(false)
                .build());

        // Reply to Message ID
        addParameter(StringParameter.builder()
                .name("replyToMessageId")
                .displayName("Reply to Message ID")
                .description("If the message is a reply, ID of the original message")
                .placeholder("123456")
                .build());

        // Photo URL (для resource=photo)
        addParameter(StringParameter.builder()
                .name("photoUrl")
                .displayName("Photo URL")
                .description("HTTP URL or file_id of the photo")
                .required(true)
                .displayCondition("resource=photo")
                .placeholder("https://example.com/image.jpg")
                .build());

        // Caption (для photo/document)
        addParameter(StringParameter.builder()
                .name("caption")
                .displayName("Caption")
                .description("Photo or document caption")
                .multiline(true)
                .displayCondition("resource=photo,document")
                .build());

        // Document URL (для resource=document)
        addParameter(StringParameter.builder()
                .name("documentUrl")
                .displayName("Document URL")
                .description("HTTP URL or file_id of the document")
                .required(true)
                .displayCondition("resource=document")
                .placeholder("https://example.com/file.pdf")
                .build());

        // Inline Keyboard
        addParameter(JsonParameter.builder()
                .name("replyMarkup")
                .displayName("Reply Markup")
                .description("Additional interface options (inline keyboard)")
                .build());

        // Additional Options
        addParameter(CollectionParameter.builder()
                .name("additionalFields")
                .displayName("Additional Fields")
                .description("Add additional optional fields")
                .fields(Arrays.asList(
                        StringParameter.builder()
                                .name("protectContent")
                                .displayName("Protect Content")
                                .build(),
                        StringParameter.builder()
                                .name("messageThreadId")
                                .displayName("Message Thread ID")
                                .build()
                ))
                .build());
    }

    @Override
    protected void defineOutputs() {
        addOutput("main", OutputDefinition.builder()
                .name("main")
                .displayName("Main")
                .type("main")
                .description("Standard output with message result")
                .schema(Map.of(
                        "message_id", "number",
                        "chat", "object",
                        "text", "string",
                        "date", "number"
                ))
                .build());
    }

    @Override
    public Class<? extends NodeExecutor> getExecutorClass() {
        return TelegramBotActionNodeExecutor.class;
    }
}