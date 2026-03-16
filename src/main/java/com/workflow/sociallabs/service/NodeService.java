package com.workflow.sociallabs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.domain.entity.Credential;
import com.workflow.sociallabs.domain.enums.NodeCategory;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.exception.ResourceNotFoundException;
import com.workflow.sociallabs.exception.ValidationException;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.*;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import com.workflow.sociallabs.security.CredentialEncryption;
import com.workflow.sociallabs.utility.MapSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з Nodes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeRegistry nodeRegistry = NodeRegistry.getInstance();

    private final CredentialEncryption encryption;
    private final CredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    /**
     * Отримати доступні node з фільтрацією та пагінацією
     */
    public List<NodeRegistry.NodeMetadata> getAvailableNodes(
            NodeType type,
            NodeCategory category,
            Pageable pageable
    ) {
        List<NodeRegistry.NodeMetadata> filteredNodes = nodeRegistry.filterNodes(type, category);

        // Сортування (за executor name)
        filteredNodes.sort(Comparator.comparing(NodeRegistry.NodeMetadata::getDiscriminator));

        // Пагінація
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredNodes.size());

        return filteredNodes.subList(start, end);
    }

    public void validateNodeParameters(NodeParameters parameters, NodeDiscriminator discriminator) {
        try {
            TypedNodeParameters typed = objectMapper.convertValue(parameters.getValues(), TypedNodeParameters.class);
            if (typed != null) {
                typed.validate();
                log.debug("Parameters validated for {}", discriminator);
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid parameters for node " + discriminator + ": " + e.getMessage());
        } catch (Exception e) {
            log.warn("Could not validate parameters for {}: {}", discriminator, e.getMessage());
        }
    }

    /**
     * Тестове виконання ноди з UI.
     *
     * Очікуваний формат testData:
     * {
     *   "parameters": { ... },
     *   "credentialId": 1,
     *   "inputItems": [              ← масив items
     *     { "json": { "key": "val" } }
     *   ]
     * }
     */
    public Map<String, Object> testNodeExecution(NodeDiscriminator discriminator, Map<String, Object> testData) {

        log.info("Testing node execution: {}", discriminator);

        try {
            NodeRegistry.NodeMetadata metadata = resolveMetadata(discriminator);

            // 1. Параметри
            @SuppressWarnings("unchecked")
            Map<String, Object> rawParams = (Map<String, Object>) testData.getOrDefault("parameters", Map.of());

            NodeParameters nodeParameters = convertToNodeParameters(rawParams, discriminator);
            TypedNodeParameters typedParams = validateAndConvertParameters(
                    nodeParameters, discriminator);

            // 2. Credentials
            Map<String, Object> credentials = resolveCredentials(testData, metadata, discriminator);

            // 3. Input items — підтримуємо два формати від UI:
            //    а) новий: "inputItems": [ { "json": {...} }, ... ]
            //    б) legacy: "inputData": [ {...}, {...} ]
            List<WorkflowItem> inputItems = resolveInputItems(testData);

            // 5. ExecutionContext
            ExecutionContext context = ExecutionContext.builder()
                    .executionId(-1L)
                    .workflowId(-1L)
                    .nodeId(UUID.randomUUID().toString())
                    .inputItems(inputItems)        // List<WorkflowItem>
                    .credentials(credentials)
                    .parameters(typedParams)
                    .build();

            // 6. Execute
            NodeExecutor executor = createExecutor(discriminator);
            NodeResult result = executor.execute(context);

            return formatTestResult(result, discriminator);

        } catch (ValidationException | ResourceNotFoundException e) {
            log.error("Test execution validation failed: {}", e.getMessage());
            return createErrorResult(e.getMessage(), "VALIDATION_ERROR");
        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            return createErrorResult(e.getMessage(), "EXECUTION_ERROR");
        }
    }

    public NodeParameters convertToNodeParameters(Map<String, Object> raw, NodeDiscriminator discriminator) {

        if (raw == null || raw.isEmpty()) {
            return NodeParameters.withType(discriminator.value, new HashMap<>());
        }
        Map<String, Object> params = new HashMap<>(raw);
        params.put("@type", discriminator.value);
        return NodeParameters.withType(discriminator.value, params);
    }

    /**
     * Валідувати та конвертувати параметри в TypedNodeParameters
     */
    private TypedNodeParameters validateAndConvertParameters(
            NodeParameters parameters,
            NodeDiscriminator discriminator
    ) {
        try {
            TypedNodeParameters typedParams = objectMapper.convertValue(
                    parameters.getValues(),
                    TypedNodeParameters.class
            );

            if (typedParams != null) {
                typedParams.validate();
                log.debug("Parameters validated successfully for {}", discriminator);
            }

            return typedParams;

        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid parameters for node " + discriminator + ": " + e.getMessage()
            );
        } catch (Exception e) {
            log.warn("Could not validate parameters for {}: {}", discriminator, e.getMessage());
            return null;
        }
    }

    private NodeRegistry.NodeMetadata resolveMetadata(NodeDiscriminator discriminator) {
        return nodeRegistry.getAllNodes().stream()
                .filter(n -> n.getDiscriminator() == discriminator)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Node type not registered: " + discriminator));
    }

    private Map<String, Object> resolveCredentials(
            Map<String, Object> testData,
            NodeRegistry.NodeMetadata metadata,
            NodeDiscriminator discriminator) {

        Long credentialId = extractCredentialId(testData);

        if (credentialId == null) {
            if (metadata.getSupportedCredential() != null) {
                log.warn("Node {} requires credential type {} but none provided", discriminator, metadata.getSupportedCredential());
            }
            return new HashMap<>();
        }

        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found: " + credentialId));

        if (metadata.getSupportedCredential() != null && credential.getType() != metadata.getSupportedCredential()) {
            throw new ValidationException(String.format("Credential type mismatch. Expected: %s, got: %s", metadata.getSupportedCredential(), credential.getType()));
        }

        return decryptCredentials(credential);
    }

    /**
     * Підтримує два формати вхідних даних від UI:
     *
     * Новий (n8n-стиль):
     *   "inputItems": [ { "json": { "key": "val" }, "binary": {} }, ... ]
     *
     * Legacy (старий формат):
     *   "inputData": [ { "key": "val" }, ... ]
     *
     * Якщо нічого не передано — один порожній item (тригер-нода не потребує вхідних даних).
     */
    @SuppressWarnings("unchecked")
    private List<WorkflowItem> resolveInputItems(Map<String, Object> testData) {

        // Новий формат
        if (testData.containsKey("inputItems")) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) testData.get("inputItems");

            return raw.stream()
                    .map(entry -> {
                        Map<String, Object> json = (Map<String, Object>) entry.getOrDefault("json", Map.of());
                        Map<String, Object> binary = (Map<String, Object>) entry.getOrDefault("binary", Map.of());
                        return new WorkflowItem(json, binary);
                    })
                    .collect(Collectors.toList());
        }

        // Legacy формат
        if (testData.containsKey("inputData")) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) testData.get("inputData");
            return raw.stream()
                    .map(WorkflowItem::of)
                    .collect(Collectors.toList());
        }

        // Default — один порожній item
        return WorkflowItem.single(new HashMap<>());
    }

    private NodeExecutor createExecutor(NodeDiscriminator discriminator) throws Exception {
        Class<? extends NodeExecutor> cls = nodeRegistry
                .getExecutorClass(discriminator)
                .orElseThrow(() -> new IllegalStateException(
                        "No executor found for node: " + discriminator));

        NodeExecutor executor = cls.getDeclaredConstructor().newInstance();
        beanFactory.autowireBean(executor); // ін'єктуємо Spring-залежності
        return executor;
    }

    private Map<String, Object> decryptCredentials(Credential credential) {
        try {
            String decrypted = encryption.decrypt(credential.getEncryptedData());
            return MapSerializer.deserializeFromJson(decrypted);
        } catch (Exception e) {
            log.error("Failed to decrypt credentials: {}", e.getMessage());
            throw new ValidationException("Failed to decrypt credentials: " + e.getMessage());
        }
    }

    private Long extractCredentialId(Map<String, Object> testData) {
        Object raw = testData.get("credentialId");
        if (raw instanceof Number n)   {
            return n.longValue();
        }
        if (raw instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Форматує результат для відповіді UI.
     * outputs[0] = main port items (або true branch для IF).
     * outputs[1] = false branch для IF, тощо.
     */
    private Map<String, Object> formatTestResult(NodeResult result, NodeDiscriminator discriminator) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isSuccess());
        response.put("nodeType", discriminator.name());
        response.put("executionTimeMs", result.getExecutionTimeMs());

        if (result.isSuccess()) {
            // Конвертуємо WorkflowItem → plain Map для зручності UI
            if (result.getOutputs() != null) {
                List<List<Map<String, Object>>> outputsForUi = result.getOutputs().stream()
                        .map(port -> port.stream()
                                .map(WorkflowItem::json)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());

                response.put("outputs", outputsForUi);

                // Shortcut: перший порт як "data" для простих нод
                response.put("data", outputsForUi.isEmpty() ? List.of() : outputsForUi.get(0));
            }
            response.put("message", "Node executed successfully");
        } else {
            response.put("error", result.getError());
            response.put("errorStack", result.getErrorStack());
            response.put("message", "Node execution failed");
        }

        return response;
    }

    private Map<String, Object> createErrorResult(String message, String errorType) {
        return Map.of(
                "success",   false,
                "error",     message,
                "errorType", errorType,
                "message",   "Test execution failed"
        );
    }
}
