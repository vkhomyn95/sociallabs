package com.workflow.sociallabs.dto.response;

import lombok.*;

/**
 * DTO для Connection між нодами
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {

    private Long id;
    private String sourceNodeId;
    private String sourceOutput;      // Output port name
    private String targetNodeId;
    private String targetInput;       // Input port name
}