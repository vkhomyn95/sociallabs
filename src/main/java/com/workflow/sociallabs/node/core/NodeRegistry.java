package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.domain.enums.CredentialType;
import com.workflow.sociallabs.domain.enums.NodeCategory;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Реєстр всіх доступних нод у системі
 * Singleton pattern
 */
@Component
public class NodeRegistry {

    private static NodeRegistry instance;

    private final Map<NodeDiscriminator, NodeMetadata> nodes = new ConcurrentHashMap<>();
    private final Map<NodeDiscriminator, Class<? extends NodeExecutor>> executors = new ConcurrentHashMap<>();

    private NodeRegistry() {}

    public static synchronized NodeRegistry getInstance() {
        if (instance == null) {
            instance = new NodeRegistry();
        }
        return instance;
    }

    /**
     * Зареєструвати node з мінімальною інформацією
     */
    public void register(
            NodeDiscriminator discriminator,
            NodeType type,
            NodeCategory category,
            Class<? extends NodeExecutor> executorClass,
            CredentialType supportedCredential
    ) {
        NodeMetadata metadata = NodeMetadata.builder()
                .discriminator(discriminator)
                .type(type)
                .category(category)
                .supportedCredential(supportedCredential)
                .build();

        nodes.put(discriminator, metadata);
        executors.put(discriminator, executorClass);
    }

    /**
     * Отримати клас executor
     */
    public Optional<Class<? extends NodeExecutor>> getExecutorClass(NodeDiscriminator executor) {
        return Optional.ofNullable(executors.get(executor));
    }

    /**
     * Отримати всі node
     */

    public Collection<NodeMetadata> getAllNodes() {
        return nodes.values();
    }

    /**
     * Фільтрувати node
     */
    public List<NodeMetadata> filterNodes(
            NodeType type,
            NodeCategory category
    ) {
        return nodes.values().stream()
                .filter(node -> type == null || node.getType() == type)
                .filter(node -> category == null || node.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Метадані node (мінімальна інформація)
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class NodeMetadata {
        private NodeDiscriminator discriminator;
        private NodeType type;
        private NodeCategory category;
        private CredentialType supportedCredential;
    }
}
