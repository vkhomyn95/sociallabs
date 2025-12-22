package com.workflow.sociallabs.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для повного Workflow
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;

    private List<NodeResponse> nodes;

    private List<ConnectionResponse> connections;

    private Map<String, Object> settings;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
