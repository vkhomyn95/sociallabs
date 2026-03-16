package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реєстр усіх node executors
 * Spring автоматично знаходить і реєструє всі NodeExecutor бінів
 */
@Slf4j
@Component
public class NodeExecutorRegistry {

    private final List<NodeExecutor> executors;

    // Мапа: NodeDiscriminator -> NodeExecutor
    private final Map<NodeDiscriminator, NodeExecutor> executorMap = new ConcurrentHashMap<>();

    /**
     * Spring автоматично inject всі NodeExecutor бінів
     */
    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        this.executors = executors;
    }

    /**
     * Ініціалізація реєстру після створення бінів
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing NodeExecutorRegistry with {} executors", executors.size());

        for (NodeExecutor executor : executors) {
            NodeDiscriminator discriminator = executor.getNodeType();

            if (executorMap.containsKey(discriminator)) {
                log.warn("Duplicate executor found for type: {}. Using the latest one.", discriminator);
            }

            executorMap.put(discriminator, executor);
            log.debug("Registered executor: {} -> {}", discriminator, executor.getClass().getSimpleName());
        }

        log.info("NodeExecutorRegistry initialized with {} unique executor types", executorMap.size());
    }

    /**
     * Отримати executor для заданого типу ноди
     *
     * @param discriminator тип ноди
     * @return executor для цього типу
     * @throws IllegalArgumentException якщо executor не знайдено
     */
    public NodeExecutor getExecutor(NodeDiscriminator discriminator) {
        NodeExecutor executor = executorMap.get(discriminator);

        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for node type: " + discriminator
            );
        }

        return executor;
    }

    /**
     * Перевірити чи є executor для заданого типу
     */
    public boolean hasExecutor(NodeDiscriminator discriminator) {
        return executorMap.containsKey(discriminator);
    }

    /**
     * Отримати всі зареєстровані типи нод
     */
    public Set<NodeDiscriminator> getRegisteredTypes() {
        return Collections.unmodifiableSet(executorMap.keySet());
    }

    /**
     * Отримати кількість зареєстрованих executors
     */
    public int getExecutorCount() {
        return executorMap.size();
    }

    /**
     * Отримати статистику реєстру
     */
    public RegistryStatistics getStatistics() {
        Map<String, Long> executorsByCategory = new HashMap<>();

        for (NodeExecutor executor : executorMap.values()) {
            String category = executor.getClass().getPackage().getName();
            executorsByCategory.merge(category, 1L, Long::sum);
        }

        return RegistryStatistics.builder()
                .totalExecutors(executorMap.size())
                .executorsByCategory(executorsByCategory)
                .registeredTypes(new ArrayList<>(executorMap.keySet()))
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class RegistryStatistics {
        private final int totalExecutors;
        private final Map<String, Long> executorsByCategory;
        private final List<NodeDiscriminator> registeredTypes;
    }
}