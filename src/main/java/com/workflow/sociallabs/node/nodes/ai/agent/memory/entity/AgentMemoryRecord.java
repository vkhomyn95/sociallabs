package com.workflow.sociallabs.node.nodes.ai.agent.memory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_memory", indexes = {
        @Index(name = "idx_memory_session", columnList = "session_id"),
        @Index(name = "idx_memory_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMemoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 500)
    private String sessionId;

    @Column(nullable = false, length = 20)
    private String role;  // USER | ASSISTANT | TOOL

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // JSON серіалізований AgentMessage

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
