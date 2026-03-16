package com.workflow.sociallabs.domain.repository;

import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.domain.enums.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для роботи з Workflow
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    /**
     * Знайти всі активні workflows
     */
    List<Workflow> findByActive(Boolean active);

    /**
     * Знайти workflow за назвою
     */
    Optional<Workflow> findByName(String name);

    /**
     * Знайти workflows за назвою (like)
     */
    List<Workflow> findByNameContainingIgnoreCase(String name);

    /**
     * Fetch workflow з нодами та з'єднаннями в одному запиті
     */
//    @Query("""
//        SELECT DISTINCT w FROM Workflow w
//        LEFT JOIN FETCH w.nodes n
//        LEFT JOIN FETCH w.connections c
//        WHERE w.id = :workflowId
//        """)
//    Optional<Workflow> findById(@Param("workflowId") Long workflowId);

    /**
     * Знайти workflows створені після певної дати
     */
    List<Workflow> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Підрахунок активних workflows
     */
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.active = true")
    Long countActiveWorkflows();
}
