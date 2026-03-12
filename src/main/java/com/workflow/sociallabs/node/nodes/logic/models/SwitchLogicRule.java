package com.workflow.sociallabs.node.nodes.logic.models;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwitchLogicRule {
    private String outputIndex;  // "0", "1", "2"...
    private String outputName;   // "Success", "Error", ...
    private String operation;    // EQUALS, CONTAINS, GT, etc.
    private String value;        // значення для порівняння
}
