package com.workflow.sociallabs.dto.mapper;

import com.workflow.sociallabs.domain.entity.Connection;
import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.entity.Workflow;
import com.workflow.sociallabs.dto.response.ConnectionResponse;
import com.workflow.sociallabs.dto.response.NodeDefinitionResponse;
import com.workflow.sociallabs.dto.response.NodeResponse;
import com.workflow.sociallabs.dto.response.WorkflowResponse;
import com.workflow.sociallabs.node.core.NodeParameter;
import com.workflow.sociallabs.node.core.OutputDefinition;
import com.workflow.sociallabs.node.parameters.NumberParameter;
import com.workflow.sociallabs.node.parameters.OptionsParameter;
import com.workflow.sociallabs.node.parameters.StringParameter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
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
                .sourceOutput(connection.getSourceOutput())
                .targetNodeId(connection.getTargetNode().getNodeId())
                .targetInput(connection.getTargetInput())
                .build();
    }

    /**
     * NodeDefinition -> NodeDefinitionResponse
     */
//    public NodeDefinitionResponse toNodeDefinitionResponse(NodeDefinition definition) {
//        return NodeDefinitionResponse.builder()
//                .type(definition.getExecutor())
//                .name(definition.getName())
//                .description(definition.getDescription())
//                .category(definition.getCategory())
//                .icon(definition.getIcon())
//                .color(definition.getColor())
//                .nodeType(definition.getType().name())
//                .parameters(definition.getParameters().stream()
//                        .map(this::toParameterDto)
//                        .collect(Collectors.toList()))
//                .outputs(definition.getOutputs().entrySet().stream()
//                        .collect(Collectors.toMap(
//                                Map.Entry::getKey,
//                                e -> toOutputDto(e.getValue())
//                        )))
//                .credentialTypes(definition.getSupportedCredentialType())
//                .build();
//    }

    /**
     * NodeParameter -> NodeParameterDto
     */
    private NodeDefinitionResponse.NodeParameterDto toParameterDto(NodeParameter<?> param) {
        NodeDefinitionResponse.NodeParameterDto dto = NodeDefinitionResponse.NodeParameterDto.builder()
                .name(param.getName())
                .displayName(param.getDisplayName())
                .description(param.getDescription())
                .type(param.getType())
                .defaultValue(param.getDefaultValue())
                .required(param.isRequired())
                .hidden(param.isHidden())
                .build();

        // Додаткові поля залежно від типу
        if (param instanceof StringParameter) {
            StringParameter sp = (StringParameter) param;
            dto.setMinLength(sp.getMinLength());
            dto.setMaxLength(sp.getMaxLength());
            dto.setPattern(sp.getPattern());
            dto.setMultiline(sp.isMultiline());
            dto.setPlaceholder(sp.getPlaceholder());
        } else if (param instanceof NumberParameter) {
            NumberParameter np = (NumberParameter) param;
            dto.setMin(np.getMin());
            dto.setMax(np.getMax());
            dto.setStep(np.getStep());
        } else if (param instanceof OptionsParameter) {
            OptionsParameter op = (OptionsParameter) param;
            dto.setOptions(op.getOptions().stream()
                    .map(opt -> NodeDefinitionResponse.OptionDto.builder()
                            .value(opt.getValue())
                            .name(opt.getName())
                            .description(opt.getDescription())
                            .build())
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * OutputDefinition -> NodeOutputDto
     */
    private NodeDefinitionResponse.NodeOutputDto toOutputDto(
            OutputDefinition output
    ) {
        return NodeDefinitionResponse.NodeOutputDto.builder()
                .name(output.getName())
                .displayName(output.getDisplayName())
                .type(output.getType())
                .description(output.getDescription())
                .schema(output.getSchema())
                .build();
    }

    /**
     * Helper: parse JSON string to Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        // Тут має бути використання Jackson ObjectMapper
        // Simplified for now
        return new HashMap<>();
    }
}