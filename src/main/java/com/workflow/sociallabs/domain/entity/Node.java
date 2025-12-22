package com.workflow.sociallabs.domain.entity;

import com.workflow.sociallabs.domain.converter.NodeParametersConverter;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.model.NodeDiscriminator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "nodes", indexes = {
        @Index(name = "idx_node_workflow", columnList = "workflow_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_uuid", nullable = false, unique = true, length = 100)
    private String nodeId; // Unique ID in workflow (UUID)

    @Enumerated
    @Column(nullable = false)
    private NodeType type; // TRIGGER, ACTION, TRANSFORM

    @Enumerated
    private NodeDiscriminator discriminator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false, length = 255)
    private String name; // Display name in workflow

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Convert(converter = NodeParametersConverter.class)
    @Column(name = "parameters", columnDefinition = "TEXT")
    private NodeParameters parameters; // JSON object with parameter values

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credential_id")
    private Credential credential;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disabled = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Чи є нода тригером
     */
    public boolean isTrigger() {
        return type == NodeType.TRIGGER;
    }

    /**
     * Чи є нода action
     */
    public boolean isAction() {
        return type == NodeType.ACTION;
    }

    /**
     * Отримати параметр за ключем
     */
    public Object getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }

    /**
     * Встановити параметр
     */
    public void setParameter(String key, Object value) {
        if (parameters == null) {
            parameters = NodeParameters.builder().build();
        }
        parameters.set(key, value);
    }
}
