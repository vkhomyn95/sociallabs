package com.workflow.sociallabs.dto.request;

import lombok.*;

/**
 * Request для створення connection
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequest {

    private String sourceNodeId;
    private String sourceOutput;
    private String targetNodeId;
    private String targetInput;
}
