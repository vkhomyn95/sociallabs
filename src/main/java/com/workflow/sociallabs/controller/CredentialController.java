package com.workflow.sociallabs.controller;

import com.workflow.sociallabs.dto.request.CredentialRequest;
import com.workflow.sociallabs.service.CredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller для роботи з Credentials
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/credentials")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * Отримати всі credentials (без sensitive data)
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllCredentials() {
        log.info("GET /api/v1/credentials - Fetching all credentials");
        List<Map<String, Object>> credentials = credentialService.getAllCredentials();
        return ResponseEntity.ok(credentials);
    }

    /**
     * Отримати credentials за типом
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Map<String, Object>>> getCredentialsByType(
            @PathVariable String type
    ) {
        log.info("GET /api/v1/credentials/type/{} - Fetching credentials by type", type);
        List<Map<String, Object>> credentials = credentialService.getCredentialsByType(type);
        return ResponseEntity.ok(credentials);
    }

    /**
     * Створити новий credential
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCredential(
            @Valid @RequestBody CredentialRequest request
    ) {
        log.info("POST /api/v1/credentials - Creating new credential: {}", request.getName());
        Map<String, Object> credential = credentialService.createCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(credential);
    }

    /**
     * Оновити credential
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCredential(
            @PathVariable Long id,
            @Valid @RequestBody CredentialRequest request
    ) {
        log.info("PUT /api/v1/credentials/{} - Updating credential", id);
        Map<String, Object> credential = credentialService.updateCredential(id, request);
        return ResponseEntity.ok(credential);
    }

    /**
     * Видалити credential
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredential(@PathVariable Long id) {
        log.info("DELETE /api/v1/credentials/{} - Deleting credential", id);
        credentialService.deleteCredential(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Тестувати credential
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testCredential(@PathVariable Long id) {
        log.info("POST /api/v1/credentials/{}/test - Testing credential", id);
        Map<String, Object> result = credentialService.testCredential(id);
        return ResponseEntity.ok(result);
    }
}

