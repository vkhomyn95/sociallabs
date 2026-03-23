package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientParseMode;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.STRING;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientOperation.SEND;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource.MESSAGE;

@Component
public final class TelegramSendMessageTool extends AbstractTelegramClientToolAdapter<TelegramSendMessageTool.Input> {

    public TelegramSendMessageTool(TelegramClientActionNodeExecutor executor) {
        super(executor);
    }

    @Override
    public String getName() {
        return "telegram_send_message";
    }

    @Override
    public String getDescription() {
        return "Send a text message to a Telegram chat. Use for notifications, results, or any text communication.";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName()).description(getDescription())
                .parameter(param("chatId", STRING, "Chat ID or @username", true))
                .parameter(param("text", STRING, "Message text (Markdown supported)", true))
                .parameter(param("parseMode", STRING, "TEXT | MARKDOWN | HTML", false))
                .required(List.of("chatId", "text"))
                .build();
    }

    @Override
    protected TelegramClientActionParameters mapInputToNodeParameters(
            Input input,
            ToolContext ctx
    ) {
        return baseBuilder(MESSAGE, SEND, input.chatId())
                .text(input.text())
                .parseMode(input.parseMode() != null
                        ? TelegramClientParseMode.valueOf(input.parseMode())
                        : TelegramClientParseMode.MARKDOWN)
                .build();
    }

    public record Input(
            @JsonProperty(required = true) @NotBlank String chatId,
            @JsonProperty(required = true) @NotBlank @Size(max = 4096) String text,
            @JsonProperty @Nullable String parseMode
    ) implements ToolInput {}
}
