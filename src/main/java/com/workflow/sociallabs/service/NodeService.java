package com.workflow.sociallabs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.domain.entity.Credential;
import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.enums.NodeCategory;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.exception.ResourceNotFoundException;
import com.workflow.sociallabs.exception.ValidationException;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.NodeRegistry;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import com.workflow.sociallabs.security.CredentialEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

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



    /**
     * Валідація параметрів ноди
     */
    public Map<String, Object> validateNodeParameters(String nodeType, Map<String, Object> parameters) {
//        NodeDefinition definition = nodeRegistry
//                .getNodeDefinition(nodeType)
//                .orElseThrow(() -> new ResourceNotFoundException("Node type not found: " + nodeType));
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("valid", true);
//        List<String> errors = new ArrayList<>();
//
//        // Валідація кожного параметра
//        for (NodeParameter<?> param : definition.getParameters()) {
//            Object value = parameters.get(param.getName());
//
//            if (param.isRequired() && value == null) {
//                errors.add(param.getName() + " is required");
//                result.put("valid", false);
//            }
//
//            // Додаткова валідація через validate метод
//            // Simplified version
//        }
//
//        result.put("errors", errors);
//        return result;

        return null;
    }

    /**
     * Тестове виконання ноди
     * Не зберігає нічого в базу, просто виконує логіку ноди
     */
    public Map<String, Object> testNodeExecution(
            NodeDiscriminator discriminator,
            Map<String, Object> testData
    ) {
        log.info("Testing node execution: {}", discriminator);

        try {
            // Перевірити чи існує нода в registry
            NodeRegistry.NodeMetadata metadata = nodeRegistry.getAllNodes().stream()
                    .filter(n -> n.getDiscriminator() == discriminator)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Node type not registered: " + discriminator
                    ));

            // Отримати параметри з testData
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) testData.get("parameters");
            if (parameters == null) {
                parameters = new HashMap<>();
            }

            // Отримати credentialId якщо є
            Long credentialId = extractCredentialId(testData);

            // Завантажити credentials якщо потрібні
            Map<String, Object> credentials = new HashMap<>();
            if (credentialId != null) {
                Credential credential = credentialRepository.findById(credentialId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Credential not found: " + credentialId
                        ));

                // Перевірити тип credential
                if (metadata.getSupportedCredential() != null &&
                        credential.getType() != metadata.getSupportedCredential()) {
                    throw new ValidationException(String.format(
                            "Credential type mismatch. Expected: %s, got: %s",
                            metadata.getSupportedCredential(),
                            credential.getType()
                    ));
                }

                // Дешифрувати credentials
                credentials = decryptCredentials(credential);
            }

            // Перевірити чи credentials обов'язкові
            if (credentials.isEmpty() && metadata.getSupportedCredential() != null) {
                log.warn("Node {} requires credentials but none provided", discriminator);
            }

            // Конвертувати параметри в NodeParameters
            NodeParameters nodeParameters = convertToNodeParameters(parameters, discriminator);

            // Створити тимчасову Node entity для контексту
            Node tempNode = createTempNode(discriminator, metadata, nodeParameters);

            // Валідувати параметри через TypedNodeParameters
            TypedNodeParameters typedParams = validateAndConvertParameters(
                    nodeParameters,
                    discriminator
            );

            // Отримати вхідні дані для тесту
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> inputData = (List<Map<String, Object>>)
                    testData.getOrDefault("inputData", Collections.singletonList(new HashMap<>()));

            // Створити контекст виконання
            ExecutionContext context = ExecutionContext.builder()
                    .executionId(-1L) // Тестовий execution ID
                    .workflowId(-1L)  // Тестовий workflow ID
                    .nodeId(UUID.randomUUID().toString())
                    .node(tempNode)
                    .inputData(inputData)
                    .credentials(credentials)
                    .typedParameters(typedParams)
                    .workflowVariables(new HashMap<>())
                    .workflowData(new HashMap<>())
                    .startTime(Instant.now())
                    .build();

            // Створити executor та виконати
            NodeExecutor executor = createExecutor(discriminator);
            NodeResult result = executor.execute(context);

            // Форматувати результат
            return formatTestResult(result, discriminator);

        } catch (ValidationException | ResourceNotFoundException e) {
            log.error("Test execution validation failed: {}", e.getMessage());
            return createErrorResult(e.getMessage(), "VALIDATION_ERROR");
        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            return createErrorResult(e.getMessage(), "EXECUTION_ERROR");
        }
    }

    /**
     * Створити executor для ноди
     */
    private NodeExecutor createExecutor(NodeDiscriminator discriminator) throws Exception {
        Class<? extends NodeExecutor> executorClass = nodeRegistry
                .getExecutorClass(discriminator)
                .orElseThrow(() -> new IllegalStateException(
                        "No executor found for node: " + discriminator
                ));

        return executorClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Конвертувати Map параметрів в NodeParameters
     */
    private NodeParameters convertToNodeParameters(
            Map<String, Object> rawParameters,
            NodeDiscriminator discriminator
    ) {
        if (rawParameters == null || rawParameters.isEmpty()) {
            return NodeParameters.withType(discriminator.value, new HashMap<>());
        }

        Map<String, Object> params = new HashMap<>(rawParameters);
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

    /**
     * Створити тимчасову Node entity для тесту
     */
    private Node createTempNode(
            NodeDiscriminator discriminator,
            NodeRegistry.NodeMetadata metadata,
            NodeParameters parameters
    ) {
        return Node.builder()
                .nodeId(UUID.randomUUID().toString())
                .type(metadata.getType())
                .discriminator(discriminator)
                .name("Test Node - " + discriminator.name())
                .parameters(parameters)
                .disabled(false)
                .build();
    }

    /**
     * Дешифрувати credentials
     */
    private Map<String, Object> decryptCredentials(Credential credential) {
        try {
            String decryptedData = encryption.decrypt(credential.getEncryptedData());
            return objectMapper.readValue(decryptedData, Map.class);
        } catch (Exception e) {
            log.error("Failed to decrypt credentials: {}", e.getMessage());
            throw new ValidationException("Failed to decrypt credentials: " + e.getMessage());
        }
    }

    /**
     * Витягнути credentialId з testData
     */
    private Long extractCredentialId(Map<String, Object> testData) {
        Object credId = testData.get("credentialId");
        if (credId == null) {
            return null;
        }

        if (credId instanceof Number) {
            return ((Number) credId).longValue();
        }

        if (credId instanceof String) {
            try {
                return Long.parseLong((String) credId);
            } catch (NumberFormatException e) {
                log.warn("Invalid credentialId format: {}", credId);
                return null;
            }
        }

        return null;
    }

    /**
     * Форматувати результат тесту
     */
    private Map<String, Object> formatTestResult(NodeResult result, NodeDiscriminator discriminator) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("nodeType", discriminator.name());
        response.put("executionTimeMs", result.getExecutionTimeMs());

        if (result.isSuccess()) {
            response.put("data", result.getData());
            response.put("metadata", result.getMetadata());
            response.put("message", "Node executed successfully");
        } else {
            response.put("error", result.getError());
            response.put("errorStack", result.getErrorStack());
            response.put("message", "Node execution failed");
        }

        return response;
    }

    /**
     * Створити результат з помилкою
     */
    private Map<String, Object> createErrorResult(String message, String errorType) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("errorType", errorType);
        response.put("message", "Test execution failed");
        return response;
    }
}
