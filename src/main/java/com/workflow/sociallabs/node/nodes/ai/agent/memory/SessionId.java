package com.workflow.sociallabs.node.nodes.ai.agent.memory;

import java.util.Objects;

public record SessionId(String value) {

    public SessionId {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("SessionId must not be blank");
    }

    /**
     * Стандартна побудова: workflowId:nodeId:contextKey
     * contextKey — наприклад chatId або userId з input item
     */
    public static SessionId of(Long workflowId, String nodeId, String contextKey) {
        return new SessionId(workflowId + ":" + nodeId + ":" + contextKey);
    }

    public static SessionId of(String raw) { return new SessionId(raw); }
}
