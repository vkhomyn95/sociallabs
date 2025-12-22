package com.workflow.sociallabs.dto.response;

import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.*;

import java.util.Map;

/**
 * DTO для Node Instance у workflow
 */
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResponse {

    private String nodeId;
    private String name;

    private NodeType type;
    private NodeDiscriminator discriminator;
    private PositionDto position;

    private Map<String, Object> parameters;

    private Long credentialId;

    private Boolean disabled;

    private String notes;

    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionDto {
        private Integer x;
        private Integer y;
    }
}