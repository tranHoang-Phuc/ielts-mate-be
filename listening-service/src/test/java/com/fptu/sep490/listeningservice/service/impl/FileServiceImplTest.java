package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.AudioFileUpload;
import com.fptu.sep490.event.SseEvent;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FileServiceImplTest {

	static class DummyProducerFactory implements ProducerFactory<String, Object> {
		@Override
		public Producer<String, Object> createProducer() { return null; }
		@Override
		public Producer<String, Object> createProducer(String txIdPrefix) { return null; }
		@Override
		public boolean transactionCapable() { return false; }
		@Override
		public boolean isProducerPerThread() { return false; }
		@Override
		public Map<String, Object> getConfigurationProperties() { return Collections.emptyMap(); }
		@Override
		public void updateConfigs(Map<String, Object> updates) { }
		@Override
		public void addListener(Listener<String, Object> listener) { }
	}

	static class StubKafkaTemplate extends KafkaTemplate<String, Object> {
		String topic;
		Object payload;
		StubKafkaTemplate() { super(new DummyProducerFactory()); }
		public java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> send(String topic, Object data) {
			this.topic = topic;
			this.payload = data;
			return null;
		}
	}

	private static void setPrivate(Object target, String fieldName, Object value) {
		Class<?> cls = target.getClass();
		while (cls != null) {
			try {
				Field f = cls.getDeclaredField(fieldName);
				f.setAccessible(true);
				f.set(target, value);
				return;
			} catch (NoSuchFieldException ignored) {
				cls = cls.getSuperclass();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException(new NoSuchFieldException(fieldName));
	}

	static class TestFileServiceImpl extends FileServiceImpl {
		Map<String, Object> uploadResult = new HashMap<>();
		RuntimeException toThrow;
		TestFileServiceImpl(KafkaTemplate<String, Object> kafka) {
			super(null, kafka, null);
		}
		@Override
		protected Map<?, ?> doUpload(String folderName, MultipartFile multipart) {
			if (toThrow != null) throw toThrow;
			return uploadResult;
		}
	}

	@org.junit.jupiter.api.Test
	void uploadAsync_success_sendsAudioUploadEvent() throws Exception {
		StubKafkaTemplate kafka = new StubKafkaTemplate();
		TestFileServiceImpl service = new TestFileServiceImpl(kafka);
		setPrivate(service, "uploadAudioTopic", "topic1");
		setPrivate(service, "sseEventTopic", "topic2");
		service.uploadResult.put("public_id", "pid");
		service.uploadResult.put("version", Integer.valueOf(5));
		service.uploadResult.put("format", "mp3");
		service.uploadResult.put("resource_type", "video");
		service.uploadResult.put("url", "http://u");
		service.uploadResult.put("bytes", Integer.valueOf(123));

		var multipart = new org.springframework.mock.web.MockMultipartFile("f", "f.mp3", "audio/mpeg", "abc".getBytes());
		UUID taskId = UUID.randomUUID();
		UUID clientId = UUID.randomUUID();

		assertDoesNotThrow(() -> service.uploadAsync("folder", multipart, taskId, clientId));

		assertEquals("topic1", kafka.topic);
		assertTrue(kafka.payload instanceof AudioFileUpload);
		AudioFileUpload ev = (AudioFileUpload) kafka.payload;
		assertEquals(taskId, ev.getTaskId());
		assertEquals("pid", ev.getPublicId());
		assertEquals(5, ev.getVersion());
		assertEquals("mp3", ev.getFormat());
		assertEquals("video", ev.getResourceType());
		assertEquals("http://u", ev.getPublicUrl());
		assertEquals("folder", ev.getFolderName());
		assertEquals(123, ev.getBytes());
	}

	@org.junit.jupiter.api.Test
	void uploadAsync_error_sendsSseEventAndThrows() throws Exception {
		StubKafkaTemplate kafka = new StubKafkaTemplate();
		TestFileServiceImpl service = new TestFileServiceImpl(kafka);
		setPrivate(service, "uploadAudioTopic", "topic1");
		setPrivate(service, "sseEventTopic", "topic2");
		service.toThrow = new RuntimeException("boom");

		var multipart = new org.springframework.mock.web.MockMultipartFile("f", "f.mp3", "audio/mpeg", "abc".getBytes());
		UUID taskId = UUID.randomUUID();
		UUID clientId = UUID.randomUUID();

		AppException ex = assertThrows(AppException.class, () -> service.uploadAsync("folder", multipart, taskId, clientId));
		assertEquals(com.fptu.sep490.listeningservice.constants.Constants.ErrorCode.ERROR_WHEN_UPLOAD, ex.getBusinessErrorCode());
		assertEquals("topic2", kafka.topic);
		assertTrue(kafka.payload instanceof SseEvent);
		SseEvent sse = (SseEvent) kafka.payload;
		assertEquals(clientId, sse.clientId());
		assertEquals("error", sse.status());
		assertEquals("Error when handling file", sse.message());
	}
}
