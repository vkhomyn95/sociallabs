package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolOutput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.ARRAY;
import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.STRING;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientOperation.SEND;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource.POLL;

@Component
public final class TelegramSendPollTool extends AbstractTelegramClientToolAdapter<TelegramSendPollTool.Input> {

    public TelegramSendPollTool(TelegramClientActionNodeExecutor e) {
        super(e);
    }

    @Override
    public String getName() {
        return "telegram_send_poll";
    }

    @Override
    public String getDescription() {
        return "Send a poll to a Telegram chat.";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName()).description(getDescription())
                .parameter(param("chatId", STRING, "Chat ID", true))
                .parameter(param("question", STRING, "Poll question", true))
                .parameter(param("options", ARRAY, "List of answer options (min 2)", true))
                .required(List.of("chatId", "question", "options"))
                .build();
    }

    @Override
    protected TelegramClientActionParameters mapInputToNodeParameters(
            Input input, ToolContext ctx) {
        return baseBuilder(POLL, SEND, input.chatId())
                .question(input.question())
                .pollOptions(input.options())
                .build();
    }

    public record Input(
            @JsonProperty(required = true) @NotBlank String chatId,
            @JsonProperty(required = true) @NotBlank String question,
            @JsonProperty(required = true) @Size(min = 2, max = 10) List<String> options
    ) implements ToolInput {
    }
}
