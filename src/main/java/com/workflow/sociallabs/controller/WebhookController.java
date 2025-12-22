package com.workflow.sociallabs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller для Webhooks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebhookController {

//    private final WebhookService webhookService;
//
//    /**
//     * Обробка webhook запиту
//     * URL format: /api/v1/webhooks/{workflowId}/{webhookPath}
//     */
//    @PostMapping("/{workflowId}/**")
//    public ResponseEntity<Map<String, Object>> handleWebhook(
//            @PathVariable Long workflowId,
//            @RequestBody(required = false) Map<String, Object> body,
//            @RequestHeader Map<String, String> headers,
//            @RequestParam Map<String, String> queryParams
//    ) {
//        log.info("POST /api/v1/webhooks/{} - Handling webhook", workflowId);
//
//        Map<String, Object> webhookData = new HashMap<>();
//        webhookData.put("body", body);
//        webhookData.put("headers", headers);
//        webhookData.put("query", queryParams);
//
//        Map<String, Object> result = webhookService.processWebhook(workflowId, webhookData);
//        return ResponseEntity.ok(result);
//    }
//
//    /**
//     * GET webhook (для деяких сервісів типу Telegram)
//     */
//    @GetMapping("/{workflowId}/**")
//    public ResponseEntity<Map<String, Object>> handleWebhookGet(
//            @PathVariable Long workflowId,
//            @RequestHeader Map<String, String> headers,
//            @RequestParam Map<String, String> queryParams
//    ) {
//        log.info("GET /api/v1/webhooks/{} - Handling GET webhook", workflowId);
//
//        Map<String, Object> webhookData = new HashMap<>();
//        webhookData.put("headers", headers);
//        webhookData.put("query", queryParams);
//
//        Map<String, Object> result = webhookService.processWebhook(workflowId, webhookData);
//        return ResponseEntity.ok(result);
//    }
//
//    /**
//     * Отримати URL webhook для workflow
//     */
//    @GetMapping("/{workflowId}/url")
//    public ResponseEntity<Map<String, String>> getWebhookUrl(@PathVariable Long workflowId) {
//        log.info("GET /api/v1/webhooks/{}/url - Getting webhook URL", workflowId);
//        String url = webhookService.getWebhookUrl(workflowId);
//        return ResponseEntity.ok(Map.of("webhookUrl", url));
//    }
}
