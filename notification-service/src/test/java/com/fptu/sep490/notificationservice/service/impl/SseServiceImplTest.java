package com.fptu.sep490.notificationservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SseServiceImplTest {

	@Test
	void subscribe_addsEmitter_and_onCompletionRemoves() {
		SseServiceImpl service = new SseServiceImpl(new ObjectMapper());
		UUID clientId = UUID.randomUUID();
		SseEmitter emitter = service.subscribe(clientId);
		assertNotNull(emitter);
		@SuppressWarnings("unchecked")
		Map<UUID, SseEmitter> map = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(service, "clientEmitters");
		assertTrue(map.containsKey(clientId));
		CountDownLatch latch = new CountDownLatch(1);
		emitter.onCompletion(latch::countDown);
		emitter.complete();
		try { latch.await(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
		assertFalse(!map.containsKey(clientId));
	}

	@Test
	void sendMessage_withEmitter_success_keepsEmitter() throws Exception {
		SseServiceImpl service = new SseServiceImpl(new ObjectMapper());
		UUID clientId = UUID.randomUUID();
		SseEmitter emitter = service.subscribe(clientId);
		@SuppressWarnings("unchecked")
		Map<UUID, SseEmitter> map = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(service, "clientEmitters");
		service.sendMessage(clientId, "hello", "ok");
		assertTrue(map.containsKey(clientId));
	}

	@Test
	void sendMessage_emitterAbsent_noop() {
		SseServiceImpl service = new SseServiceImpl(new ObjectMapper());
		service.sendMessage(UUID.randomUUID(), "msg", "ok");
		// no exception expected
	}

	@Test
	void sendMessage_serializationThrowsIOException_removesEmitter() throws Exception {
		ObjectMapper mapper = mock(ObjectMapper.class);
		when(mapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom"){});
		SseServiceImpl service = new SseServiceImpl(mapper);
		UUID clientId = UUID.randomUUID();
		service.subscribe(clientId);
		@SuppressWarnings("unchecked")
		Map<UUID, SseEmitter> map = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(service, "clientEmitters");
		assertTrue(map.containsKey(clientId));
		service.sendMessage(clientId, "msg", "fail");
		assertFalse(map.containsKey(clientId));
	}

	@Test
	void subscribe_onErrorRemoves() {
		SseServiceImpl service = new SseServiceImpl(new ObjectMapper());
		UUID clientId = UUID.randomUUID();
		SseEmitter emitter = service.subscribe(clientId);
		@SuppressWarnings("unchecked")
		Map<UUID, SseEmitter> map = (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(service, "clientEmitters");
		assertTrue(map.containsKey(clientId));
		emitter.completeWithError(new RuntimeException("err"));
		// onError callback may execute asynchronously; wait briefly for removal
		for (int i = 0; i < 20 && map.containsKey(clientId); i++) {
			try { Thread.sleep(10); } catch (InterruptedException ignored) {}
		}
		assertFalse(!map.containsKey(clientId));
	}
}

