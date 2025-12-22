package com.workflow.sociallabs.controller;

import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/telegram/auth")
@RequiredArgsConstructor
public class CredentialTelegramController {

    private final TelegramClientAuthService authService;

    /**
     * Початок процесу аутентифікації
     */
    @PostMapping("/start")
    public Map<String, Object> startAuth(@RequestBody Map<String, String> request) {
        String apiId = request.get("apiId");
        String apiHash = request.get("apiHash");
        String phoneNumber = request.get("phoneNumber");
        String authMethod = request.getOrDefault("authMethod", "qrcode"); // qrcode or phone

        String sessionId = authService.startAuthSession(apiId, apiHash, phoneNumber, authMethod);

        return Map.of(
                "success", true,
                "sessionId", sessionId,
                "message", "Authentication session started"
        );
    }

    /**
     * SSE endpoint для отримання updates в реальному часі
     */
    @GetMapping(value = "/updates/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> getAuthUpdates(@PathVariable String sessionId) {
        return authService.getAuthUpdatesStream(sessionId)
                .timeout(Duration.ofMinutes(5))
                .onErrorResume(e -> {
                    log.error("Error in auth stream", e);
                    return Flux.just(Map.of(
                            "type", "error",
                            "message", e.getMessage()
                    ));
                });
    }

    /**
     * Відправка коду підтвердження
     */
    @PostMapping("/submit-code")
    public Map<String, Object> submitCode(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String code = request.get("code");

        authService.submitCode(sessionId, code);

        return Map.of(
                "success", true,
                "message", "Code submitted"
        );
    }

    /**
     * Відправка пароля 2FA
     */
    @PostMapping("/submit-password")
    public Map<String, Object> submitPassword(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String password = request.get("password");

        authService.submitPassword(sessionId, password);

        return Map.of(
                "success", true,
                "message", "Password submitted"
        );
    }

    /**
     * Скасування аутентифікації
     */
    @DeleteMapping("/cancel/{sessionId}")
    public Map<String, Object> cancelAuth(@PathVariable String sessionId) {
        authService.cancelAuthSession(sessionId);

        return Map.of(
                "success", true,
                "message", "Authentication cancelled"
        );
    }

    /**
     * Отримання статусу сесії
     */
    @GetMapping("/status/{sessionId}")
    public Map<String, Object> getStatus(@PathVariable String sessionId) {
        return authService.getSessionStatus(sessionId);
    }
}