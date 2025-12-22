package com.workflow.sociallabs.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "connections",
        indexes = {
                @Index(name = "idx_connection_workflow", columnList = "workflow_id"),
                @Index(name = "idx_connection_source", columnList = "source_node_id"),
                @Index(name = "idx_connection_target", columnList = "target_node_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"source_node_id", "source_output", "target_node_id", "target_input"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id")
    private Node sourceNode;

    @Column(name = "source_output", nullable = false, length = 50)
    @Builder.Default
    private String sourceOutput = "main"; // Output port name

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private Node targetNode;

    @Column(name = "target_input", nullable = false, length = 50)
    @Builder.Default
    private String targetInput = "main"; // Input port name

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
