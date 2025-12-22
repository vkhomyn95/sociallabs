package com.workflow.sociallabs.dto.request;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Request для створення/оновлення workflow
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRequest {

    private String name;
    private String description;
    private Boolean active;
    private List<NodeRequest> nodes;
    private List<ConnectionRequest> connections;
    private Map<String, Object> settings;
}
