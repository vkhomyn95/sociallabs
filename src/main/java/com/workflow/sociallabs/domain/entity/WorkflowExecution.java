package com.workflow.sociallabs.domain.entity;

import com.workflow.sociallabs.domain.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "workflow_executions", indexes = {
        @Index(name = "idx_execution_workflow", columnList = "workflow_id"),
        @Index(name = "idx_execution_status", columnList = "status"),
        @Index(name = "idx_execution_started", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "execution_data", columnDefinition = "TEXT")
    private String executionData; // JSON with execution context

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("executedAt ASC")
    @Builder.Default
    private List<ExecutionLog> logs = new ArrayList<>();

    @Column(name = "trigger_data", columnDefinition = "TEXT")
    private String triggerData; // JSON with trigger input

    @Column(name = "mode", length = 20)
    @Builder.Default
    private String mode = "trigger"; // trigger, manual, webhook

    public void addLog(ExecutionLog log) {
        logs.add(log);
        log.setExecution(this);
    }
}
