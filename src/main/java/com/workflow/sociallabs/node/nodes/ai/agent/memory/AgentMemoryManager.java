package com.workflow.sociallabs.node.nodes.ai.agent.memory;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public final class AgentMemoryManager {

    // Spring inject всі реалізації — вибираємо по типу
    private final Map<AgentMemory.MemoryType, AgentMemory> memoryMap;

    public AgentMemoryManager(List<AgentMemory> memories) {
        this.memoryMap = memories.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AgentMemory::getType,
                        Function.identity()
                ));
    }

    /**
     * Завантажити history для цього запуску
     */
    public List<AgentMessage> loadHistory(
            MemoryConfig config,
            Long workflowId,
            String nodeId,
            Map<String, Object> itemJson) {

        if (!config.isEnabled()) return List.of();

        AgentMemory memory = resolve(config.getMemoryType());
        SessionId sessionId = buildSessionId(config, workflowId, nodeId, itemJson);

        List<AgentMessage> history = memory.load(sessionId);

        // Обрізаємо до maxWindowSize
        int max = config.getMaxWindowSize();
        if (history.size() > max) {
            history = history.subList(history.size() - max, history.size());
        }

        log.debug("Loaded {} messages from {} for session {}", history.size(), config.getMemoryType(), sessionId.value());

        return history;
    }

    /**
     * Зберегти нові повідомлення після запуску
     */
    public void saveHistory(
            MemoryConfig config,
            Long workflowId,
            String nodeId,
            Map<String, Object> itemJson,
            List<AgentMessage> newMessages) {

        if (!config.isEnabled() || newMessages.isEmpty()) return;

        AgentMemory memory = resolve(config.getMemoryType());
        SessionId sessionId = buildSessionId(config, workflowId, nodeId, itemJson);

        memory.save(sessionId, newMessages);

        log.debug("Saved {} messages to {} for session {}", newMessages.size(), config.getMemoryType(), sessionId.value());
    }

    public void clearHistory(MemoryConfig config, Long workflowId, String nodeId, String contextKey) {
        if (!config.isEnabled()) return;
        SessionId sessionId = SessionId.of(workflowId, nodeId, contextKey);
        resolve(config.getMemoryType()).clear(sessionId);
    }

    // ── Private ─────────────────────────────────────────────────

    private AgentMemory resolve(AgentMemory.MemoryType type) {
        AgentMemory memory = memoryMap.get(type);
        if (memory == null) throw new IllegalStateException("No AgentMemory bean for type: " + type);
        return memory;
    }

    private SessionId buildSessionId(
            MemoryConfig config,
            Long workflowId,
            String nodeId,
            Map<String, Object> itemJson) {

        String contextKey = switch (config.getSessionKeyStrategy()) {
            case FIXED -> "global";
            case FROM_ITEM -> {
                String field = config.getSessionKeyField();
                if (field == null || field.isBlank()) {
                    throw new IllegalArgumentException("sessionKeyField is required for FROM_ITEM strategy");
                }
                Object val = itemJson.get(field);
                if (val == null) {
                    throw new IllegalArgumentException("Field '" + field + "' not found in item for session key");
                }
                yield val.toString();
            }
            case CUSTOM -> throw new UnsupportedOperationException("Custom expressions not yet implemented");
        };

        return SessionId.of(workflowId, nodeId, contextKey);
    }
}
