package com.workflow.sociallabs.node.nodes.ai.agent.memory.impl;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.AgentMemory;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.SessionId;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.entity.AgentMemoryRecord;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.repository.AgentMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public final class DatabaseMemory implements AgentMemory {

    private final AgentMemoryRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${agent.memory.db.max-messages:100}")
    private int maxMessages;

    @Override
    public List<AgentMessage> load(SessionId sessionId) {
        List<AgentMemoryRecord> records =
                repository.findBySessionIdOrderByCreatedAtAsc(sessionId.value());

        return records.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public void save(SessionId sessionId, List<AgentMessage> newMessages) {
        List<AgentMemoryRecord> records = newMessages.stream()
                .map(msg -> AgentMemoryRecord.builder()
                        .sessionId(sessionId.value())
                        .role(msg.role().name())
                        .content(serialize(msg))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();

        repository.saveAll(records);

        // Обрізаємо старі якщо перевищуємо ліміт
        long count = repository.countBySessionId(sessionId.value());
        if (count > maxMessages) {
            long toDelete = count - maxMessages;
            repository.deleteOldestBySessionId(sessionId.value(), toDelete);
            log.debug("Trimmed {} old messages for session {}", toDelete, sessionId.value());
        }
    }

    @Override
    @Transactional
    public void clear(SessionId sessionId) {
        repository.deleteBySessionId(sessionId.value());
    }

    @Override
    public MemoryType getType() { return MemoryType.DATABASE; }

    private String serialize(AgentMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message", e);
            return "{}";
        }
    }

    private AgentMessage deserialize(AgentMemoryRecord record) {
        try {
            return switch (AgentMessage.Role.valueOf(record.getRole())) {
                case USER      -> objectMapper.readValue(record.getContent(),
                        AgentMessage.UserMessage.class);
                case ASSISTANT -> objectMapper.readValue(record.getContent(),
                        AgentMessage.AssistantMessage.class);
                case TOOL      -> objectMapper.readValue(record.getContent(),
                        AgentMessage.ToolResultMessage.class);
            };
        } catch (Exception e) {
            log.warn("Failed to deserialize memory record {}", record.getId(), e);
            return null;
        }
    }
}
