package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.enumeration.IeltsType;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.repository.client.MarkupClient;
import com.fptu.sep490.listeningservice.service.FileService;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.*;
	import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
	import org.springframework.util.LinkedMultiValueMap;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ListeningTaskServiceImplTest {

	@InjectMocks
	ListeningTaskServiceImpl service;
	@Mock ListeningTaskRepository listeningTaskRepository;
	@Mock QuestionGroupRepository questionGroupRepository;
	@Mock DragItemRepository dragItemRepository;
	@Mock QuestionRepository questionRepository;
	@Mock ChoiceRepository choiceRepository;
	@Mock FileService fileService;
	@Mock Helper helper;
	@Mock RedisService redisService;
	@Mock KeyCloakUserClient keyCloakUserClient;
	@Mock KeyCloakTokenClient keyCloakTokenClient;
	@Mock MarkupClient markupClient;
	@Mock KafkaTemplate<String, Object> kafkaTemplate;
	@Mock HttpServletRequest httpServletRequest;

	@BeforeEach
	void setUp() throws JsonProcessingException {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(service, "topicMasterTopic", "topic.master");
		ReflectionTestUtils.setField(service, "realm", "realm");
		ReflectionTestUtils.setField(service, "clientId", "client");
		ReflectionTestUtils.setField(service, "clientSecret", "secret");
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer ctoken"), anyString()))
				.thenReturn(UserProfileResponse.builder().id("u1").firstName("A").lastName("B").email("e@x.com").build());
	}

	@Test
	void getUserProfileById_cached_returnsCachedProfile_inService() throws Exception {
		String userId = "u1";
		UserProfileResponse cached = UserProfileResponse.builder().id(userId).firstName("A").lastName("B").email("e@x.com").build();
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(cached);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");

		UserProfileResponse res = ReflectionTestUtils.invokeMethod(service, "getUserProfileById", userId);

		assertEquals(cached, res);
		verify(keyCloakUserClient, never()).getUserById(anyString(), anyString(), anyString());
	}

	@Test
	void getUserProfileById_noCache_fetchesAndCaches_inService() throws Exception {
		String userId = "u2";
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(null);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		UserProfileResponse fetched = UserProfileResponse.builder().id(userId).firstName("C").lastName("D").email("cd@x.com").build();
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer ctoken"), eq(userId))).thenReturn(fetched);

		UserProfileResponse res = ReflectionTestUtils.invokeMethod(service, "getUserProfileById", userId);

		assertEquals(fetched, res);
		verify(redisService).saveValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(fetched), any());
	}

	@Test
	void getUserProfileById_noCachedToken_requestsToken_thenFetches_inService() throws Exception {
		String userId = "u3";
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(null);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn(null);
		when(keyCloakTokenClient.requestToken(any(LinkedMultiValueMap.class), eq("realm")))
				.thenReturn(KeyCloakTokenResponse.builder().accessToken("newtoken").expiresIn(3600).build());
		UserProfileResponse fetched = UserProfileResponse.builder().id(userId).email("u3@x.com").build();
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer newtoken"), eq(userId))).thenReturn(fetched);

		UserProfileResponse res = ReflectionTestUtils.invokeMethod(service, "getUserProfileById", userId);

		assertEquals(fetched, res);
		verify(redisService).saveValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq("newtoken"), any());
		verify(redisService).saveValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(fetched), any());
	}

	@Test
	void getUserProfileById_nullFromKeycloak_throws_inService() throws JsonProcessingException {
		String userId = "u4";
		when(redisService.getValue(eq(Constants.RedisKey.USER_PROFILE + userId), eq(UserProfileResponse.class))).thenReturn(null);
		when(redisService.getValue(eq(Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN), eq(String.class))).thenReturn("ctoken");
		when(keyCloakUserClient.getUserById(eq("realm"), eq("Bearer ctoken"), eq(userId))).thenReturn(null);

		AppException ex = assertThrows(AppException.class, () -> ReflectionTestUtils.invokeMethod(service, "getUserProfileById", userId));
		assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
	}

	@Test
	void getTaskTitle_success_mapsTitlesAndIds() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		var t1 = ListeningTask.builder().taskId(id1).title("Alpha").build();
		var t2 = ListeningTask.builder().taskId(id2).title("Beta").build();
		when(listeningTaskRepository.findAllById(eq(List.of(id1, id2)))).thenReturn(List.of(t1, t2));

		List<TaskTitle> res = service.getTaskTitle(List.of(id1, id2));

		assertEquals(2, res.size());
		assertEquals(id1, res.get(0).taskId());
		assertEquals("Alpha", res.get(0).title());
		assertEquals(id2, res.get(1).taskId());
		assertEquals("Beta", res.get(1).title());
	}

	@Test
	void getTaskTitle_emptyInput_returnsEmptyList() {
		when(listeningTaskRepository.findAllById(eq(List.of()))).thenReturn(List.of());
		List<TaskTitle> res = service.getTaskTitle(List.of());
		assertNotNull(res);
		assertTrue(res.isEmpty());
	}

	@Test
	void fromExamAttemptHistory_success_mapsByPartAndDetails() {
		// Arrange tasks (Part 1 and Part 2)
		UUID t1Id = UUID.randomUUID();
		UUID t2Id = UUID.randomUUID();
		UUID audio1 = UUID.randomUUID();
		UUID audio2 = UUID.randomUUID();
		ListeningTask t1 = ListeningTask.builder()
				.taskId(t1Id)
				.title("T1")
				.instruction("I1")
				.audioFileId(audio1)
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.build();
		ListeningTask t2 = ListeningTask.builder()
				.taskId(t2Id)
				.title("T2")
				.instruction("I2")
				.audioFileId(audio2)
				.ieltsType(IeltsType.GENERAL_TRAINING)
				.partNumber(PartNumber.PART_2)
				.build();
		when(listeningTaskRepository.findAllByIdSortedByPartNumber(anyList()))
				.thenReturn(List.of(t1, t2));

		// Arrange groups mapped to task part numbers
		UUID g1Id = UUID.randomUUID();
		UUID g2Id = UUID.randomUUID();
		var g1 = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(g1.getGroupId()).thenReturn(g1Id);
		when(g1.getSectionOrder()).thenReturn(1);
		when(g1.getSectionLabel()).thenReturn("G1");
		when(g1.getInstruction()).thenReturn("GInstr1");
		when(g1.getListeningTask()).thenReturn(t1);
		when(g1.getDragItems()).thenReturn(List.of(mock(com.fptu.sep490.listeningservice.model.DragItem.class))); // trigger drag mapping

		var g2 = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(g2.getGroupId()).thenReturn(g2Id);
		when(g2.getSectionOrder()).thenReturn(2);
		when(g2.getSectionLabel()).thenReturn("G2");
		when(g2.getInstruction()).thenReturn("GInstr2");
		when(g2.getListeningTask()).thenReturn(t2);
		when(g2.getDragItems()).thenReturn(List.of(mock(com.fptu.sep490.listeningservice.model.DragItem.class)));

		when(questionGroupRepository.findAllByIdOrderBySectionOrder(anyList()))
				.thenReturn(List.of(g1, g2));

		// Arrange questions: q1 for g1 is MC, q2 for g2 is non-MC
		UUID q1Id = UUID.randomUUID();
		UUID q2Id = UUID.randomUUID();
		var q1 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q1.getQuestionId()).thenReturn(q1Id);
		when(q1.getQuestionGroup()).thenReturn(g1);
		when(q1.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(q1.getQuestionOrder()).thenReturn(1);
		when(q1.getBlankIndex()).thenReturn(0);
		when(q1.getInstructionForChoice()).thenReturn("IC1");
		when(q1.getNumberOfCorrectAnswers()).thenReturn(1);
		when(q1.getInstructionForMatching()).thenReturn("IM1");
		when(q1.getZoneIndex()).thenReturn(0);
		when(q1.getCorrectAnswer()).thenReturn("A");
		when(q1.getCorrectAnswerForMatching()).thenReturn("A->1");
		when(q1.getExplanation()).thenReturn("E1");
		when(q1.getPoint()).thenReturn(1);

		var q2 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q2.getQuestionId()).thenReturn(q2Id);
		when(q2.getQuestionGroup()).thenReturn(g2);
		when(q2.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.FILL_IN_THE_BLANKS);
		when(q2.getQuestionOrder()).thenReturn(2);
		when(q2.getBlankIndex()).thenReturn(1);
		when(q2.getInstructionForChoice()).thenReturn(null);
		when(q2.getNumberOfCorrectAnswers()).thenReturn(1);
		when(q2.getInstructionForMatching()).thenReturn(null);
		when(q2.getZoneIndex()).thenReturn(1);
		when(q2.getCorrectAnswer()).thenReturn("B");
		when(q2.getCorrectAnswerForMatching()).thenReturn(null);
		when(q2.getExplanation()).thenReturn("E2");
		when(q2.getPoint()).thenReturn(2);

		when(questionRepository.findAllByIdOrderByQuestionOrder(anyList()))
				.thenReturn(List.of(q1, q2));

		// Arrange drag items from history per group
		UUID di1Id = UUID.randomUUID();
		UUID di2Id = UUID.randomUUID();
		var d1 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(d1.getDragItemId()).thenReturn(di1Id);
		when(d1.getContent()).thenReturn("D1");
		var d2 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(d2.getDragItemId()).thenReturn(di2Id);
		when(d2.getContent()).thenReturn("D2");
		when(dragItemRepository.findAllById(eq(List.of(di1Id)))).thenReturn(List.of(d1));
		when(dragItemRepository.findAllById(eq(List.of(di2Id)))).thenReturn(List.of(d2));

		// Arrange choices for q1 (MC)
		UUID c1Id = UUID.randomUUID();
		UUID c2Id = UUID.randomUUID();
		var c1 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(c1.getChoiceId()).thenReturn(c1Id);
		when(c1.getLabel()).thenReturn("A");
		when(c1.getContent()).thenReturn("Alpha");
		when(c1.getChoiceOrder()).thenReturn(1);
		when(c1.isCorrect()).thenReturn(true);
		var c2 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(c2.getChoiceId()).thenReturn(c2Id);
		when(c2.getLabel()).thenReturn("B");
		when(c2.getContent()).thenReturn("Beta");
		when(c2.getChoiceOrder()).thenReturn(2);
		when(c2.isCorrect()).thenReturn(false);
		when(choiceRepository.findById(c1Id)).thenReturn(Optional.of(c1));
		when(choiceRepository.findById(c2Id)).thenReturn(Optional.of(c2));

		// Build history
		var history = com.fptu.sep490.listeningservice.model.json.ExamAttemptHistory.builder()
				.taskId(List.of(t1Id, t2Id))
				.questionGroupIds(List.of(g1Id, g2Id))
				.groupMapItems(Map.of(g1Id, List.of(di1Id), g2Id, List.of(di2Id)))
				.questionIds(List.of(q1Id, q2Id))
				.questionMapChoices(Map.of(q1Id, List.of(c1Id, c2Id)))
				.build();

		// Act
		var res = service.fromExamAttemptHistory(history);

		// Assert task-level mapping
		assertEquals(2, res.size());
		var t1Res = res.get(0);
		assertEquals(t1Id, t1Res.taskId());
		assertEquals(PartNumber.PART_1.ordinal(), t1Res.partNumber());
		assertEquals("T1", t1Res.title());
		assertEquals("I1", t1Res.instruction());
		assertEquals(audio1, t1Res.audioFileId());
		assertEquals(1, t1Res.questionGroups().size());
		var g1Res = t1Res.questionGroups().get(0);
		assertEquals(g1Id, g1Res.questionGroupId());
		assertEquals(1, g1Res.sectionOrder());
		assertEquals("G1", g1Res.sectionLabel());
		assertEquals("GInstr1", g1Res.instruction());
		assertEquals(1, g1Res.dragItems().size());
		assertEquals(di1Id.toString(), g1Res.dragItems().get(0).dragItemId());
		assertEquals("D1", g1Res.dragItems().get(0).content());
		assertEquals(1, g1Res.questions().size());
		var q1Mapped = g1Res.questions().get(0);
		assertEquals(q1Id, q1Mapped.questionId());
		assertEquals(1, q1Mapped.questionOrder());
		assertEquals(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE.ordinal(), q1Mapped.questionType());
		assertEquals("IC1", q1Mapped.instructionForChoice());
		assertEquals(1, q1Mapped.numberOfCorrectAnswers());
		assertEquals("IM1", q1Mapped.instructionForMatching());
		assertEquals(0, q1Mapped.zoneIndex());
		assertEquals("A", q1Mapped.correctAnswer());
		assertEquals("A->1", q1Mapped.correctAnswerForMatching());
		assertEquals("E1", q1Mapped.explanation());
		assertEquals(1, q1Mapped.point());
		assertNotNull(q1Mapped.choices());
		assertEquals(2, q1Mapped.choices().size());
		assertEquals(c1Id, q1Mapped.choices().get(0).choiceId());
		assertEquals("A", q1Mapped.choices().get(0).label());
		assertEquals("Alpha", q1Mapped.choices().get(0).content());
		assertEquals(1, q1Mapped.choices().get(0).choiceOrder());
		assertEquals(true, q1Mapped.choices().get(0).isCorrect());

		var t2Res = res.get(1);
		assertEquals(t2Id, t2Res.taskId());
		assertEquals(PartNumber.PART_2.ordinal(), t2Res.partNumber());
		assertEquals(1, t2Res.questionGroups().size());
		var g2Res = t2Res.questionGroups().get(0);
		assertEquals(g2Id, g2Res.questionGroupId());
		assertEquals(1, g2Res.dragItems().size());
		assertEquals(di2Id.toString(), g2Res.dragItems().get(0).dragItemId());
		assertEquals("D2", g2Res.dragItems().get(0).content());
		assertEquals(1, g2Res.questions().size());
		var q2Mapped = g2Res.questions().get(0);
		assertEquals(q2Id, q2Mapped.questionId());
		assertEquals(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.FILL_IN_THE_BLANKS.ordinal(), q2Mapped.questionType());
		assertTrue(q2Mapped.choices() == null || q2Mapped.choices().isEmpty());
	}

	@Test
	void fromExamAttemptHistory_missingChoice_throws() {
		// Arrange minimal data: one task, one group, one MC question with missing choice
		UUID tId = UUID.randomUUID();
		ListeningTask t = ListeningTask.builder()
				.taskId(tId)
				.title("T")
				.instruction("I")
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.build();
		when(listeningTaskRepository.findAllByIdSortedByPartNumber(anyList())).thenReturn(List.of(t));

		UUID gId = UUID.randomUUID();
		var g = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(g.getGroupId()).thenReturn(gId);
		when(g.getSectionOrder()).thenReturn(1);
		when(g.getSectionLabel()).thenReturn("G");
		when(g.getInstruction()).thenReturn("GI");
		when(g.getListeningTask()).thenReturn(t);
		when(g.getDragItems()).thenReturn(Collections.emptyList());
		when(questionGroupRepository.findAllByIdOrderBySectionOrder(anyList())).thenReturn(List.of(g));

		UUID qId = UUID.randomUUID();
		var q = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q.getQuestionId()).thenReturn(qId);
		when(q.getQuestionGroup()).thenReturn(g);
		when(q.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(q.getQuestionOrder()).thenReturn(1);
		when(questionRepository.findAllByIdOrderByQuestionOrder(anyList())).thenReturn(List.of(q));

		UUID missingChoiceId = UUID.randomUUID();
		when(choiceRepository.findById(missingChoiceId)).thenReturn(Optional.empty());

		var history = com.fptu.sep490.listeningservice.model.json.ExamAttemptHistory.builder()
				.taskId(List.of(tId))
				.questionGroupIds(List.of(gId))
				.groupMapItems(Collections.emptyMap())
				.questionIds(List.of(qId))
				.questionMapChoices(Map.of(qId, List.of(missingChoiceId)))
				.build();

		// Act + Assert
		AppException ex = assertThrows(AppException.class, () -> service.fromExamAttemptHistory(history));
		assertEquals(Constants.ErrorCode.CHOICE_NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void createListeningTask_invalid_nullAudio_throws() {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
		ListeningTaskCreationRequest req = ListeningTaskCreationRequest.builder()
				.ieltsType(0)
				.partNumber(0)
				.instruction("instr")
				.title("title")
				.status(0)
				.audioFile(null)
				.isAutomaticTranscription(true)
				.transcription(null)
				.build();
		AppException ex = assertThrows(AppException.class, () -> service.createListeningTask(req, httpServletRequest));
		assertEquals(Constants.ErrorCode.INVALID_REQUEST, ex.getBusinessErrorCode());
	}

	@Test
	void createListeningTask_invalid_contentType_throws() {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
		MultipartFile bad = new org.springframework.mock.web.MockMultipartFile("f", "f.txt", "text/plain", "abc".getBytes());
		ListeningTaskCreationRequest req = ListeningTaskCreationRequest.builder()
				.ieltsType(0)
				.partNumber(0)
				.instruction("instr")
				.title("title")
				.status(0)
				.audioFile(bad)
				.isAutomaticTranscription(false)
				.transcription("t")
				.build();
		AppException ex = assertThrows(AppException.class, () -> service.createListeningTask(req, httpServletRequest));
		assertEquals(Constants.ErrorCode.INVALID_REQUEST, ex.getBusinessErrorCode());
	}

	@Test
	void createListeningTask_invalid_tooLarge_throws() {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
		byte[] big = new byte[10 * 1024 * 1024 + 1];
		MultipartFile mf = new org.springframework.mock.web.MockMultipartFile("f", "f.mp3", "audio/mpeg", big);
		ListeningTaskCreationRequest req = ListeningTaskCreationRequest.builder()
				.ieltsType(0)
				.partNumber(0)
				.instruction("instr")
				.title("title")
				.status(0)
				.audioFile(mf)
				.isAutomaticTranscription(false)
				.transcription("t")
				.build();
		AppException ex = assertThrows(AppException.class, () -> service.createListeningTask(req, httpServletRequest));
		assertEquals(Constants.ErrorCode.INVALID_REQUEST, ex.getBusinessErrorCode());
	}

	@Test
	void createListeningTask_success_persists_uploads_and_sendsKafka() throws Exception {
		// Arrange
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
		MultipartFile mf = new org.springframework.mock.web.MockMultipartFile("f", "f.mp3", "audio/mpeg", "abc".getBytes());
		ListeningTaskCreationRequest req = ListeningTaskCreationRequest.builder()
				.ieltsType(0) // IeltsType.ACADEMIC
				.partNumber(0) // PartNumber.PART_1
				.instruction("instr")
				.title("title")
				.status(1) // assume PUBLISHED ordinal 1
				.audioFile(mf)
				.isAutomaticTranscription(true) // should nullify transcription
				.transcription("ignored")
				.build();

		UUID taskId = UUID.randomUUID();
		UUID audioId = UUID.randomUUID();
		ListeningTask saved = ListeningTask.builder()
				.taskId(taskId)
				.audioFileId(audioId)
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.title("title")
				.instruction("instr")
				.status(Status.PUBLISHED)
				.build();
		when(listeningTaskRepository.save(any(ListeningTask.class))).thenReturn(saved);

		// Act
		ListeningTaskResponse res = service.createListeningTask(req, httpServletRequest);

		// Assert
		assertEquals(taskId, res.taskId());
		assertEquals(audioId, res.audioFileId());
		assertEquals(IeltsType.ACADEMIC.ordinal(), res.ieltsType());
		assertEquals(PartNumber.PART_1.ordinal(), res.partNumber());
		assertEquals("title", res.title());
		assertEquals("instr", res.instruction());
		verify(fileService).uploadAsync(eq("listening-tasks"), same(mf), eq(taskId), eq(UUID.fromString(userId)), false);
		verify(kafkaTemplate).send(eq("topic.master"), any());
	}

	@Test
	void updateTask_notFound_throws() {
		when(listeningTaskRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> service.updateTask(UUID.randomUUID(), null, null, null, null, null, null, null, httpServletRequest));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void updateTask_withAudio_updatesFields_uploadsAndSendsKafka() throws Exception {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
		UUID originalTaskId = UUID.randomUUID();
		UUID originalAudioId = UUID.randomUUID();
		ListeningTask original = ListeningTask.builder()
				.taskId(originalTaskId)
				.audioFileId(originalAudioId)
				.ieltsType(IeltsType.GENERAL_TRAINING)
				.partNumber(PartNumber.PART_2)
				.title("old-title")
				.instruction("old-instr")
				.status(Status.DRAFT)
				.version(3)
				.build();
		when(listeningTaskRepository.findById(originalTaskId)).thenReturn(Optional.of(original));
		List<ListeningTask> allVersions = new ArrayList<>();
		allVersions.add(ListeningTask.builder().taskId(UUID.randomUUID()).version(1).build());
		allVersions.add(ListeningTask.builder().taskId(UUID.randomUUID()).version(5).build()); // max = 5
		when(listeningTaskRepository.findAllVersion(originalTaskId)).thenReturn(allVersions);
		when(listeningTaskRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
		when(listeningTaskRepository.save(any(ListeningTask.class))).thenAnswer(inv -> {
			ListeningTask nv = inv.getArgument(0);
			if (nv.getTaskId() == null) nv.setTaskId(UUID.randomUUID());
			return nv;
		});

		MultipartFile newAudio = new org.springframework.mock.web.MockMultipartFile("f", "f.mp3", "audio/mpeg", "abc".getBytes());

		ListeningTaskResponse res = service.updateTask(
				originalTaskId,
				Status.PUBLISHED.ordinal(),
				IeltsType.ACADEMIC.ordinal(),
				PartNumber.PART_3.ordinal(),
				"new-instr",
				"new-title",
				newAudio,
				"new-trans",
				httpServletRequest
		);

		assertEquals("new-title", res.title());
		assertEquals(PartNumber.PART_3.ordinal(), res.partNumber());
		assertEquals(IeltsType.ACADEMIC.ordinal(), res.ieltsType());
		verify(fileService).uploadAsync(eq("listening-tasks"), same(newAudio), any(UUID.class), eq(UUID.fromString(userId)),false);
		verify(kafkaTemplate).send(eq("topic.master"), any());
	}

	@Test
	void updateTask_withoutAudio_copiesAudioId_andFallsBackFields() throws Exception {
		String userId = UUID.randomUUID().toString();
		when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
		UUID originalTaskId = UUID.randomUUID();
		UUID originalAudioId = UUID.randomUUID();
		ListeningTask original = ListeningTask.builder()
				.taskId(originalTaskId)
				.audioFileId(originalAudioId)
				.ieltsType(IeltsType.GENERAL_TRAINING)
				.partNumber(PartNumber.PART_2)
				.title("old-title")
				.instruction("old-instr")
				.status(Status.DRAFT)
				.version(2)
				.build();
		when(listeningTaskRepository.findById(originalTaskId)).thenReturn(Optional.of(original));
		when(listeningTaskRepository.findAllVersion(originalTaskId)).thenReturn(List.of(ListeningTask.builder().taskId(UUID.randomUUID()).version(2).build()));
		ArgumentCaptor<ListeningTask> savedCaptor = ArgumentCaptor.forClass(ListeningTask.class);
		when(listeningTaskRepository.save(any(ListeningTask.class))).thenAnswer(inv -> inv.getArgument(0));
		when(listeningTaskRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

		ListeningTaskResponse res = service.updateTask(
				originalTaskId,
				null, // keep status from original
				null, // keep ielts type
				null, // keep part number
				null, // keep instruction
				null, // keep title
				null, // no audio -> copy audio id
				null,
				httpServletRequest
		);

		verify(listeningTaskRepository, atLeastOnce()).save(savedCaptor.capture());
		ListeningTask lastSaved = savedCaptor.getValue();
		assertEquals(originalAudioId, lastSaved.getAudioFileId());
		assertEquals(original.getIeltsType().ordinal(), res.ieltsType());
		assertEquals(original.getPartNumber().ordinal(), res.partNumber());
		assertEquals(original.getTitle(), res.title());
		assertEquals(original.getInstruction(), res.instruction());
		verify(fileService, never()).uploadAsync(any(), any(), any(), any(),anyBoolean());
		verify(kafkaTemplate).send(eq("topic.master"), any());
	}

	@Test
	void deleteTask_notFound_throws() {
		when(listeningTaskRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> service.deleteTask(UUID.randomUUID()));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void deleteTask_success_marksDeleted_sendsKafka_andSavesAll() {
		UUID taskId = UUID.randomUUID();
		ListeningTask origin = ListeningTask.builder().taskId(taskId).title("t").isDeleted(false).build();
		ListeningTask child1 = ListeningTask.builder().taskId(UUID.randomUUID()).isDeleted(false).parent(origin).build();
		ListeningTask child2 = ListeningTask.builder().taskId(UUID.randomUUID()).isDeleted(false).parent(origin).build();
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(origin));
		when(listeningTaskRepository.findAllByParentId(taskId)).thenReturn(List.of(child1, child2));

		ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

		service.deleteTask(taskId);

		verify(kafkaTemplate).send(eq("topic.master"), any());
		verify(listeningTaskRepository).saveAll(listCaptor.capture());
		@SuppressWarnings("unchecked")
		List<ListeningTask> savedList = listCaptor.getValue();
		assertEquals(3, savedList.size());
		assertTrue(savedList.get(0).getIsDeleted());
		assertTrue(savedList.get(1).getIsDeleted());
		assertTrue(savedList.get(2).getIsDeleted());
	}

	@Test
	void getActivatedTask_noAccessToken_returnsOriginalPage() {
		ListeningTask t = ListeningTask.builder()
				.taskId(UUID.randomUUID())
				.title("T1")
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.status(Status.PUBLISHED)
				.build();
		// set required fields for mapping
		ReflectionTestUtils.setField(t, "createdBy", "u1");
		ReflectionTestUtils.setField(t, "updatedBy", "u1");
		ReflectionTestUtils.setField(t, "createdAt", LocalDateTime.now());
		ReflectionTestUtils.setField(t, "updatedAt", LocalDateTime.now());

		Page<ListeningTask> page = new PageImpl<>(List.of(t), PageRequest.of(0, 10), 1);
		when(listeningTaskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
		when(listeningTaskRepository.findCurrentVersionsByIds(anyList())).thenReturn(List.of(t));
		when(httpServletRequest.getCookies()).thenReturn(null);

		Page<ListeningTaskGetResponse> res = service.getActivatedTask(0, 10, null, null, null, null, null, null, null, httpServletRequest);
		assertEquals(1, res.getTotalElements());
		ListeningTaskGetResponse first = res.getContent().get(0);
		assertEquals("T1", first.title());
		assertNull(first.isMarkedUp());
	}

	@Test
	void getActivatedTask_withMarkup_appliesFlags() {
		ListeningTask t = ListeningTask.builder()
				.taskId(UUID.randomUUID())
				.title("T1")
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.status(Status.PUBLISHED)
				.build();
		ReflectionTestUtils.setField(t, "createdBy", "u1");
		ReflectionTestUtils.setField(t, "updatedBy", "u1");
		ReflectionTestUtils.setField(t, "createdAt", LocalDateTime.now());
		ReflectionTestUtils.setField(t, "updatedAt", LocalDateTime.now());

		Page<ListeningTask> page = new PageImpl<>(List.of(t), PageRequest.of(0, 10), 1);
		when(listeningTaskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
		when(listeningTaskRepository.findCurrentVersionsByIds(anyList())).thenReturn(List.of(t));

		Cookie auth = new Cookie("Authorization", "abc");
		when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{auth});

		MarkedUpResponse marked = new MarkedUpResponse(Map.of(t.getTaskId(), 2));
		BaseResponse<MarkedUpResponse> base = new BaseResponse<>("success", "ok", marked, null);
		ResponseEntity<BaseResponse<MarkedUpResponse>> ok = new ResponseEntity<>(base, HttpStatus.OK);
		when(markupClient.getMarkedUpData(anyString(), anyString())).thenReturn(ok);

		Page<ListeningTaskGetResponse> res = service.getActivatedTask(0, 10, null, null, null, null, null, null, null, httpServletRequest);
		assertEquals(1, res.getTotalElements());
		ListeningTaskGetResponse first = res.getContent().get(0);
		assertEquals("T1", first.title());
		assertEquals(true, first.isMarkedUp());
		assertEquals(2, first.markupTypes());
	}

	@Test
	void getListeningTask_mixedParentAndRoot_mapsAndSorts() {
		// Parent and child
		UUID parentId = UUID.randomUUID();
		ListeningTask parent = ListeningTask.builder()
				.taskId(parentId)
				.title("ParentTitle")
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.status(Status.PUBLISHED)
				.build();
		ReflectionTestUtils.setField(parent, "createdBy", "u1");
		ReflectionTestUtils.setField(parent, "updatedBy", "u1");
		ReflectionTestUtils.setField(parent, "createdAt", LocalDateTime.now().minusHours(2));
		ReflectionTestUtils.setField(parent, "updatedAt", LocalDateTime.now().minusHours(2));

		ListeningTask child = ListeningTask.builder()
				.taskId(UUID.randomUUID())
				.title("ChildCurrent")
				.ieltsType(IeltsType.GENERAL_TRAINING)
				.partNumber(PartNumber.PART_2)
				.status(Status.PUBLISHED)
				.parent(parent)
				.build();
		ReflectionTestUtils.setField(child, "createdBy", "u1");
		ReflectionTestUtils.setField(child, "updatedBy", "u1");
		ReflectionTestUtils.setField(child, "createdAt", LocalDateTime.now());
		ReflectionTestUtils.setField(child, "updatedAt", LocalDateTime.now());

		// Root task
		ListeningTask root = ListeningTask.builder()
				.taskId(UUID.randomUUID())
				.title("RootTitle")
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_3)
				.status(Status.DRAFT)
				.build();
		ReflectionTestUtils.setField(root, "createdBy", "u1");
		ReflectionTestUtils.setField(root, "updatedBy", "u1");
		ReflectionTestUtils.setField(root, "createdAt", LocalDateTime.now().minusHours(1));
		ReflectionTestUtils.setField(root, "updatedAt", LocalDateTime.now().minusHours(1));

		Page<ListeningTask> page = new PageImpl<>(List.of(child, root), PageRequest.of(0, 10), 2);
		when(listeningTaskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
		when(listeningTaskRepository.findCurrentVersionsByIds(anyList())).thenReturn(List.of(child, root));

		Page<ListeningTaskGetResponse> res = service.getListeningTask(0, 10, null, null, null, null, null, null, null, null);
		assertEquals(2, res.getTotalElements());
		// Sorted by createdAt descending: child first
		ListeningTaskGetResponse first = res.getContent().get(0);
		ListeningTaskGetResponse second = res.getContent().get(1);
		// Child path uses parent id/title
		assertEquals(parentId, first.taskId());
		assertEquals("ParentTitle", first.title());
		// Root path remains its own id/title
		assertEquals(root.getTaskId(), second.taskId());
		assertEquals("RootTitle", second.title());
	}

	@Test
	void getTaskById_notFound_throws() {
		when(listeningTaskRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> service.getTaskById(UUID.randomUUID()));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void getTaskById_success_buildsStructuredResponse() {
		UUID taskId = UUID.randomUUID();
		UUID audioId = UUID.randomUUID();
		ListeningTask originalTask = ListeningTask.builder()
				.taskId(taskId)
				.title("Orig")
				.instruction("Instr")
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.status(Status.PUBLISHED)
				.audioFileId(audioId)
				.build();
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(originalTask));

		ListeningTask currentVersion = ListeningTask.builder()
				.taskId(taskId)
				.title("CurrTitle")
				.instruction("CurrInstr")
				.ieltsType(IeltsType.GENERAL_TRAINING)
				.partNumber(PartNumber.PART_2)
				.status(Status.PUBLISHED)
				.audioFileId(audioId)
				.transcription("T")
				.build();
		when(listeningTaskRepository.findLastestVersion(taskId)).thenReturn(currentVersion);

		UUID groupId = UUID.randomUUID();
		var originalGroup = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(originalGroup.getGroupId()).thenReturn(groupId);
		when(questionGroupRepository.findOriginalVersionByTaskId(taskId)).thenReturn(List.of(originalGroup));
		var latestGroup = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(latestGroup.getGroupId()).thenReturn(groupId); // align ids for later key lookups
		when(latestGroup.getSectionOrder()).thenReturn(3);
		when(latestGroup.getSectionLabel()).thenReturn("Sec");
		when(latestGroup.getInstruction()).thenReturn("GInstr");
		when(latestGroup.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(questionGroupRepository.findLatestVersionByOriginalId(groupId)).thenReturn(latestGroup);

		// Drag items
		UUID di1 = UUID.randomUUID();
		var drag1 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag1.getDragItemId()).thenReturn(di1);
		when(drag1.getParent()).thenReturn(null);
		when(drag1.getContent()).thenReturn("D1");
		UUID di2p = UUID.randomUUID();
		var drag2p = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag2p.getDragItemId()).thenReturn(di2p);
		var drag2 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag2.getParent()).thenReturn(drag2p);
		when(drag2.getDragItemId()).thenReturn(UUID.randomUUID());
		when(drag2.getContent()).thenReturn("D2");
		when(dragItemRepository.findCurrentVersionByGroupId(groupId)).thenReturn(List.of(drag1, drag2));

		// Questions: one original, one non-original with parent id equals child id to match mapping
		UUID qOrigId = UUID.randomUUID();
		UUID qChildId = UUID.randomUUID();
		var qOrig = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qOrig.getQuestionId()).thenReturn(qOrigId);
		when(qOrig.getParent()).thenReturn(null);
		when(qOrig.getIsOriginal()).thenReturn(true);
		when(qOrig.getQuestionOrder()).thenReturn(1);
		when(qOrig.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(qOrig.getPoint()).thenReturn(1);
		when(qOrig.getNumberOfCorrectAnswers()).thenReturn(1);
		when(qOrig.getInstructionForChoice()).thenReturn("IC");
		when(qOrig.getBlankIndex()).thenReturn(0);
		when(qOrig.getCorrectAnswer()).thenReturn("A");
		when(qOrig.getInstructionForMatching()).thenReturn("IM");
		when(qOrig.getCorrectAnswerForMatching()).thenReturn("A->1");
		when(qOrig.getZoneIndex()).thenReturn(0);

		var qParent = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qParent.getQuestionId()).thenReturn(qChildId);
		var qChild = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qChild.getQuestionId()).thenReturn(qChildId);
		when(qChild.getParent()).thenReturn(qParent);
		when(qChild.getIsOriginal()).thenReturn(false);
		when(qChild.getQuestionOrder()).thenReturn(2);
		when(qChild.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(qChild.getPoint()).thenReturn(1);
		when(qChild.getNumberOfCorrectAnswers()).thenReturn(1);
		when(qChild.getInstructionForChoice()).thenReturn("IC2");
		when(qChild.getBlankIndex()).thenReturn(0);
		when(qChild.getInstructionForMatching()).thenReturn("IM2");
		when(qChild.getCorrectAnswerForMatching()).thenReturn("B->2");
		when(qChild.getZoneIndex()).thenReturn(0);

		when(questionRepository.findOriginalVersionByGroupId(groupId)).thenReturn(List.of(qOrigId, qChildId));
		when(questionRepository.findAllCurrentVersion(List.of(qOrigId, qChildId))).thenReturn(List.of(qOrig, qChild));

		// Choices for original
		UUID cAId = UUID.randomUUID();
		var cA = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(cA.getChoiceId()).thenReturn(cAId);
		when(cA.getParent()).thenReturn(null);
		when(cA.getLabel()).thenReturn("A");
		when(cA.getChoiceOrder()).thenReturn(1);
		when(cA.getContent()).thenReturn("Alpha");
		when(cA.isCorrect()).thenReturn(true);
		// Choice with parent for mapping
		UUID cBParentId = UUID.randomUUID();
		var cBParent = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(cBParent.getChoiceId()).thenReturn(cBParentId);
		var cB = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(cB.getParent()).thenReturn(cBParent);
		when(cB.getChoiceId()).thenReturn(UUID.randomUUID());
		when(cB.getLabel()).thenReturn("B");
		when(cB.getChoiceOrder()).thenReturn(2);
		when(cB.getContent()).thenReturn("Beta");
		when(cB.isCorrect()).thenReturn(false);
		when(choiceRepository.findCurrentVersionByQuestionId(qOrigId)).thenReturn(List.of(cA, cB));

		// Choices for non-original from parent id (equals child id)
		var cC = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(cC.getParent()).thenReturn(null);
		when(cC.getChoiceId()).thenReturn(UUID.randomUUID());
		when(cC.getLabel()).thenReturn("C");
		when(cC.getChoiceOrder()).thenReturn(1);
		when(cC.getContent()).thenReturn("Gamma");
		when(cC.isCorrect()).thenReturn(false);
		when(choiceRepository.findCurrentVersionByQuestionId(qChildId)).thenReturn(List.of(cC));

		var res = service.getTaskById(taskId);

		assertEquals(taskId, res.taskId());
		assertEquals("CurrTitle", res.title());
		assertEquals("CurrInstr", res.instruction());
		assertEquals(com.fptu.sep490.listeningservice.model.enumeration.IeltsType.GENERAL_TRAINING.ordinal(), res.ieltsType());
		assertEquals(com.fptu.sep490.listeningservice.model.enumeration.PartNumber.PART_2.ordinal(), res.partNumber());
		assertEquals(audioId, res.audioFileId());
		assertEquals(com.fptu.sep490.listeningservice.model.enumeration.Status.PUBLISHED.ordinal(), res.status());
		assertEquals(1, res.questionGroups().size());
		var g = res.questionGroups().get(0);
		assertEquals(groupId, g.groupId());
		assertEquals(3, g.sectionOrder());
		assertEquals("Sec", g.sectionLabel());
		assertEquals("GInstr", g.instruction());
		assertEquals(2, g.dragItems().size());
		// parent id mapping used for second drag item
		assertEquals(di1, g.dragItems().get(0).dragItemId());
		assertEquals(di2p, g.dragItems().get(1).dragItemId());
		// questions sorted by order
		assertEquals(2, g.questions().size());
		assertEquals(1, g.questions().get(0).questionOrder());
		assertEquals(2, g.questions().get(1).questionOrder());
		// non-original question id mapped to parent id
		assertEquals(qOrigId, g.questions().get(0).questionId());
		assertEquals(qChildId, g.questions().get(1).questionId());
		// choices mapped with parent choice id when present
		assertNotNull(g.questions().get(0).choices());
		assertEquals(cAId, g.questions().get(0).choices().get(0).choiceId());
		assertEquals(cBParentId, g.questions().get(0).choices().get(1).choiceId());
	}

	@Test
	void fromListeningTask_notFound_throws() {
		when(listeningTaskRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> service.fromListeningTask(UUID.randomUUID().toString()));
		assertEquals(Constants.ErrorCode.LISTENING_TASK_NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void fromListeningTask_success_mapsGroupsQuestionsChoicesAndDragItems() {
		UUID taskId = UUID.randomUUID();
		ListeningTask task = ListeningTask.builder()
				.taskId(taskId)
				.title("T")
				.instruction("I")
				.audioFileId(UUID.randomUUID())
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_2)
				.build();
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

		// Groups
		UUID groupId = UUID.randomUUID();
		var group = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(group.getGroupId()).thenReturn(groupId);
		when(group.getSectionOrder()).thenReturn(1);
		when(group.getSectionLabel()).thenReturn("Sec");
		when(group.getInstruction()).thenReturn("GInstr");
		// associate with task part number via getListeningTask() if used
		when(group.getListeningTask()).thenReturn(task);
		when(listeningTaskRepository.findAllByIdSortedByPartNumber(anyList())).thenReturn(List.of(task));
		when(questionGroupRepository.findAllByListeningTaskByTaskId(eq(taskId))).thenReturn(List.of(group));

		// Drag items: only isCurrent=true should be included; test parent fallback
		var dragParent = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(dragParent.getDragItemId()).thenReturn(UUID.randomUUID());
		var drag1 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag1.getIsCurrent()).thenReturn(true);
		when(drag1.getParent()).thenReturn(null);
		when(drag1.getDragItemId()).thenReturn(UUID.randomUUID());
		when(drag1.getContent()).thenReturn("D1");
		var drag2 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag2.getIsCurrent()).thenReturn(true);
		when(drag2.getParent()).thenReturn(dragParent);
		when(drag2.getDragItemId()).thenReturn(UUID.randomUUID());
		when(drag2.getContent()).thenReturn("D2");
		var drag3 = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag3.getIsCurrent()).thenReturn(false); // should be filtered out
		when(dragItemRepository.findCurrentVersionsByGroupId(groupId)).thenReturn(List.of(drag1, drag2, drag3));

		// Questions
		UUID qNoParentId = UUID.randomUUID();
		var qNoParent = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qNoParent.getQuestionId()).thenReturn(qNoParentId);
		when(qNoParent.getParent()).thenReturn(null);
		when(qNoParent.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(qNoParent.getQuestionOrder()).thenReturn(1);
		when(qNoParent.getInstructionForChoice()).thenReturn("IC");
		when(qNoParent.getNumberOfCorrectAnswers()).thenReturn(1);
		when(qNoParent.getInstructionForMatching()).thenReturn("IM");
		when(qNoParent.getBlankIndex()).thenReturn(0);

		UUID qWithParentId = UUID.randomUUID();
		var qParent = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qParent.getQuestionId()).thenReturn(qWithParentId);
		var qWithParent = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qWithParent.getQuestionId()).thenReturn(qWithParentId);
		when(qWithParent.getParent()).thenReturn(qParent);
		when(qWithParent.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(qWithParent.getQuestionOrder()).thenReturn(2);
		when(qWithParent.getInstructionForChoice()).thenReturn("IC2");
		when(qWithParent.getNumberOfCorrectAnswers()).thenReturn(1);
		when(qWithParent.getInstructionForMatching()).thenReturn("IM2");
		when(qWithParent.getBlankIndex()).thenReturn(0);

		UUID qOtherId = UUID.randomUUID();
		var qOther = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qOther.getQuestionId()).thenReturn(qOtherId);
		when(qOther.getParent()).thenReturn(null);
		when(qOther.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.FILL_IN_THE_BLANKS);
		when(qOther.getQuestionOrder()).thenReturn(3);

		when(questionRepository.findCurrentVersionByGroup(groupId)).thenReturn(List.of(qNoParent, qWithParent, qOther));

		// Choices for qNoParent -> getVersionChoiceByQuestionId
		var c1 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(c1.getChoiceId()).thenReturn(UUID.randomUUID());
		when(c1.getLabel()).thenReturn("A");
		when(c1.getContent()).thenReturn("Alpha");
		when(c1.getChoiceOrder()).thenReturn(1);
		when(c1.isCorrect()).thenReturn(true);
		var c2 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(c2.getChoiceId()).thenReturn(UUID.randomUUID());
		when(c2.getLabel()).thenReturn("B");
		when(c2.getContent()).thenReturn("Beta");
		when(c2.getChoiceOrder()).thenReturn(2);
		when(c2.isCorrect()).thenReturn(false);
		when(choiceRepository.getVersionChoiceByQuestionId(qNoParentId)).thenReturn(List.of(c1, c2));

		// Choices for qWithParent -> from parent with one not current
		java.util.UUID oc1Id = java.util.UUID.randomUUID();
		java.util.UUID oc2Id = java.util.UUID.randomUUID();
		var oc1 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(oc1.getChoiceId()).thenReturn(oc1Id);
		when(oc1.getIsCurrent()).thenReturn(false);
		var oc2 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(oc2.getChoiceId()).thenReturn(oc2Id);
		when(oc2.getIsCurrent()).thenReturn(true);
		when(choiceRepository.getVersionChoiceByParentQuestionId(qWithParentId)).thenReturn(List.of(oc1, oc2));
		var curOc1 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(curOc1.getChoiceId()).thenReturn(oc1Id);
		when(curOc1.getLabel()).thenReturn("C");
		when(curOc1.getContent()).thenReturn("Gamma");
		when(curOc1.getChoiceOrder()).thenReturn(1);
		when(curOc1.isCorrect()).thenReturn(false);
		when(choiceRepository.getCurrentVersionChoiceByChoiceId(oc1Id)).thenReturn(curOc1);

		var res = service.fromListeningTask(taskId.toString());

		// Verify the repository calls
		verify(questionGroupRepository).findAllByListeningTaskByTaskId(eq(taskId));
		verify(questionRepository).findCurrentVersionByGroup(eq(groupId));
		verify(dragItemRepository).findCurrentVersionsByGroupId(eq(groupId));

		assertEquals(taskId, res.taskId());
		assertEquals(task.getTitle(), res.title());
		assertEquals(task.getInstruction(), res.instruction());
		assertEquals(task.getAudioFileId(), res.audioFileId());
		assertEquals(task.getIeltsType().ordinal(), res.ieltsType());
		assertEquals(task.getPartNumber().ordinal(), res.partNumber());
		// The service returns all groups found for the task
		System.out.println("Expected 1 group, got: " + res.questionGroups().size());
		System.out.println("Groups: " + res.questionGroups());
		assertEquals(1, res.questionGroups().size()); // We mocked only 1 group
		var g = res.questionGroups().get(0);
		assertEquals(groupId, g.questionGroupId());
		assertEquals(1, g.sectionOrder());
		assertEquals("Sec", g.sectionLabel());
		assertEquals("GInstr", g.instruction());
		// Drag items filtered to isCurrent true (2 items), parent id fallback used for second
		assertEquals(2, g.dragItems().size());
		assertEquals(drag1.getDragItemId().toString(), g.dragItems().get(0).dragItemId());
		assertEquals(dragParent.getDragItemId().toString(), g.dragItems().get(1).dragItemId());
		// Questions mapped in order 1..3
		assertEquals(3, g.questions().size());
		var orders = g.questions().stream().map(com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse::questionOrder)
				.sorted().toList();
		assertEquals(java.util.List.of(1, 2, 3), orders);
		// MC without parent has two choices
		var qOrder1 = g.questions().stream().filter(qq -> qq.questionOrder() == 1).findFirst().orElseThrow();
		assertNotNull(qOrder1.choices());
		assertEquals(2, qOrder1.choices().size());
		// MC with parent resolved current choice via getCurrentVersionChoiceByChoiceId
		var qOrder2 = g.questions().stream().filter(qq -> qq.questionOrder() == 2).findFirst().orElseThrow();
		assertNotNull(qOrder2.choices());
	}
}
