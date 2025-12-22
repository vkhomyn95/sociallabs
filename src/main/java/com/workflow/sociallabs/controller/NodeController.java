package com.workflow.sociallabs.controller;

import com.workflow.sociallabs.domain.enums.NodeCategory;
import com.workflow.sociallabs.domain.enums.NodeType;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.NodeRegistry;
import com.workflow.sociallabs.service.NodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Контролер для роботи з доступними nodes та інформацію про налаштовані параметри
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    /**
     * Отримати всі доступні node з фільтрацією та пагінацією
     *
     * @param type фільтр за типом node (TRIGGER, ACTION, TRANSFORM)
     * @param category фільтр за категорією (COMMUNICATION, DATA, etc.)
     * @param pageable параметри пагінації
     */
    @GetMapping
    public ResponseEntity<List<NodeRegistry.NodeMetadata>> getAvailableNodes(
            @RequestParam(required = false) NodeType type,
            @RequestParam(required = false) NodeCategory category,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        log.info("GET /api/v1/nodes - type: {}, category: {}, page: {}", type, category, pageable.getPageNumber());

        List<NodeRegistry.NodeMetadata> response = nodeService.getAvailableNodes(type, category, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Валідація параметрів ноди
     */
    @PostMapping("/{nodeType}/validate")
    public ResponseEntity<Map<String, Object>> validateNodeParameters(
            @PathVariable String nodeType,
            @RequestBody Map<String, Object> parameters
    ) {
        log.info("POST /api/v1/nodes/{}/validate - Validating parameters", nodeType);
        Map<String, Object> result = nodeService.validateNodeParameters(nodeType, parameters);
        return ResponseEntity.ok(result);
    }

    /**
     * Тестувати виконання ноди
     */
    @PostMapping("/{discriminator}/test")
    public ResponseEntity<Map<String, Object>> testNode(
            @PathVariable NodeDiscriminator discriminator,
            @RequestBody Map<String, Object> testData
    ) {
        log.info("POST /api/v1/nodes/{}/test - Testing node execution", discriminator);

        Map<String, Object> result = nodeService.testNodeExecution(discriminator, testData);

        return ResponseEntity.ok(result);
    }
}
