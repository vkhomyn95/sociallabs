package com.workflow.sociallabs.domain.repository;

import com.workflow.sociallabs.domain.entity.WorkflowExecution;
import com.workflow.sociallabs.domain.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для роботи з WorkflowExecution
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    /**
     * Знайти всі executions workflow
     */
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(Long workflowId);

    /**
     * Знайти executions за статусом
     */
    List<WorkflowExecution> findByStatus(ExecutionStatus status);

    /**
     * Знайти executions workflow за статусом
     */
    List<WorkflowExecution> findByWorkflowIdAndStatus(
            Long workflowId,
            ExecutionStatus status
    );

    /**
     * Знайти executions за період
     */
    @Query("""
        SELECT e FROM WorkflowExecution e
        WHERE e.startedAt BETWEEN :startDate AND :endDate
        ORDER BY e.startedAt DESC
        """)
    List<WorkflowExecution> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Fetch execution з логами
     */
    @Query("""
        SELECT DISTINCT e FROM WorkflowExecution e
        LEFT JOIN FETCH e.logs
        WHERE e.id = :id
        """)
    Optional<WorkflowExecution> findByIdWithLogs(@Param("id") Long id);

    /**
     * Знайти активні (running) executions
     */
    @Query("""
        SELECT e FROM WorkflowExecution e
        WHERE e.status = 'RUNNING'
        AND e.startedAt < :timeout
        """)
    List<WorkflowExecution> findStuckExecutions(@Param("timeout") LocalDateTime timeout);

    /**
     * Підрахунок executions за статусом
     */
    Long countByStatus(ExecutionStatus status);

    /**
     * Підрахунок executions workflow
     */
    Long countByWorkflowId(Long workflowId);

    /**
     * Видалити старі executions
     */
    @Modifying
    @Query("""
        DELETE FROM WorkflowExecution e
        WHERE e.finishedAt < :date
        """)
    void deleteOldExecutions(@Param("date") LocalDateTime date);

    /**
     * Статистика executions за workflow
     */
    @Query("""
        SELECT e.status, COUNT(e)
        FROM WorkflowExecution e
        WHERE e.workflow.id = :workflowId
        GROUP BY e.status
        """)
    List<Object[]> getExecutionStatistics(@Param("workflowId") Long workflowId);
}
