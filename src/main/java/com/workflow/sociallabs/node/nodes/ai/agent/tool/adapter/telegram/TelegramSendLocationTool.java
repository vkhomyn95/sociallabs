package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.NUMBER;
import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.STRING;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientOperation.SEND;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource.LOCATION;

@Component
public final class TelegramSendLocationTool extends AbstractTelegramClientToolAdapter<TelegramSendLocationTool.Input> {

    public TelegramSendLocationTool(TelegramClientActionNodeExecutor executor) {
        super(executor);
    }

    @Override
    public String getName() {
        return "telegram_send_location";
    }

    @Override
    public String getDescription() {
        return "Send a geographic location pin to a Telegram chat.";
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
                .parameter(param("latitude", NUMBER, "Latitude (-90 to 90)", true))
                .parameter(param("longitude", NUMBER, "Longitude (-180 to 180)", true))
                .required(List.of("chatId", "latitude", "longitude"))
                .build();
    }

    @Override
    protected TelegramClientActionParameters mapInputToNodeParameters(
            Input input,
            ToolContext ctx
    ) {
        return baseBuilder(LOCATION, SEND, input.chatId())
                .latitude(input.latitude())
                .longitude(input.longitude())
                .build();
    }

    public record Input(
            @JsonProperty(required = true) @NotBlank String chatId,
            @JsonProperty(required = true) double latitude,
            @JsonProperty(required = true) double longitude
    ) implements ToolInput {
    }
}
