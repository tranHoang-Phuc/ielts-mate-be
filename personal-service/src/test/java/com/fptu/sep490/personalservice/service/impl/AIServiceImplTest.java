package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.TopicMaster;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.repository.TopicMaterRepository;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.strategy.AIStrategyFactory;
import com.fptu.sep490.personalservice.strategy.AiApiStrategy;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AIServiceImplTest {

	@Mock
	AIStrategyFactory aiStrategyFactory;
	@Mock
	ConfigRepository configRepository;
	@Mock
	TopicMaterRepository topicMaterRepository;
	@Mock
	Helper helper;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	ReadingClient readingClient;
	@Mock
	ListeningClient listeningClient;

	AIServiceImpl service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new AIServiceImpl(
				aiStrategyFactory,
				configRepository,
				topicMaterRepository,
				helper,
				objectMapper,
				readingClient,
				listeningClient
		);
	}

	@Test
	void callAIForSuggesting_success() throws Exception {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken()).thenReturn(userId);
		when(configRepository.getConfigByKeyAndAccountId(eq(Constants.Config.TARGET_CONFIG), any(UUID.class)))
				.thenReturn(Optional.of("{\"target\":\"7.0\"}"));
		when(topicMaterRepository.findAll()).thenReturn(Collections.singletonList(TopicMaster.builder().topicName("Topic 1").build()));
		when(objectMapper.writeValueAsString(any())).thenReturn("[{'topicName':'Topic 1'}]");

		AiApiStrategy strategy = mock(AiApiStrategy.class);
		when(aiStrategyFactory.getStrategy(anyString())).thenReturn(strategy);
		AIResponse expected = new AIResponse() {
			@Override
			public String getContent() { return "ok"; }
			@Override
			public boolean isSuccess() { return true; }
		};
		when(strategy.callModel(anyString(), anyString())).thenReturn(expected);

		AIResponse actual = service.callAIForSuggesting(Mockito.mock(HttpServletRequest.class));
		assertSame(expected, actual);
		verify(aiStrategyFactory).getStrategy(anyString());
		verify(strategy).callModel(anyString(), anyString());
	}

	@Test
	void callAIForSuggesting_missingConfig_throwsAppException() {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken()).thenReturn(userId);
		when(configRepository.getConfigByKeyAndAccountId(eq(Constants.Config.TARGET_CONFIG), any(UUID.class)))
				.thenReturn(Optional.empty());

		AppException ex = assertThrows(AppException.class, () -> service.callAIForSuggesting(Mockito.mock(HttpServletRequest.class)));
		assertNotNull(ex);
		verify(aiStrategyFactory, never()).getStrategy(anyString());
	}

	@Test
	void callAIForSuggesting_unexpectedException_wrapsIntoRuntime() throws Exception {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken()).thenReturn(userId);
		when(configRepository.getConfigByKeyAndAccountId(eq(Constants.Config.TARGET_CONFIG), any(UUID.class)))
				.thenReturn(Optional.of("{\"target\":\"7.0\"}"));
		when(topicMaterRepository.findAll()).thenReturn(Collections.emptyList());
		when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

		RuntimeException ex = assertThrows(RuntimeException.class, () -> service.callAIForSuggesting(Mockito.mock(HttpServletRequest.class)));
		assertTrue(ex.getMessage().startsWith("Failed to call AI model: "));
		assertTrue(ex.getMessage().contains("boom"));
		verify(aiStrategyFactory, never()).getStrategy(anyString());
	}
}
