package com.workflow.sociallabs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.domain.converter.NodeParametersConverter;
import com.workflow.sociallabs.domain.entity.*;
import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.domain.repository.ConnectionRepository;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.domain.repository.NodeRepository;
import com.workflow.sociallabs.domain.repository.WorkflowRepository;
import com.workflow.sociallabs.dto.mapper.WorkflowMapper;
import com.workflow.sociallabs.dto.request.ConnectionRequest;
import com.workflow.sociallabs.dto.request.NodeRequest;
import com.workflow.sociallabs.dto.request.WorkflowRequest;
import com.workflow.sociallabs.dto.response.ExecutionResponse;
import com.workflow.sociallabs.dto.response.WorkflowResponse;
import com.workflow.sociallabs.exception.ResourceNotFoundException;
import com.workflow.sociallabs.exception.ValidationException;
import com.workflow.sociallabs.execution.engine.WorkflowEngine;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.NodeRegistry;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з Workflows
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final NodeRegistry nodeRegistry = NodeRegistry.getInstance();
    private final NodeRepository nodeInstanceRepository;
    private final ConnectionRepository connectionRepository;
    private final CredentialRepository credentialRepository;
    private final WorkflowMapper mapper;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;

    /**
     * Отримати всі workflows
     */
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getAllWorkflows() {
        return workflowRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Отримати workflow за ID
     */
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
        return mapper.toResponse(workflow);
    }

    /**
     * Створити новий workflow
     */
    @Transactional
    public WorkflowResponse createWorkflow(WorkflowRequest request) {
        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(false)
                .build();

        // Зберегти workflow спочатку
        workflow = workflowRepository.save(workflow);

        // Додати ноди якщо є
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            for (NodeRequest nodeReq : request.getNodes()) {
                addNodeToWorkflow(workflow, nodeReq);
            }
        }

        // Додати з'єднання якщо є
        if (request.getConnections() != null && !request.getConnections().isEmpty()) {
            for (ConnectionRequest connReq : request.getConnections()) {
                addConnectionToWorkflow(workflow, connReq);
            }
        }

        workflow = workflowRepository.save(workflow);
        log.info("Created workflow: {} (id={})", workflow.getName(), workflow.getId());

        return mapper.toResponse(workflow);
    }

    /**
     * Оновити workflow
     */
    @Transactional
    public WorkflowResponse updateWorkflow(Long id, WorkflowRequest request) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));

        // Оновити основні поля
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());

        // Видалити всі старі ноди та з'єднання
        nodeInstanceRepository.deleteAll(workflow.getNodes());
        connectionRepository.deleteAll(workflow.getConnections());

        workflow.getNodes().clear();
        workflow.getConnections().clear();

        // Flush changes
        workflowRepository.flush();

        // Додати нові ноди
        if (request.getNodes() != null) {
            for (NodeRequest nodeReq : request.getNodes()) {
                addNodeToWorkflow(workflow, nodeReq);
            }
        }

        // Додати нові з'єднання
        if (request.getConnections() != null) {
            for (ConnectionRequest connReq : request.getConnections()) {
                addConnectionToWorkflow(workflow, connReq);
            }
        }

        workflow = workflowRepository.save(workflow);
        log.info("Updated workflow: {} (id={})", workflow.getName(), workflow.getId());

        return mapper.toResponse(workflow);
    }

    /**
     * Видалити workflow
     */
    @Transactional
    public void deleteWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));

        if (workflow.getActive()) {
            throw new ValidationException("Cannot delete active workflow. Deactivate it first.");
        }

        workflowRepository.delete(workflow);
        log.info("Deleted workflow: {} (id={})", workflow.getName(), workflow.getId());
    }

    /**
     * Активувати/деактивувати workflow
     */
    @Transactional
    public WorkflowResponse toggleWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));

        workflow.setActive(!workflow.getActive());

        if (workflow.getActive()) {
            workflowEngine.activateWorkflow(workflow);
            log.info("Activated workflow: {}", workflow.getName());
        } else {
            workflowEngine.deactivateWorkflow(workflow);
            log.info("Deactivated workflow: {}", workflow.getName());
        }

        workflow = workflowRepository.save(workflow);
        return mapper.toResponse(workflow);
    }

    /**
     * Виконати workflow
     */
    @Transactional
    public ExecutionResponse executeWorkflow(Long id, Map<String, Object> triggerData) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));

        log.info("Manually executing workflow: {} (id={})", workflow.getName(), workflow.getId());

        WorkflowExecution execution = workflowEngine.executeWorkflow(
                workflow,
                triggerData != null ? triggerData : new HashMap<>(),
                "manual"
        );

        return mapExecutionToResponse(execution);
    }

    /**
     * Отримати історію виконань
     */
    @Transactional(readOnly = true)
    public List<ExecutionResponse> getWorkflowExecutions(Long workflowId, int page, int size) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));

        return workflow.getExecutions().stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapExecutionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Додати ноду до workflow
     * Використовує NodeRegistry для валідації типів нод
     */
    private void addNodeToWorkflow(Workflow workflow, NodeRequest request) {
        // Валідація discriminator
        if (request.getDiscriminator() == null) {
            throw new ValidationException("Node discriminator is required");
        }

        NodeDiscriminator discriminator = request.getDiscriminator();

        // Перевірити чи існує нода в registry
        NodeRegistry.NodeMetadata metadata = nodeRegistry.getAllNodes().stream()
                .filter(n -> n.getDiscriminator() == discriminator)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Node type not registered: " + discriminator +
                                ". Available nodes: " + nodeRegistry.getAllNodes().stream()
                                .map(n -> n.getDiscriminator().name())
                                .collect(Collectors.joining(", "))
                ));

        // Обробити credential якщо є
        Credential credential = null;
        if (request.getCredentialId() != null) {
            credential = credentialRepository.findById(request.getCredentialId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Credential not found: " + request.getCredentialId()
                    ));

            // Перевірити чи credential підходить для цього типу ноди
            if (metadata.getSupportedCredential() != null &&
                    credential.getType() != metadata.getSupportedCredential()) {
                throw new ValidationException(String.format(
                        "Credential type mismatch for node '%s'. Expected: %s, got: %s",
                        discriminator,
                        metadata.getSupportedCredential(),
                        credential.getType()
                ));
            }
        } else if (metadata.getSupportedCredential() != null) {
            // Попередження якщо credential обов'язковий але не передано
            log.warn("Node {} requires credential of type {}, but none provided",
                    discriminator, metadata.getSupportedCredential());
        }

        // Конвертувати параметри в NodeParameters
        NodeParameters parameters = convertToNodeParameters(
                request.getParameters(),
                discriminator
        );

        // Валідувати параметри
        validateNodeParameters(parameters, discriminator);

        // Згенерувати UUID якщо не передано
        String nodeId = request.getId() != null && !request.getId().trim().isEmpty()
                ? request.getId()
                : UUID.randomUUID().toString();

        // Визначити ім'я ноди
        String nodeName = request.getName();

        // Створити NodeInstance
        Node nodeInstance = Node.builder()
                .nodeId(nodeId)
                .workflow(workflow)
                .type(metadata.getType())
                .discriminator(discriminator)
                .name(nodeName)
                .positionX(request.getPosition() != null ? request.getPosition().getX() : 0)
                .positionY(request.getPosition() != null ? request.getPosition().getY() : 0)
                .parameters(parameters)
                .credential(credential)
                .disabled(request.getDisabled() != null ? request.getDisabled() : false)
                .notes(request.getNotes())
                .build();

        workflow.addNode(nodeInstance);

        log.debug("Added node '{}' ({}) to workflow '{}' at position ({}, {})",
                nodeName, discriminator, workflow.getName(),
                nodeInstance.getPositionX(), nodeInstance.getPositionY());
    }

    /**
     * Конвертувати Map параметрів в NodeParameters з типом
     */
    private NodeParameters convertToNodeParameters(
            Map<String, Object> rawParameters,
            NodeDiscriminator discriminator
    ) {
        if (rawParameters == null || rawParameters.isEmpty()) {
            return NodeParameters.withType(discriminator.value, new HashMap<>());
        }

        // Створити копію параметрів
        Map<String, Object> params = new HashMap<>(rawParameters);

        // Додати @type для Jackson polymorphism
        params.put("@type", discriminator.value);

        return NodeParameters.withType(discriminator.value, params);
    }

    /**
     * Валідувати параметри ноди через TypedNodeParameters
     */
    private void validateNodeParameters(NodeParameters parameters, NodeDiscriminator discriminator) {
        try {
            // Спробувати конвертувати в типізовані параметри та валідувати
            TypedNodeParameters typedParams = objectMapper.convertValue(
                    parameters.getValues(),
                    TypedNodeParameters.class
            );

            if (typedParams != null) {
                typedParams.validate();
                log.debug("Node parameters validated successfully for {}", discriminator);
            } else {
                log.debug("No typed parameters found for {} - skipping validation", discriminator);
            }
        } catch (IllegalArgumentException e) {
            // Validation error - кидаємо як ValidationException
            throw new ValidationException(
                    "Invalid parameters for node " + discriminator + ": " + e.getMessage()
            );
        } catch (Exception e) {
            // Інші помилки - логуємо warning але не блокуємо
            log.warn("Could not validate typed parameters for {}: {}",
                    discriminator, e.getMessage());
        }
    }

    /**
     * Додати з'єднання до workflow
     */
    private void addConnectionToWorkflow(Workflow workflow, ConnectionRequest request) {
        Node sourceNode = workflow.getNodes().stream()
                .filter(n -> n.getNodeId().equals(request.getSourceNodeId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source node not found: " + request.getSourceNodeId()
                ));

        Node targetNode = workflow.getNodes().stream()
                .filter(n -> n.getNodeId().equals(request.getTargetNodeId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target node not found: " + request.getTargetNodeId()
                ));

        Connection connection = Connection.builder()
                .workflow(workflow)
                .sourceNode(sourceNode)
                .sourceOutput(request.getSourceOutput() != null ? request.getSourceOutput() : "main")
                .targetNode(targetNode)
                .targetInput(request.getTargetInput() != null ? request.getTargetInput() : "main")
                .build();

        workflow.addConnection(connection);

        log.debug("Added connection from '{}' to '{}'",
                sourceNode.getName(), targetNode.getName());
    }

    /**
     * Конвертація Execution в Response
     */
    private ExecutionResponse mapExecutionToResponse(WorkflowExecution execution) {
        return ExecutionResponse.builder()
                .id(execution.getId())
                .workflowId(execution.getWorkflow().getId())
                .status(execution.getStatus().name())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .durationMs(execution.getFinishedAt() != null ?
                        java.time.Duration.between(execution.getStartedAt(),
                                execution.getFinishedAt()).toMillis() : null)
                .errorMessage(execution.getErrorMessage())
                .logs(execution.getLogs().stream()
                        .map(this::mapLogToDto)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Конвертація ExecutionLog в DTO
     */
    private ExecutionResponse.NodeExecutionLogDto mapLogToDto(ExecutionLog log) {
        return ExecutionResponse.NodeExecutionLogDto.builder()
                .nodeId(log.getNode().getNodeId())
                .nodeName(log.getNode().getName())
                .status(log.getStatus().name())
                .executedAt(log.getExecutedAt())
                .durationMs(log.getDurationMs())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}