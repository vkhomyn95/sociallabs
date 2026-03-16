package com.workflow.sociallabs.dto.request;

import com.workflow.sociallabs.domain.enums.ConnectionType;
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
    private Integer sourceOutputIndex;
    private String targetNodeId;
    private Integer targetInputIndex;
    private ConnectionType type = ConnectionType.MAIN;
}
