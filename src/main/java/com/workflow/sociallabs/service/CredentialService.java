package com.workflow.sociallabs.service;

import com.workflow.sociallabs.domain.entity.*;
import com.workflow.sociallabs.domain.enums.CredentialType;
import com.workflow.sociallabs.domain.repository.*;
import com.workflow.sociallabs.dto.request.CredentialRequest;
import com.workflow.sociallabs.exception.ResourceNotFoundException;
import com.workflow.sociallabs.security.CredentialEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Сервіс для роботи з Credentials
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final CredentialEncryption encryption;
    private final ObjectMapper objectMapper;

    /**
     * Отримати всі credentials (без sensitive data)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCredentials() {
        return credentialRepository.findAll().stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());
    }

    /**
     * Отримати credentials за типом
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCredentialsByType(String type) {
        return credentialRepository.findByType(
                        CredentialType.valueOf(type)).stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());
    }

    /**
     * Створити credential
     */
    @Transactional
    public Map<String, Object> createCredential(CredentialRequest request) {
        // Шифрувати дані
        String encryptedData = encryption.encrypt(serializeToJson(request.getData()));

        Credential credential = Credential.builder()
                .name(request.getName())
                .type(CredentialType.valueOf(request.getType()))
                .encryptedData(encryptedData)
                .description(request.getDescription())
                .build();

        credential = credentialRepository.save(credential);
        log.info("Created credential: {} (id={})", credential.getName(), credential.getId());

        return toSafeMap(credential);
    }

    /**
     * Оновити credential
     */
    @Transactional
    public Map<String, Object> updateCredential(Long id, CredentialRequest request) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found: " + id));

        credential.setName(request.getName());
        credential.setDescription(request.getDescription());

        if (request.getData() != null) {
            String encryptedData = encryption.encrypt(serializeToJson(request.getData()));
            credential.setEncryptedData(encryptedData);
        }

        credential = credentialRepository.save(credential);
        return toSafeMap(credential);
    }

    /**
     * Видалити credential
     */
    @Transactional
    public void deleteCredential(Long id) {
        credentialRepository.deleteById(id);
    }

    /**
     * Тестувати credential
     */
    public Map<String, Object> testCredential(Long id) {
        // Simplified
        return Map.of("success", true, "message", "Credential test not implemented");
    }

    /**
     * Конвертувати credential в safe map (без sensitive data)
     */
    private Map<String, Object> toSafeMap(Credential credential) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", credential.getId());
        map.put("name", credential.getName());
        map.put("type", credential.getType().name());
        map.put("description", credential.getDescription());
        map.put("createdAt", credential.getCreatedAt());
        return map;
    }

    private String serializeToJson(Object obj) {
        if (obj == null) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize credential data to JSON", e);
            throw new IllegalStateException("Invalid credential data format");
        }
    }
}
