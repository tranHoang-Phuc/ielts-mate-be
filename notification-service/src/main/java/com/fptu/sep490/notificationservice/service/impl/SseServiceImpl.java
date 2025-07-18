package com.fptu.sep490.notificationservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.notificationservice.service.SseService;
import com.fptu.sep490.notificationservice.viewmodel.event.BaseMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service

public class SseServiceImpl implements SseService {

    private final Map<UUID, SseEmitter> clientEmitters = new ConcurrentHashMap<>();


    private final ObjectMapper objectMapper;

    public SseServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter subscribe(UUID clientId) {
        SseEmitter emitter = new SseEmitter(0L);
        clientEmitters.put(clientId, emitter);

        emitter.onCompletion(() -> clientEmitters.remove(clientId));
        emitter.onTimeout(()    -> clientEmitters.remove(clientId));
        emitter.onError((e)    -> clientEmitters.remove(clientId));

        return emitter;
    }

    @Override
    public void sendMessage(UUID clientId, String message, String status) {
        SseEmitter emitter = clientEmitters.get(clientId);
        BaseMessageResponse response = BaseMessageResponse.builder()
                .message(message)
                .status(status)
                .build();
        log.info("Sending message to client {}: {}", clientId, response);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(response)));
            } catch (IOException e) {
                clientEmitters.remove(clientId);
            }
        }
    }
}
