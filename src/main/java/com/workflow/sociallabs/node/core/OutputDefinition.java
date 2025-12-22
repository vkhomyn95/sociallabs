package com.workflow.sociallabs.node.core;

import lombok.*;
import java.util.Map;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutputDefinition {

    private String name;
    private String displayName;
    private String type; // "main", "error", "custom"
    private String description;
    private Map<String, String> schema; // JSON schema for output data
}
