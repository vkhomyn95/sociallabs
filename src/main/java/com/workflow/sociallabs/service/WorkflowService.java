package com.workflow.sociallabs.service;

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
import com.workflow.sociallabs.execution.engine.WorkflowExecutionService;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.NodeRegistry;
import com.workflow.sociallabs.node.core.WorkflowExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NodeRepository nodeRepository;
    private final ConnectionRepository connectionRepository;
    private final CredentialRepository credentialRepository;
    private final WorkflowMapper mapper;
    private final WorkflowExecutionService workflowExecutionService; // ← замість WorkflowEngine
    private final WorkflowExecutionCache executionCache;             // ← для invalidate
    private final NodeService nodeService;

    // NodeRegistry — singleton, не через Spring DI щоб не дублювати
    private final NodeRegistry nodeRegistry = NodeRegistry.getInstance();

    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflows() {
        return workflowRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long workflowId) {
        return mapper.toResponse(findWorkflow(workflowId));
    }

    @Transactional
    public WorkflowResponse createWorkflow(WorkflowRequest request) {
        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(false)
                .build();

        workflow = workflowRepository.save(workflow);

        applyNodesAndConnections(workflow, request);

        workflow = workflowRepository.save(workflow);
        log.info("Created workflow: {} (id={})", workflow.getName(), workflow.getId());
        return mapper.toResponse(workflow);
    }

    @Transactional
    public WorkflowResponse updateWorkflow(Long workflowId, WorkflowRequest request) {
        Workflow workflow = findWorkflow(workflowId);

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());

        // 1. Спочатку обриваємо зовнішні ключі — connections видаляємо першими
        //    щоб уникнути constraint violation при видаленні nodes
        connectionRepository.deleteAllByWorkflowId(workflowId);
        nodeRepository.deleteAllByWorkflowId(workflowId);
        // 2. Очищаємо колекції в entity
        workflow.getNodes().clear();
        workflow.getConnections().clear();
        // 3. Flush щоб Hibernate синхронізував стан
        workflowRepository.flush();
        // 4. Додаємо нові
        applyNodesAndConnections(workflow, request);

        workflow = workflowRepository.save(workflow);

        // Invalidate граф у кеші — він застарів
        executionCache.invalidate(workflowId);

        log.info("Updated workflow: {} (id={})", workflow.getName(), workflow.getId());
        return mapper.toResponse(workflow);
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        Workflow workflow = findWorkflow(id);

        if (workflow.getActive()) {
            throw new ValidationException("Cannot delete active workflow. Deactivate it first.");
        }

        workflowRepository.delete(workflow);
        executionCache.invalidate(id);
        log.info("Deleted workflow: {} (id={})", workflow.getName(), workflow.getId());
    }

    @Transactional
    public WorkflowResponse toggleWorkflow(Long workflowId) {
        Workflow workflow = findWorkflow(workflowId);
        workflow.setActive(!workflow.getActive());
        workflow = workflowRepository.save(workflow);

        if (workflow.getActive()) {
            // Передаємо збережену entity — граф будується у executionService
            workflowExecutionService.activateWorkflow(workflow);
            log.info("Activated workflow: {}", workflow.getName());
        } else {
            workflowExecutionService.deactivateWorkflow(workflow.getId());
            log.info("Deactivated workflow: {}", workflow.getName());
        }

        return mapper.toResponse(workflow);
    }

    /**
     * Ручний запуск workflow через UI.
     * Шукаємо перший тригер і запускаємо від нього.
     */
    @Transactional(readOnly = true)
    public ExecutionResponse executeWorkflow(Long workflowId, Map<String, Object> triggerData) {
        Workflow workflow = findWorkflow(workflowId);
        log.info("Manually executing workflow: {} (id={})", workflow.getName(), workflowId);

        // Знаходимо перший trigger node
        String triggerNodeId = workflow.getNodes().stream()
                .filter(Node::isTrigger)
                .findFirst()
                .map(Node::getNodeId)
                .orElseThrow(() -> new ValidationException("Workflow has no trigger node: " + workflowId));

        Map<String, Object> data = triggerData != null ? triggerData : Map.of();

        // Запускаємо і чекаємо результат (manual = синхронно для відповіді UI)
        var result = workflowExecutionService
                .executeWorkflow(workflowId, triggerNodeId, data)
                .join();

        return mapResultToResponse(workflowId, result);
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> getWorkflowExecutions(Long workflowId, int page, int size) {
        Workflow workflow = findWorkflow(workflowId);

        return workflow.getExecutions().stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapExecutionToResponse)
                .collect(Collectors.toList());
    }

    private Workflow findWorkflow(Long workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
    }

    private void applyNodesAndConnections(Workflow workflow, WorkflowRequest request) {
        if (request.getNodes() != null) {
            request.getNodes().forEach(n -> addNodeToWorkflow(workflow, n));
        }
        if (request.getConnections() != null) {
            request.getConnections().forEach(c -> addConnectionToWorkflow(workflow, c));
        }
    }

    private void addNodeToWorkflow(Workflow workflow, NodeRequest request) {
        if (request.getDiscriminator() == null) {
            throw new ValidationException("Node discriminator is required");
        }

        NodeDiscriminator discriminator = request.getDiscriminator();

        NodeRegistry.NodeMetadata metadata = nodeRegistry.getAllNodes().stream()
                .filter(n -> n.getDiscriminator() == discriminator)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Node type not registered: " + discriminator));

        Credential credential = resolveCredential(request, metadata, discriminator);

        NodeParameters parameters = nodeService.convertToNodeParameters(request.getParameters(), discriminator);
        nodeService.validateNodeParameters(parameters, discriminator);

        String nodeId = (request.getNodeId() != null && !request.getNodeId().isBlank())
                ? request.getNodeId()
                : UUID.randomUUID().toString();

        Node node = Node.builder()
                .nodeId(nodeId)
                .workflow(workflow)
                .type(metadata.getType())
                .discriminator(discriminator)
                .name(request.getName())
                .positionX(request.getPosition() != null ? request.getPosition().getX() : 0)
                .positionY(request.getPosition() != null ? request.getPosition().getY() : 0)
                .parameters(parameters)
                .credential(credential)
                .disabled(request.getDisabled() != null ? request.getDisabled() : false)
                .notes(request.getNotes())
                .build();

        workflow.addNode(node);
        log.debug("Added node '{}' ({}) to workflow '{}'", node.getName(), discriminator, workflow.getName());
    }

    private Credential resolveCredential(
            NodeRequest request,
            NodeRegistry.NodeMetadata metadata,
            NodeDiscriminator discriminator) {

        if (request.getCredentialId() == null) {
            if (metadata.getSupportedCredential() != null) {
                log.warn("Node {} requires credential type {} but none provided", discriminator, metadata.getSupportedCredential());
            }
            return null;
        }

        Credential credential = credentialRepository.findById(request.getCredentialId())
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found: " + request.getCredentialId()));

        if (metadata.getSupportedCredential() != null && credential.getType() != metadata.getSupportedCredential()) {
            throw new ValidationException(
                    String.format(
                            "Credential type mismatch for node '%s'. Expected: %s, got: %s",
                            discriminator, metadata.getSupportedCredential(), credential.getType()
                    )
            );
        }

        return credential;
    }

    private void addConnectionToWorkflow(Workflow workflow, ConnectionRequest request) {
        Node sourceNode = findNodeInWorkflow(workflow, request.getSourceNodeId());
        Node targetNode = findNodeInWorkflow(workflow, request.getTargetNodeId());

        Connection connection = Connection.builder()
                .workflow(workflow)
                .sourceNode(sourceNode)
                .sourceOutputIndex(request.getSourceOutputIndex() != null ? request.getSourceOutputIndex() : 0)   // int, default = port 0
                .targetNode(targetNode)
                .targetInputIndex(request.getTargetInputIndex() != null ? request.getTargetInputIndex() : 0)    // int, default = port 0
                .type(request.getType())
                .build();

        workflow.addConnection(connection);
        log.debug(
                "Added connection: '{}' [{}] → '{}' [{}]",
                sourceNode.getName(), connection.getSourceOutputIndex(), targetNode.getName(), connection.getTargetInputIndex()
        );
    }

    private Node findNodeInWorkflow(Workflow workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Node not found in workflow: " + nodeId));
    }

    private ExecutionResponse mapResultToResponse(Long workflowId, WorkflowExecutionResult result) {
        return ExecutionResponse.builder()
                .workflowId(workflowId)
                .status(result.isSuccess() ? "SUCCESS" : "ERROR")
                .errorMessage(result.isSuccess() ? null : result.getError())
                .build();
    }

    private ExecutionResponse mapExecutionToResponse(WorkflowExecution execution) {
        return ExecutionResponse.builder()
                .id(execution.getId())
                .workflowId(execution.getWorkflow().getId())
                .status(execution.getStatus().name())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .durationMs(execution.getFinishedAt() != null
                        ? java.time.Duration.between(
                        execution.getStartedAt(),
                        execution.getFinishedAt()).toMillis()
                        : null)
                .errorMessage(execution.getErrorMessage())
                .logs(execution.getLogs().stream()
                        .map(this::mapLogToDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private ExecutionResponse.NodeExecutionLogDto mapLogToDto(ExecutionLog logEntry) {
        return ExecutionResponse.NodeExecutionLogDto.builder()
                .nodeId(logEntry.getNode().getNodeId())
                .nodeName(logEntry.getNode().getName())
                .status(logEntry.getStatus().name())
                .executedAt(logEntry.getExecutedAt())
                .durationMs(logEntry.getDurationMs())
                .errorMessage(logEntry.getErrorMessage())
                .build();
    }
}