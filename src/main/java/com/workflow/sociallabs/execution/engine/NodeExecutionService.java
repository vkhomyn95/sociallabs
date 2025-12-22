package com.workflow.sociallabs.execution.engine;

import com.workflow.sociallabs.domain.entity.*;
import com.workflow.sociallabs.domain.enums.ExecutionStatus;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.NodeRegistry;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.security.CredentialEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервіс для виконання окремих нод
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeExecutionService {

    private final NodeRegistry nodeRegistry;
    private final CredentialRepository credentialRepository;
    private final CredentialEncryption encryption;
    private final Map<String, Object> activeTriggers = new ConcurrentHashMap<>();

    /**
     * Виконати ноду
     */
    public NodeResult executeNode(
            Node nodeInstance,
            List<Map<String, Object>> inputData,
            Map<String, Object> workflowContext,
            WorkflowExecution execution
    ) {
        Instant startTime = Instant.now();

        try {
            // Отримати executor для node
            NodeExecutor executor = createExecutor(nodeInstance.getDiscriminator());

            // Підготувати контекст виконання
            ExecutionContext context = buildExecutionContext(
                    nodeInstance,
                    inputData,
                    workflowContext,
                    execution
            );

            // Виконати ноду
            NodeResult result = executor.execute(context);

            // Логувати результат
            logNodeExecution(nodeInstance, execution, ExecutionStatus.SUCCESS,
                    startTime, result, null);

            return result;

        } catch (Exception e) {
            log.error("Node execution failed: {} - {}", nodeInstance.getName(), e.getMessage(), e);

            NodeResult errorResult = NodeResult.error(e.getMessage(), getStackTrace(e));

            logNodeExecution(nodeInstance, execution, ExecutionStatus.ERROR,
                    startTime, errorResult, e.getMessage());

            return errorResult;
        }
    }

    /**
     * Створити executor для ноди
     */
    private NodeExecutor createExecutor(NodeDiscriminator discriminator) throws Exception {
        Class<? extends NodeExecutor> executorClass = nodeRegistry
                .getExecutorClass(discriminator)
                .orElseThrow(() -> new IllegalStateException("No executor found for node discriminator: " + discriminator));

        return executorClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Побудувати контекст виконання
     */
    private ExecutionContext buildExecutionContext(
            Node nodeInstance,
            List<Map<String, Object>> inputData,
            Map<String, Object> workflowContext,
            WorkflowExecution execution
    ) {
        // Парсити параметри з JSON
//        Map<String, Object> parameters = parseJsonToMap(nodeInstance.getParameters());
//
//        // Завантажити credentials якщо потрібні
//        Map<String, Object> credentials = new HashMap<>();
//        if (nodeInstance.getCredential() != null) {
//            credentials = loadCredentials(nodeInstance.getCredential());
//        }
//
//        return ExecutionContext.builder()
//                .executionId(execution.getId())
//                .nodeId(nodeInstance.getNodeId())
//                .parameters(parameters)
//                .credentials(credentials)
//                .inputData(inputData)
//                .workflowData(workflowContext)
//                .startTime(Instant.now())
//                .build();
        return null;
    }

    /**
     * Завантажити credentials з дешифруванням
     */
    private Map<String, Object> loadCredentials(Credential credential) {
        String decryptedData = encryption.decrypt(credential.getEncryptedData());
        return parseJsonToMap(decryptedData);
    }

    /**
     * Логувати виконання ноди
     */
    private void logNodeExecution(
            Node nodeInstance,
            WorkflowExecution execution,
            ExecutionStatus status,
            Instant startTime,
            NodeResult result,
            String errorMessage
    ) {
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();

        ExecutionLog log = ExecutionLog.builder()
                .execution(execution)
                .node(nodeInstance)
                .status(status)
                .executedAt(LocalDateTime.now())
                .durationMs(durationMs)
                .inputData("{}") // Simplified
                .outputData(result.isSuccess() ? "{}" : null)
                .errorMessage(errorMessage)
                .errorStack(result.getErrorStack())
                .build();

        execution.addLog(log);
    }

    /**
     * Активувати тригер
     */
    public void activateTrigger(Node triggerNode, Workflow workflow) throws Exception {
        // Simplified - має бути повна імплементація
        log.info("Activating trigger: {}", triggerNode.getName());
        activeTriggers.put(triggerNode.getNodeId(), workflow);
    }

    /**
     * Деактивувати тригер
     */
    public void deactivateTrigger(Node triggerNode) {
        log.info("Deactivating trigger: {}", triggerNode.getName());
        activeTriggers.remove(triggerNode.getNodeId());
    }

    private Map<String, Object> parseJsonToMap(String json) {
        return new HashMap<>(); // Simplified
    }

    private String getStackTrace(Exception e) {
        return e.getMessage(); // Simplified
    }
}
