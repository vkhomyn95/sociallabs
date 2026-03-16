package com.workflow.sociallabs.dto.mapper;

import com.workflow.sociallabs.domain.entity.Connection;
import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.dto.response.ConnectionResponse;
import com.workflow.sociallabs.dto.response.NodeResponse;
import com.workflow.sociallabs.dto.response.WorkflowResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper для конвертації між entities та DTOs
 */
@Component
public class WorkflowMapper {

    /**
     * Entity -> Response DTO
     */
    public WorkflowResponse toResponse(Workflow workflow) {
        return WorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .active(workflow.getActive())
                .nodes(workflow.getNodes().stream()
                        .map(this::toNodeInstanceResponse)
                        .collect(Collectors.toList()))
                .connections(workflow.getConnections().stream()
                        .map(this::toConnectionResponse)
                        .collect(Collectors.toList()))
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }

    /**
     * NodeInstance -> NodeInstanceResponse
     */
    public NodeResponse toNodeInstanceResponse(Node node) {
        return NodeResponse.builder()
                .nodeId(node.getNodeId())
                .name(node.getName())
                .type(node.getType())
                .discriminator(node.getDiscriminator())
                .position(NodeResponse.PositionDto.builder()
                        .x(node.getPositionX())
                        .y(node.getPositionY())
                        .build())
                .parameters(node.getParameters() != null ? node.getParameters().getValues(): null)
                .credentialId(node.getCredential() != null ? node.getCredential().getId() : null)
                .disabled(node.getDisabled())
                .notes(node.getNotes())
                .build();
    }

    /**
     * Connection -> ConnectionResponse
     */
    public ConnectionResponse toConnectionResponse(Connection connection) {
        return ConnectionResponse.builder()
                .id(connection.getId())
                .sourceNodeId(connection.getSourceNode().getNodeId())
                .sourceOutputIndex(connection.getSourceOutputIndex())
                .targetNodeId(connection.getTargetNode().getNodeId())
                .targetInputIndex(connection.getTargetInputIndex())
                .build();
    }
}