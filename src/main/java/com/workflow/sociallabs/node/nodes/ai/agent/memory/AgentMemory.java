package com.workflow.sociallabs.node.nodes.ai.agent.memory;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import lombok.NonNull;

import java.util.List;

public interface AgentMemory {

    /**
     * Завантажити попередні повідомлення для сесії.
     * Повертає порожній список якщо сесії немає.
     */
    List<AgentMessage> load(@NonNull SessionId sessionId);

    /**
     * Зберегти НОВІ повідомлення цього запуску.
     * Реалізація сама вирішує як мержити з існуючими.
     */
    void save(@NonNull SessionId sessionId, @NonNull List<AgentMessage> newMessages);

    /**
     * Очистити пам'ять для сесії (наприклад, команда /reset)
     */
    void clear(@NonNull SessionId sessionId);

    /**
     * Тип пам'яті — для логування та UI
     */
    MemoryType getType();

    enum MemoryType {
        WINDOW_BUFFER,   // останні N повідомлень в пам'яті процесу
        DATABASE,        // MariaDB
        REDIS,           // Redis з TTL
        VECTOR           // semantic search
    }
}

