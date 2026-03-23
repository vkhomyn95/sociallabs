package com.workflow.sociallabs.node.nodes.ai.agent.memory;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class MemoryConfig {

    @Builder.Default
    boolean enabled = false;

    /**
     * Який тип пам'яті використовувати
     */
    @Builder.Default
    AgentMemory.MemoryType memoryType = AgentMemory.MemoryType.WINDOW_BUFFER;

    /**
     * Звідки брати sessionId.
     * FIXED     — одна сесія для всього workflow
     * FROM_ITEM — значення з поля input item (наприклад chatId)
     * CUSTOM    — expression template
     */
    @Builder.Default
    SessionKeyStrategy sessionKeyStrategy = SessionKeyStrategy.FROM_ITEM;

    /**
     * Поле в item.json яке є ключем сесії
     * Наприклад: "chatId", "userId", "threadId"
     */
    @Nullable
    String sessionKeyField;

    /**
     * Максимум повідомлень в контексті для LLM
     * (незалежно від того скільки в storage)
     */
    @Builder.Default
    int maxWindowSize = 20;

    public static MemoryConfig disabled() {
        return MemoryConfig.builder().enabled(false).build();
    }

    public static MemoryConfig windowBuffer(String sessionKeyField) {
        return MemoryConfig.builder()
                .enabled(true)
                .memoryType(AgentMemory.MemoryType.WINDOW_BUFFER)
                .sessionKeyStrategy(SessionKeyStrategy.FROM_ITEM)
                .sessionKeyField(sessionKeyField)
                .build();
    }

    public static MemoryConfig database(String sessionKeyField) {
        return MemoryConfig.builder()
                .enabled(true)
                .memoryType(AgentMemory.MemoryType.DATABASE)
                .sessionKeyStrategy(SessionKeyStrategy.FROM_ITEM)
                .sessionKeyField(sessionKeyField)
                .build();
    }

    public enum SessionKeyStrategy { FIXED, FROM_ITEM, CUSTOM }
}
