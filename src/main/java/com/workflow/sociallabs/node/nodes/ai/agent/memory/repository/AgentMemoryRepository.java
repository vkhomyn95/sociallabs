package com.workflow.sociallabs.node.nodes.ai.agent.memory.repository;

import com.workflow.sociallabs.node.nodes.ai.agent.memory.entity.AgentMemoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentMemoryRepository
        extends JpaRepository<AgentMemoryRecord, Long> {

    List<AgentMemoryRecord> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    long countBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);

    @Query("""
        DELETE FROM AgentMemoryRecord r
        WHERE r.id IN (
            SELECT r2.id FROM AgentMemoryRecord r2
            WHERE r2.sessionId = :sessionId
            ORDER BY r2.createdAt ASC
            LIMIT :count
        )
        """)
    void deleteOldestBySessionId(
            @Param("sessionId") String sessionId,
            @Param("count") long count
    );
}
