package com.workflow.sociallabs.dto.response;

import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {

    private Long id;
    private String sourceNodeId;
    private Integer sourceOutputIndex;      // Output port name
    private String targetNodeId;
    private Integer targetInputIndex;       // Input port name
}