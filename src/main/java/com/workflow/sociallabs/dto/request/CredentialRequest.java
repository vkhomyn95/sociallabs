package com.workflow.sociallabs.dto.request;

import lombok.*;

import java.util.Map;

/**
 * Request для створення credential
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialRequest {

    private String name;
    private String type;                        // TELEGRAM_API, HTTP_AUTH, etc.
    private Map<String, Object> data;           // Credential data (буде зашифровано)
    private String description;
}

