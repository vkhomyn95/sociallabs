package com.workflow.sociallabs.config;

import com.workflow.sociallabs.domain.enums.CredentialType;
import com.workflow.sociallabs.domain.enums.NodeCategory;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.NodeRegistry;
import com.workflow.sociallabs.node.nodes.telegram.bot.TelegramBotActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientTriggerNodeExecutor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Конфігурація для реєстрації всіх нод у системі
 */
@Slf4j
@Configuration
public class NodeRegistrationConfig {

    private final NodeRegistry registry = NodeRegistry.getInstance();

    @PostConstruct
    public void registerNodes() {
        log.info("Registering nodes to the system...");

        // Telegram Bot Action
        registry.register(
                NodeDiscriminator.TELEGRAM_BOT_ACTION,
                NodeType.ACTION,
                NodeCategory.COMMUNICATION,
                TelegramBotActionNodeExecutor.class,
                CredentialType.API_KEY
        );

        // Telegram Client Action
        registry.register(
                NodeDiscriminator.TELEGRAM_CLIENT_ACTION,
                NodeType.ACTION,
                NodeCategory.COMMUNICATION,
                TelegramClientActionNodeExecutor.class,
                CredentialType.TELEGRAM_CLIENT
        );

        // Telegram Client Trigger
        registry.register(
                NodeDiscriminator.TELEGRAM_CLIENT_TRIGGER,
                NodeType.TRIGGER,
                NodeCategory.COMMUNICATION,
                TelegramClientTriggerNodeExecutor.class,
                CredentialType.TELEGRAM_CLIENT
        );

        log.info("Registered {} nodes", registry.getAllNodes().size());
    }
}
