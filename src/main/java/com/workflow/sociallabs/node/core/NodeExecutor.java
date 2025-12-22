package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.model.NodeDiscriminator;

/**
 * Інтерфейс для виконання node
 * Всі конкретні node повинні імплементувати цей інтерфейс
 */
public interface NodeExecutor {

    /**
     * Виконати node з заданим контекстом
     */
    NodeResult execute(ExecutionContext context) throws Exception;

    /**
     * Отримати тип node
     */
    NodeDiscriminator getNodeType();

    /**
     * Чи потребує node credentials
     */
    default boolean requiresCredentials() {
        return false;
    }
}
