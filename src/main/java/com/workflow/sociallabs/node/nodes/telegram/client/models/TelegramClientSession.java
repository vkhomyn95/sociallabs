package com.workflow.sociallabs.node.nodes.telegram.client.models;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.jni.TdApi;
import lombok.*;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramClientSession {

    String sessionId;

    int apiId;
    String apiHash;
    String phoneNumber;

    TelegramClientAuthMethod authMethod;
    TelegramClientAuthStatus status;

    SimpleTelegramClient client;
    SimpleTelegramClientFactory factory;
    Sinks.Many<Map<String, Object>> updatesSink;
    TdApi.User currentUser;
}
