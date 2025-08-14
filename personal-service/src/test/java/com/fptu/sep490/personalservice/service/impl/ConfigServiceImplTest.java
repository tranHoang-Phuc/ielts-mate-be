package com.fptu.sep490.personalservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.StreakEvent;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.ReminderConfig;
import com.fptu.sep490.personalservice.model.UserConfig;
import com.fptu.sep490.personalservice.model.enumeration.RecurrenceType;
import com.fptu.sep490.personalservice.model.json.StreakConfig;
import com.fptu.sep490.personalservice.model.json.TargetConfig;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.repository.ReminderConfigRepository;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ReminderConfigUpdateRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ReminderConfigResponse;
import com.fptu.sep490.personalservice.viewmodel.response.StreakConfigResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfigServiceImplTest {

	@Mock
	ConfigRepository configRepository;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	KafkaTemplate<String, Object> kafkaTemplate;
	@Mock
	ReminderConfigRepository reminderConfigRepository;
	@Mock
	Helper helper;

	ConfigServiceImpl service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new ConfigServiceImpl(
				configRepository,
				objectMapper,
				kafkaTemplate,
				reminderConfigRepository,
				helper
		);
		service.notificationTopic = "topic";
	}


	@Test
	void getOrAddStreakConfig_existing_today_noEvent() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(configRepository.getConfigByKeyAndAccountId(anyString(), eq(accountId)))
				.thenReturn(Optional.of("json"));
		StreakConfig cfg = StreakConfig.builder()
				.startDate(LocalDate.now())
				.lastUpdated(LocalDate.now())
				.currentStreak(5)
				.build();
		when(objectMapper.convertValue(any(), eq(StreakConfig.class))).thenReturn(cfg);
		when(configRepository.findByConfigNameAndAccountId(anyString(), eq(accountId)))
				.thenReturn(UserConfig.builder().build());
		when(objectMapper.writeValueAsString(any())).thenReturn("json");

		StreakEvent event = StreakEvent.builder().accountId(accountId).currentDate(LocalDate.now()).build();
		StreakConfig result = service.getOrAddStreakConfig(event);

		assertEquals(5, result.getCurrentStreak());
		verify(kafkaTemplate, never()).send(anyString(), any());
		verify(configRepository).save(any(UserConfig.class));
	}

	@Test
	void getOrAddStreakConfig_existing_yesterday_increments_and_sendsEventAt3() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(configRepository.getConfigByKeyAndAccountId(anyString(), eq(accountId)))
				.thenReturn(Optional.of("json"));
		StreakConfig cfg = StreakConfig.builder()
				.startDate(LocalDate.now().minusDays(5))
				.lastUpdated(LocalDate.now().minusDays(1))
				.currentStreak(2)
				.build();
		when(objectMapper.convertValue(any(), eq(StreakConfig.class))).thenReturn(cfg);
		when(configRepository.findByConfigNameAndAccountId(anyString(), eq(accountId)))
				.thenReturn(UserConfig.builder().build());
		when(objectMapper.writeValueAsString(any())).thenReturn("json");

		StreakEvent event = StreakEvent.builder().accountId(accountId).currentDate(LocalDate.now()).build();
		StreakConfig result = service.getOrAddStreakConfig(event);

		assertEquals(3, result.getCurrentStreak());
		verify(kafkaTemplate, atLeastOnce()).send(eq("topic"), any());
	}

	@Test
	void getOrAddStreakConfig_existing_notYesterday_resetsTo1_noEvent() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(configRepository.getConfigByKeyAndAccountId(anyString(), eq(accountId)))
				.thenReturn(Optional.of("json"));
		StreakConfig cfg = StreakConfig.builder()
				.startDate(LocalDate.now().minusDays(10))
				.lastUpdated(LocalDate.now().minusDays(3))
				.currentStreak(7)
				.build();
		when(objectMapper.convertValue(any(), eq(StreakConfig.class))).thenReturn(cfg);
		when(configRepository.findByConfigNameAndAccountId(anyString(), eq(accountId)))
				.thenReturn(UserConfig.builder().build());
		when(objectMapper.writeValueAsString(any())).thenReturn("json");

		StreakEvent event = StreakEvent.builder().accountId(accountId).currentDate(LocalDate.now()).build();
		StreakConfig result = service.getOrAddStreakConfig(event);

		assertEquals(1, result.getCurrentStreak());
		verify(kafkaTemplate, never()).send(anyString(), any());
	}

	@Test
	void getOrAddStreakConfig_optionalEmpty_firstWriteFails_throwsAppException() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(configRepository.getConfigByKeyAndAccountId(anyString(), eq(accountId)))
				.thenReturn(Optional.empty());
		when(objectMapper.writeValueAsString(any()))
				.thenThrow(new JsonProcessingException("boom") {});

		StreakEvent event = StreakEvent.builder().accountId(accountId).currentDate(LocalDate.now()).build();
		assertThrows(AppException.class, () -> service.getOrAddStreakConfig(event));
		verify(configRepository, never()).save(any());
	}

	@Test
	void getOrAddStreakConfig_optionalEmpty_secondWriteFails_throwsAppException() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(configRepository.getConfigByKeyAndAccountId(anyString(), eq(accountId)))
				.thenReturn(Optional.empty());
		when(objectMapper.writeValueAsString(any()))
				.thenReturn("json")
				.thenThrow(new JsonProcessingException("boom2") {});

		StreakEvent event = StreakEvent.builder().accountId(accountId).currentDate(LocalDate.now()).build();
		assertThrows(AppException.class, () -> service.getOrAddStreakConfig(event));
		verify(configRepository).save(any(UserConfig.class));
	}
    @Test
    void getStreak_noConfig_returnsDefault() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.empty());

        LocalDate today = LocalDate.now();
        StreakConfigResponse resp = service.getStreak(req);
        assertEquals(0, resp.currentStreak());
        assertEquals(today, resp.lastUpdated());
        assertEquals(today, resp.startDate());
    }

    @Test
    void getStreak_validJson_returnsParsed() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.of("json"));
        StreakConfig cfg = StreakConfig.builder()
                .startDate(LocalDate.of(2024,1,1))
                .lastUpdated(LocalDate.of(2024,1,2))
                .currentStreak(7)
                .build();
        when(objectMapper.readValue(eq("json"), eq(StreakConfig.class))).thenReturn(cfg);

        StreakConfigResponse resp = service.getStreak(req);
        assertEquals(7, resp.currentStreak());
        assertEquals(LocalDate.of(2024,1,2), resp.lastUpdated());
        assertEquals(LocalDate.of(2024,1,1), resp.startDate());
    }

    @Test
    void getStreak_invalidJson_throwsAppException() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.of("json"));
        when(objectMapper.readValue(eq("json"), eq(StreakConfig.class))).thenThrow(new JsonProcessingException("bad") {});

        assertThrows(AppException.class, () -> service.getStreak(req));
    }

    @Test
    void getReminder_null_returnsNull() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.findByAccountId(any())).thenReturn(null);

        assertNull(service.getReminder(req));
    }

    @Test
    void getReminder_present_mapsFields() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        ReminderConfig cfg = ReminderConfig.builder()
                .configId(1)
                .email("a@b.com")
                .reminderDate(List.of(LocalDate.of(2024,1,1)))
                .reminderTime(LocalTime.of(9,0))
                .enabled(true)
                .recurrence(RecurrenceType.DAILY)
                .timeZone("UTC")
                .message("msg")
                .build();
        when(reminderConfigRepository.findByAccountId(any())).thenReturn(cfg);

        ReminderConfigResponse resp = service.getReminder(req);
        assertEquals(1, resp.configId());
        assertEquals("a@b.com", resp.email());
        assertEquals(LocalTime.of(9,0), resp.reminderTime());
        assertEquals("UTC", resp.zone());
        assertEquals(RecurrenceType.DAILY.ordinal(), resp.recurrence());
        assertTrue(resp.enabled());
        assertEquals("msg", resp.message());
    }

    @Test
    void registerReminder_exists_throwsAppException() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.existsByAccountId(any())).thenReturn(true);
        ReminderConfigCreationRequest r = ReminderConfigCreationRequest.builder()
                .email("a@b.com")
                .reminderDate(List.of(LocalDate.now()))
                .reminderTime(LocalTime.NOON)
                .recurrence(1)
                .timeZone("UTC")
                .enable(true)
                .message("m")
                .build();
        assertThrows(AppException.class, () -> service.registerReminder(r, req));
    }

    @Test
    void registerReminder_invalidRecurrence_throwsAppException() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.existsByAccountId(any())).thenReturn(false);
        ReminderConfigCreationRequest r = ReminderConfigCreationRequest.builder()
                .email("a@b.com")
                .reminderDate(List.of(LocalDate.now()))
                .reminderTime(LocalTime.NOON)
                .recurrence(999)
                .timeZone("UTC")
                .enable(true)
                .message("m")
                .build();
        assertThrows(AppException.class, () -> service.registerReminder(r, req));
    }

    @Test
    void registerReminder_success() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.existsByAccountId(any())).thenReturn(false);
        when(reminderConfigRepository.save(any(ReminderConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReminderConfigCreationRequest r = ReminderConfigCreationRequest.builder()
                .email("a@b.com")
                .reminderDate(List.of(LocalDate.now()))
                .reminderTime(LocalTime.NOON)
                .recurrence(1)
                .timeZone("UTC")
                .enable(true)
                .message("m")
                .build();

        ReminderConfigResponse resp = service.registerReminder(r, req);
        assertEquals("a@b.com", resp.email());
        assertEquals(LocalTime.NOON, resp.reminderTime());
    }

    @Test
    void updateReminder_notExists_throwsAppException() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.existsByAccountId(any())).thenReturn(false);
        ReminderConfigUpdateRequest r = new ReminderConfigUpdateRequest(
                "a@b.com", List.of(LocalDate.now()), LocalTime.NOON, 1, "UTC", true, "m");
        assertThrows(AppException.class, () -> service.updateReminder(r, req));
    }

    @Test
    void updateReminder_invalidRecurrence_throwsAppException() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.existsByAccountId(any())).thenReturn(true);
        when(reminderConfigRepository.findByAccountId(any())).thenReturn(ReminderConfig.builder().build());
        ReminderConfigUpdateRequest r = new ReminderConfigUpdateRequest(
                "a@b.com", List.of(LocalDate.now()), LocalTime.NOON, 999, "UTC", true, "m");
        assertThrows(AppException.class, () -> service.updateReminder(r, req));
    }

    @Test
    void updateReminder_success() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(reminderConfigRepository.existsByAccountId(any())).thenReturn(true);
        ReminderConfig existing = ReminderConfig.builder().build();
        when(reminderConfigRepository.findByAccountId(any())).thenReturn(existing);
        when(reminderConfigRepository.save(any(ReminderConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReminderConfigUpdateRequest r = new ReminderConfigUpdateRequest(
                "a@b.com", List.of(LocalDate.now()), LocalTime.NOON, 1, "UTC", true, "m");
        ReminderConfigResponse resp = service.updateReminder(r, req);
        assertEquals("a@b.com", resp.email());
        assertEquals(LocalTime.NOON, resp.reminderTime());
        assertEquals(RecurrenceType.DAILY.ordinal(), resp.recurrence());
    }

    @Test
    void getTarget_noValue_returnsNull() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.empty());
        assertNull(service.getTarget(req));
    }

    @Test
    void getTarget_value_parsed() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.of("json"));
        TargetConfig tc = TargetConfig.builder().listeningTarget(7.0f).readingTarget(8.0f).build();
        when(objectMapper.readValue(eq("json"), eq(TargetConfig.class))).thenReturn(tc);
        TargetConfig result = service.getTarget(req);
        assertEquals(7.0f, result.getListeningTarget());
        assertEquals(8.0f, result.getReadingTarget());
    }

    @Test
    void addOrUpdate_noExisting_createsNew() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("json");

        TargetConfig tc = TargetConfig.builder().listeningTarget(6.5f).readingTarget(7.0f).build();
        TargetConfig result = service.addOrUpdate(req, tc);
        assertSame(tc, result);
        verify(configRepository).save(any(UserConfig.class));
    }

    @Test
    void addOrUpdate_existing_updates() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
        when(configRepository.getConfigByKeyAndAccountId(anyString(), any())).thenReturn(Optional.of("val"));
        when(configRepository.findByAccountIdAndConfigName(any(), anyString())).thenReturn(UserConfig.builder().build());
        when(objectMapper.writeValueAsString(any())).thenReturn("json");

        TargetConfig tc = TargetConfig.builder().listeningTarget(6.0f).readingTarget(6.5f).build();
        TargetConfig result = service.addOrUpdate(req, tc);
        assertSame(tc, result);
        verify(configRepository).save(any(UserConfig.class));
    }

    @Test
    void buildMessage_private_allBranches_and_noMilestone() throws Exception {
        Method m = ConfigServiceImpl.class.getDeclaredMethod("buildMessage", int.class);
        m.setAccessible(true);
        assertNotNull(m.invoke(service, 90));
        assertNotNull(m.invoke(service, 30));
        assertNotNull(m.invoke(service, 10));
        assertNotNull(m.invoke(service, 3));
        assertNull(m.invoke(service, 7));
    }
}
