package com.workflow.sociallabs.node.nodes.telegram.bot;


import com.workflow.sociallabs.domain.enums.*;
import com.workflow.sociallabs.node.core.NodeDefinition;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.OutputDefinition;
import com.workflow.sociallabs.node.parameters.MultiOptionsParameter;
import com.workflow.sociallabs.node.parameters.OptionsParameter;
import com.workflow.sociallabs.node.parameters.StringParameter;

import java.util.*;

/**
 * Telegram Trigger Node Definition
 * Запускає workflow при отриманні повідомлення від Telegram Bot
 */
public class TelegramBotTriggerNodeDefinition extends NodeDefinition {

    public TelegramBotTriggerNodeDefinition() {
        this.executor = NodeExecutorDefinition.TELEGRAM_BOT;
        this.name = "Telegram Bot Trigger";
        this.description = "Triggers workflow when a Telegram message is received";
        this.category = NodeCategory.COMMUNICATION;
        this.icon = NodeIcon.TELEGRAM_NODE_ICON;
        this.color = NodeColor.TELEGRAM_NODE_COLOR;
        this.type = NodeType.TRIGGER;

        this.supportedCredentialType = CredentialType.TELEGRAM_BOT;

        defineParameters();
        defineOutputs();
    }

    @Override
    protected void defineParameters() {
        // Trigger Type
        addParameter(OptionsParameter.builder()
                .name("triggerOn")
                .displayName("Trigger On")
                .description("When should this trigger activate")
                .defaultValue("message")
                .required(true)
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("message", "On Message",
                                "Trigger on any message"),
                        new OptionsParameter.OptionValue("command", "On Command",
                                "Trigger on specific command"),
                        new OptionsParameter.OptionValue("callback", "On Callback Query",
                                "Trigger on inline keyboard callback"),
                        new OptionsParameter.OptionValue("channelPost", "On Channel Post",
                                "Trigger on channel post"),
                        new OptionsParameter.OptionValue("editedMessage", "On Edited Message",
                                "Trigger when message is edited")
                ))
                .build());

        // Command (if triggerOn = command)
        addParameter(StringParameter.builder()
                .name("command")
                .displayName("Command")
                .description("Command to listen for (without /)")
                .placeholder("start")
                .displayCondition("triggerOn=command")
                .build());

        // Chat ID Filter (optional)
        addParameter(StringParameter.builder()
                .name("chatIdFilter")
                .displayName("Chat ID Filter")
                .description("Only trigger for specific chat ID (leave empty for all)")
                .placeholder("123456789")
                .build());

        // Filter by message type
        addParameter(MultiOptionsParameter.builder()
                .name("messageTypes")
                .displayName("Message Types")
                .description("Filter by message types")
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("text", "Text"),
                        new OptionsParameter.OptionValue("photo", "Photo"),
                        new OptionsParameter.OptionValue("video", "Video"),
                        new OptionsParameter.OptionValue("document", "Document"),
                        new OptionsParameter.OptionValue("audio", "Audio"),
                        new OptionsParameter.OptionValue("voice", "Voice"),
                        new OptionsParameter.OptionValue("sticker", "Sticker")
                ))
                .displayCondition("triggerOn=message")
                .build());

        // Webhook path
        addParameter(StringParameter.builder()
                .name("webhookPath")
                .displayName("Webhook Path")
                .description("Custom webhook path (auto-generated if empty)")
                .placeholder("telegram-webhook")
                .build());
    }

    @Override
    protected void defineOutputs() {
        addOutput("main", OutputDefinition.builder()
                .name("main")
                .displayName("Main")
                .type("main")
                .description("Triggered when message is received")
                .schema(Map.of(
                        "message", "object",
                        "chat", "object",
                        "from", "object",
                        "text", "string",
                        "message_id", "number",
                        "date", "number"
                ))
                .build());
    }

    @Override
    public Class<? extends NodeExecutor> getExecutorClass() {
        return TelegramBotTriggerNodeExecutor.class;
    }
}