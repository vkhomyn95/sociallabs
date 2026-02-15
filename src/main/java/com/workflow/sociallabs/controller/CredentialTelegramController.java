package com.workflow.sociallabs.controller;

import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientService;
import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientAuthMethod;
import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientCredentialKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/telegram-client-credentials")
@RequiredArgsConstructor
public class CredentialTelegramController {

    private final TelegramClientService telegramClientService;

    /**8
     * Початок процесу аутентифікації
     */
    @PostMapping("/start")
    public Map<String, Object> startAuthSession(@RequestBody Map<String, Object> request) {
        String apiId = (String) request.get(TelegramClientCredentialKeys.API_ID);
        String apiHash = (String) request.get(TelegramClientCredentialKeys.API_HASH);
        String phoneNumber = (String) request.get(TelegramClientCredentialKeys.PHONE_NUMBER);
        String authMethod = (String) request.getOrDefault(TelegramClientCredentialKeys.AUTH_METHOD, "qrcode"); // qrcode or phone

        String sessionId = telegramClientService.startAuthSession(
                apiId,
                apiHash,
                phoneNumber,
                TelegramClientAuthMethod.valueOf(authMethod)
        );

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
    public Flux<Map<String, Object>> getAuthUpdatesStream(@PathVariable String sessionId) {
        return telegramClientService.getAuthUpdatesStream(sessionId)
                .timeout(Duration.ofMinutes(5))
                .onErrorResume(e -> {
                    log.error("[{}] Error in auth telegram client stream", sessionId, e);
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
    public Map<String, Object> submitAuthCode(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String code = request.get("code");

        telegramClientService.submitAuthCode(sessionId, code);

        return Map.of(
                "success", true,
                "message", "Code submitted"
        );
    }

    /**
     * Відправка пароля 2FA
     */
    @PostMapping("/submit-password")
    public Map<String, Object> submitAuthPassword(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String password = request.get("password");

        telegramClientService.submitAuthPassword(sessionId, password);

        return Map.of(
                "success", true,
                "message", "Password submitted"
        );
    }

    /**
     * Скасування аутентифікації
     */
    @DeleteMapping("/cancel/{sessionId}")
    public Map<String, Object> cancelAuthSession(@PathVariable String sessionId) {
        telegramClientService.cancelAuthSession(sessionId);

        return Map.of(
                "success", true,
                "message", "Authentication cancelled"
        );
    }

    /**
     * Отримання статусу сесії
     */
    @GetMapping("/status/{sessionId}")
    public Map<String, Object> getSessionStatus(@PathVariable String sessionId) {
        return telegramClientService.getSessionStatus(sessionId);
    }
}