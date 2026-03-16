package com.workflow.sociallabs.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "workflows", indexes = {
        @Index(name = "idx_workflow_active", columnList = "active"),
        @Index(name = "idx_workflow_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Node> nodes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Connection> connections = new LinkedHashSet<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    @OrderBy("startedAt DESC")
    @Builder.Default
    private List<WorkflowExecution> executions = new ArrayList<>();

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON for additional workflow settings

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Helper methods
    public void addNode(Node node) {
        nodes.add(node);
        node.setWorkflow(this);
    }

    public void removeNode(Node node) {
        nodes.remove(node);
        node.setWorkflow(null);
    }

    public void addConnection(Connection connection) {
        connections.add(connection);
        connection.setWorkflow(this);
    }
}
