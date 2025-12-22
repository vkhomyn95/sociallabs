package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.domain.enums.*;
import com.workflow.sociallabs.node.core.NodeDefinition;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.OutputDefinition;
import com.workflow.sociallabs.node.parameters.OptionsParameter;
import com.workflow.sociallabs.node.parameters.StringParameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Тригер для отримання повідомлень через Telegram Client API (TDLight)
 * Працює як користувач (не бот), може читати канали, групи, приватні чати
 */
public class TelegramClientTriggerNodeDefinition extends NodeDefinition {

    public TelegramClientTriggerNodeDefinition() {
        this.type = NodeType.TRIGGER;
        this.executor = NodeExecutorDefinition.TELEGRAM_CLIENT;
        this.name = "Telegram client trigger";
        this.description = "Triggers on new messages in channels, groups or private chats via Telegram Client";
        this.category = NodeCategory.COMMUNICATION;
        this.icon = NodeIcon.TELEGRAM_NODE_ICON;
        this.color = NodeColor.TELEGRAM_NODE_COLOR;

        this.supportedCredentialType = CredentialType.TELEGRAM_CLIENT;

        defineParameters();
        defineOutputs();
    }

    @Override
    protected void defineParameters() {
        addParameter(OptionsParameter.builder()
                .name("triggerOn")
                .displayName("Trigger On")
                .description("What type of update to listen for")
                .defaultValue("newMessage")
                .required(true)
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("newMessage", "New Message"),
                        new OptionsParameter.OptionValue("messageEdited", "Message Edited"),
                        new OptionsParameter.OptionValue("messageDeleted", "Message Deleted")
                ))
                .build());

        addParameter(StringParameter.builder()
                .name("chatIdentifier")
                .displayName("Chat ID or Username")
                .description("Channel username (@channel), chat ID, or leave empty for all")
                .placeholder("@mychannel or -1001234567890")
                .build());

        addParameter(OptionsParameter.builder()
                .name("messageFilter")
                .displayName("Message Filter")
                .description("Filter messages by type")
                .defaultValue("all")
                .options(Arrays.asList(
                        new OptionsParameter.OptionValue("all", "All Messages"),
                        new OptionsParameter.OptionValue("text", "Text Only"),
                        new OptionsParameter.OptionValue("photo", "Photos"),
                        new OptionsParameter.OptionValue("video", "Videos")
                ))
                .build());

        addParameter(StringParameter.builder()
                .name("contentFilter")
                .displayName("Content Filter")
                .description("Only trigger if message contains this text")
                .placeholder("keyword")
                .build());
    }

    @Override
    protected void defineOutputs() {
        Map<String, String> schema = new HashMap<>();
        schema.put("message_id", "number");
        schema.put("chat_id", "number");
        schema.put("chat_title", "string");
        schema.put("sender_id", "number");
        schema.put("sender_name", "string");
        schema.put("text", "string");
        schema.put("date", "number");
        schema.put("is_channel_post", "boolean");
        schema.put("reply_to_message_id", "number");
        schema.put("forward_info", "object");
        schema.put("media_type", "string");

        addOutput("main", OutputDefinition.builder()
                .name("main")
                .displayName("Main Output")
                .type("main")
                .description("Message data")
                .schema(schema)
                .build());
    }

    @Override
    public Class<? extends NodeExecutor> getExecutorClass() {
        return TelegramClientTriggerNodeExecutor.class;
    }
}