package com.workflow.sociallabs.domain.entity;

import com.workflow.sociallabs.domain.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "execution_logs", indexes = {
        @Index(name = "idx_log_execution", columnList = "execution_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private WorkflowExecution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData; // JSON

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData; // JSON

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;
}
