package com.workflow.sociallabs.domain.repository;

import com.workflow.sociallabs.domain.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для роботи з Connection
 */
@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    /**
     * Знайти всі з'єднання workflow
     */
    List<Connection> findByWorkflowId(Long workflowId);

    /**
     * Знайти всі вихідні з'єднання від ноди
     */
    List<Connection> findBySourceNodeId(Long sourceNodeId);

    /**
     * Знайти всі вхідні з'єднання до ноди
     */
    List<Connection> findByTargetNodeId(Long targetNodeId);

    /**
     * Знайти з'єднання між двома нодами
     */
    Optional<Connection> findBySourceNodeIdAndTargetNodeId(
            Long sourceNodeId,
            Long targetNodeId
    );

    /**
     * Перевірити чи існує з'єднання
     */
    boolean existsBySourceNodeIdAndSourceOutputAndTargetNodeIdAndTargetInput(
            Long sourceNodeId,
            String sourceOutput,
            Long targetNodeId,
            String targetInput
    );

    /**
     * Видалити всі з'єднання workflow
     */
    void deleteByWorkflowId(Long workflowId);

    /**
     * Видалити всі з'єднання що містять ноду
     */
    @Query("""
        DELETE FROM Connection c
        WHERE c.sourceNode.id = :nodeId
        OR c.targetNode.id = :nodeId
        """)
    @Modifying
    void deleteByNodeId(@Param("nodeId") Long nodeId);

    /**
     * Підрахунок з'єднань у workflow
     */
    Long countByWorkflowId(Long workflowId);
}
