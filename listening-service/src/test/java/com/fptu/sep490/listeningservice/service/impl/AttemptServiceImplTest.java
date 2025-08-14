package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Attempt;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.listeningservice.viewmodel.response.UserAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AttemptServiceImplTest {

	@InjectMocks
	AttemptServiceImpl attemptService;
	@Mock ListeningTaskRepository listeningTaskRepository;
	@Mock QuestionGroupRepository questionGroupRepository;
	@Mock DragItemRepository dragItemRepository;
	@Mock QuestionRepository questionRepository;
	@Mock ChoiceRepository choiceRepository;
	@Mock AttemptRepository attemptRepository;
	@Mock AnswerAttemptRepository answerAttemptRepository;
	@Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;
	@Mock KeyCloakTokenClient keyCloakTokenClient;
	@Mock KeyCloakUserClient keyCloakUserClient;
	@Mock RedisService redisService;
	@Mock Helper helper;

	@Mock HttpServletRequest request;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(attemptService, "realm", "realm");
		ReflectionTestUtils.setField(attemptService, "clientId", "client");
		ReflectionTestUtils.setField(attemptService, "clientSecret", "secret");
	}

	@Test
	void createAttempt_taskNotFound_throws() {
		UUID taskId = UUID.randomUUID();
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> attemptService.createAttempt(taskId, request));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void createAttempt_taskNotActivated_throws() {
		UUID taskId = UUID.randomUUID();
		ListeningTask original = mock(ListeningTask.class);
		when(original.getTaskId()).thenReturn(taskId);
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(original));
		ListeningTask current = mock(ListeningTask.class);
		when(current.getStatus()).thenReturn(Status.DRAFT); // not PUBLISHED
		when(listeningTaskRepository.findLastestVersion(taskId)).thenReturn(current);
		AppException ex = assertThrows(AppException.class, () -> attemptService.createAttempt(taskId, request));
		assertEquals(Constants.ErrorCode.LISTENING_TASK_NOT_ACTIVATED, ex.getBusinessErrorCode());
	}

	@Test
	void createAttempt_success_buildsResponse() throws Exception {
		// Arrange
		UUID taskId = UUID.randomUUID();
		String userId = "user-1";
		when(helper.getUserIdFromToken(request)).thenReturn(userId);

		// Original task
		var originalTask = mock(ListeningTask.class);
		when(originalTask.getTaskId()).thenReturn(taskId);
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(originalTask));

		// Current version (PUBLISHED)
		var currentTask = mock(ListeningTask.class);
		when(currentTask.getTaskId()).thenReturn(taskId);
		when(currentTask.getStatus()).thenReturn(Status.PUBLISHED);
		when(currentTask.getTitle()).thenReturn("Listening Title");
		when(currentTask.getInstruction()).thenReturn("Do it");
		when(currentTask.getIeltsType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.IeltsType.ACADEMIC);
		when(currentTask.getPartNumber()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.PartNumber.PART_1);
		UUID audioId = UUID.randomUUID();
		when(currentTask.getAudioFileId()).thenReturn(audioId);
		when(listeningTaskRepository.findLastestVersion(taskId)).thenReturn(currentTask);

		// Question group
		UUID groupId = UUID.randomUUID();
		var originalGroup = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(originalGroup.getGroupId()).thenReturn(groupId);
		when(questionGroupRepository.findOriginalVersionByTaskId(taskId)).thenReturn(List.of(originalGroup));
		var latestGroup = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(latestGroup.getGroupId()).thenReturn(groupId); // align ids so map lookups succeed
		when(latestGroup.getSectionOrder()).thenReturn(2);
		when(latestGroup.getSectionLabel()).thenReturn("Section B");
		when(latestGroup.getInstruction()).thenReturn("Listen carefully");
		when(questionGroupRepository.findLatestVersionByOriginalId(groupId)).thenReturn(latestGroup);

		// Drag items
		UUID dragId = UUID.randomUUID();
		var dragItem = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(dragItem.getDragItemId()).thenReturn(dragId);
		when(dragItem.getContent()).thenReturn("Drag Content");
		when(dragItemRepository.findCurrentVersionByGroupId(groupId)).thenReturn(List.of(dragItem));

		// Questions: one original, one non-original
		UUID qOrigId = UUID.randomUUID();
		UUID qChildId = UUID.randomUUID();
		var qOrig = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qOrig.getQuestionId()).thenReturn(qOrigId);
		when(qOrig.getIsOriginal()).thenReturn(true);
		when(qOrig.getQuestionOrder()).thenReturn(1);
		when(qOrig.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(qOrig.getNumberOfCorrectAnswers()).thenReturn(1);
		when(qOrig.getBlankIndex()).thenReturn(0);
		when(qOrig.getInstructionForChoice()).thenReturn("Choose");
		when(qOrig.getInstructionForMatching()).thenReturn("Match");
		when(qOrig.getZoneIndex()).thenReturn(0);

		var qParent = mock(com.fptu.sep490.listeningservice.model.Question.class);
		// Ensure parent id equals child id so the mapping key matches retrieval key
		when(qParent.getQuestionId()).thenReturn(qChildId);
		var qChild = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qChild.getQuestionId()).thenReturn(qChildId);
		when(qChild.getIsOriginal()).thenReturn(false);
		when(qChild.getParent()).thenReturn(qParent);
		when(qChild.getQuestionOrder()).thenReturn(2);
		when(qChild.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(qChild.getNumberOfCorrectAnswers()).thenReturn(1);
		when(qChild.getBlankIndex()).thenReturn(0);
		when(qChild.getInstructionForChoice()).thenReturn("Choose 2");
		when(qChild.getInstructionForMatching()).thenReturn("Match 2");
		when(qChild.getZoneIndex()).thenReturn(1);

		when(questionRepository.findOriginalVersionByGroupId(groupId)).thenReturn(List.of(qOrigId, qChildId));
		when(questionRepository.findAllCurrentVersion(List.of(qOrigId, qChildId))).thenReturn(List.of(qOrig, qChild));

		// Choices for questions
		UUID choice1 = UUID.randomUUID();
		UUID choice2 = UUID.randomUUID();
		var c1 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(c1.getChoiceId()).thenReturn(choice1);
		when(c1.getLabel()).thenReturn("A");
		when(c1.getContent()).thenReturn("Alpha");
		var c2 = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(c2.getChoiceId()).thenReturn(choice2);
		when(c2.getLabel()).thenReturn("B");
		when(c2.getContent()).thenReturn("Beta");
		when(choiceRepository.findCurrentVersionByQuestionId(qOrigId)).thenReturn(List.of(c1, c2));
		when(choiceRepository.findCurrentVersionByQuestionId(qOrigId)).thenReturn(List.of(c1, c2));
		when(choiceRepository.findCurrentVersionByQuestionId(qParent.getQuestionId())).thenReturn(List.of(c1));

		// objectMapper serialization for attemptVersion
		when(objectMapper.writeValueAsString(any())).thenReturn("version-json");

		// saved attempt
		var savedAttempt = mock(Attempt.class);
		UUID attemptId = UUID.randomUUID();
		when(savedAttempt.getAttemptId()).thenReturn(attemptId);
		when(attemptRepository.save(any(Attempt.class))).thenReturn(savedAttempt);

		// Act
		var response = attemptService.createAttempt(taskId, request);

		// Assert
		assertEquals(taskId, response.taskId());
		assertEquals(attemptId, response.attemptId());
		assertEquals("Listening Title", response.title());
		assertEquals("Do it", response.instruction());
		assertEquals(audioId, response.audioFileId());
		assertEquals(1, response.questionGroups().size());
		var groupResp = response.questionGroups().get(0);
		assertEquals(groupId, groupResp.questionGroupId());
		assertEquals("Section B", groupResp.sectionLabel());
		assertEquals("Listen carefully", groupResp.instruction());
		assertEquals(1, groupResp.dragItems().size());
		assertEquals("Drag Content", groupResp.dragItems().get(0).content());
		assertEquals(2, groupResp.questions().size());
		// questions sorted by questionOrder
		assertEquals(1, groupResp.questions().get(0).questionOrder());
		assertEquals(2, groupResp.questions().get(1).questionOrder());
	}

	@Test
	void saveAttempt_attemptNotFound_throws() {
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> attemptService.saveAttempt(UUID.randomUUID().toString(), request, mock(SavedAnswersRequestList.class)));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void saveAttempt_forbidden_throws() {
		Attempt attempt = mock(Attempt.class);
		UUID attemptId = UUID.randomUUID();
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("user-1");
		when(attempt.getCreatedBy()).thenReturn("other-user");
		AppException ex = assertThrows(AppException.class, () -> attemptService.saveAttempt(attemptId.toString(), request, mock(SavedAnswersRequestList.class)));
		assertEquals(Constants.ErrorCode.FORBIDDEN, ex.getBusinessErrorCode());
	}

	@Test
	void saveAttempt_notDraft_throws() {
		Attempt attempt = mock(Attempt.class);
		UUID attemptId = UUID.randomUUID();
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("user-1");
		when(attempt.getCreatedBy()).thenReturn("user-1");
		when(attempt.getStatus()).thenReturn(Status.FINISHED);
		AppException ex = assertThrows(AppException.class, () -> attemptService.saveAttempt(attemptId.toString(), request, mock(SavedAnswersRequestList.class)));
		assertEquals(Constants.ErrorCode.ATTEMPT_NOT_DRAFT, ex.getBusinessErrorCode());
	}

	@Test
	void saveAttempt_success_updatesAnswersForAllTypes() {
		// Arrange
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("user-1");
		when(attempt.getCreatedBy()).thenReturn("user-1");
		when(attempt.getStatus()).thenReturn(Status.DRAFT);

		// Questions IDs
		UUID qMC = UUID.randomUUID();
		UUID qFB = UUID.randomUUID();
		UUID qMT = UUID.randomUUID();
		UUID qDD = UUID.randomUUID();

		// Saved answers
		var ansMC = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansMC.questionId()).thenReturn(qMC);
		when(ansMC.choices()).thenReturn(java.util.List.of(UUID.randomUUID()));
		var ansFB = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansFB.questionId()).thenReturn(qFB);
		when(ansFB.dataFilled()).thenReturn("filled");
		var ansMT = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansMT.questionId()).thenReturn(qMT);
		when(ansMT.dataMatched()).thenReturn("matched");
		var ansDD = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansDD.questionId()).thenReturn(qDD);
		when(ansDD.dragItemId()).thenReturn(UUID.randomUUID());

		var answers = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList.class);
		when(answers.duration()).thenReturn(123L);
		when(answers.answers()).thenReturn(java.util.List.of(ansMC, ansFB, ansMT, ansDD));

		// Questions
		var q1 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q1.getQuestionId()).thenReturn(qMC);
		when(q1.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		var q2 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q2.getQuestionId()).thenReturn(qFB);
		when(q2.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.FILL_IN_THE_BLANKS);
		var q3 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q3.getQuestionId()).thenReturn(qMT);
		when(q3.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MATCHING);
		var q4 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q4.getQuestionId()).thenReturn(qDD);
		when(q4.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.DRAG_AND_DROP);

		when(questionRepository.findById(qMC)).thenReturn(Optional.of(q1));
		when(questionRepository.findById(qFB)).thenReturn(Optional.of(q2));
		when(questionRepository.findById(qMT)).thenReturn(Optional.of(q3));
		when(questionRepository.findById(qDD)).thenReturn(Optional.of(q4));

		when(answerAttemptRepository.findAnswerAttemptById(any())).thenReturn(null);
		when(answerAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act / Assert
		assertDoesNotThrow(() -> attemptService.saveAttempt(attemptId.toString(), request, answers));
		// Saved for each answer
		verify(answerAttemptRepository, times(4)).save(any());
		// Attempt saved updates
		verify(attemptRepository, atLeastOnce()).save(any());
	}

	@Test
	void saveAttempt_questionNotFound_throws() {
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("user-1");
		when(attempt.getCreatedBy()).thenReturn("user-1");
		when(attempt.getStatus()).thenReturn(Status.DRAFT);

		UUID missingQ = UUID.randomUUID();
		var ans = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ans.questionId()).thenReturn(missingQ);
		var answers = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList.class);
		when(answers.answers()).thenReturn(java.util.List.of(ans));

		when(questionRepository.findById(missingQ)).thenReturn(Optional.empty());

		AppException ex = assertThrows(AppException.class, () -> attemptService.saveAttempt(attemptId.toString(), request, answers));
		assertEquals(Constants.ErrorCode.QUESTION_NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void loadAttempt_attemptNotFound_throws() {
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> attemptService.loadAttempt(UUID.randomUUID().toString(), request));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void loadAttempt_forbidden_throws() {
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u2");
		AppException ex = assertThrows(AppException.class, () -> attemptService.loadAttempt(UUID.randomUUID().toString(), request));
		assertEquals(Constants.ErrorCode.FORBIDDEN, ex.getBusinessErrorCode());
	}

	@Test
	void loadAttempt_notDraft_throws() {
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.FINISHED);
		AppException ex = assertThrows(AppException.class, () -> attemptService.loadAttempt(UUID.randomUUID().toString(), request));
		assertEquals(Constants.ErrorCode.ATTEMPT_NOT_DRAFT, ex.getBusinessErrorCode());
	}

	@Test
	void loadAttempt_success_buildsResponse() throws Exception {
		// Arrange
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.DRAFT);
		when(attempt.getAttemptId()).thenReturn(attemptId);
		when(attempt.getVersion()).thenReturn("version-json");
		when(attempt.getTotalPoints()).thenReturn(5);
		when(attempt.getDuration()).thenReturn(99L);

		// AttemptVersion mapping
		UUID taskId = UUID.randomUUID();
		UUID groupId = UUID.randomUUID();
		UUID qMC = UUID.randomUUID();
		UUID qOther = UUID.randomUUID();
		UUID dragId = UUID.randomUUID();
		var qvMC = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder()
				.questionId(qMC)
				.choiceMapping(java.util.List.of(UUID.randomUUID()))
				.build();
		var qvOther = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder()
				.questionId(qOther)
				.choiceMapping(java.util.List.of())
				.build();
		java.util.Map<UUID, java.util.List<com.fptu.sep490.listeningservice.model.json.QuestionVersion>> groupMapQ = new java.util.HashMap<>();
		groupMapQ.put(groupId, java.util.List.of(qvMC, qvOther));
		java.util.Map<UUID, java.util.List<UUID>> groupMapDrag = new java.util.HashMap<>();
		groupMapDrag.put(groupId, java.util.List.of(dragId));
		var version = com.fptu.sep490.listeningservice.model.json.AttemptVersion.builder()
				.taskId(taskId)
				.groupMappingQuestion(groupMapQ)
				.groupMappingDragItem(groupMapDrag)
				.build();
		var node = mock(com.fasterxml.jackson.databind.JsonNode.class);
		when(objectMapper.readTree("version-json")).thenReturn(node);
		when(objectMapper.treeToValue(node, com.fptu.sep490.listeningservice.model.json.AttemptVersion.class)).thenReturn(version);

		// ListeningTask lookup
		ListeningTask task = mock(ListeningTask.class);
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
		when(task.getTaskId()).thenReturn(taskId);
		when(task.getIeltsType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.IeltsType.GENERAL_TRAINING);
		when(task.getPartNumber()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.PartNumber.PART_2);
		when(task.getInstruction()).thenReturn("Instr");
		when(task.getTitle()).thenReturn("Title");
		when(task.getAudioFileId()).thenReturn(UUID.randomUUID());

		// Group and questions
		var group = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(group.getGroupId()).thenReturn(groupId);
		when(group.getSectionOrder()).thenReturn(1);
		when(group.getSectionLabel()).thenReturn("Sec");
		when(group.getInstruction()).thenReturn("GInstr");
		when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

		var q1 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q1.getQuestionId()).thenReturn(qMC);
		when(q1.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(q1.getQuestionOrder()).thenReturn(2);
		when(q1.getPoint()).thenReturn(1);
		when(q1.getNumberOfCorrectAnswers()).thenReturn(1);
		when(q1.getInstructionForMatching()).thenReturn("IM");
		when(q1.getZoneIndex()).thenReturn(0);
		when(questionRepository.findById(qMC)).thenReturn(Optional.of(q1));

		var q2 = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(q2.getQuestionId()).thenReturn(qOther);
		when(q2.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.FILL_IN_THE_BLANKS);
		when(q2.getQuestionOrder()).thenReturn(1);
		when(q2.getPoint()).thenReturn(1);
		when(q2.getNumberOfCorrectAnswers()).thenReturn(1);
		when(q2.getInstructionForMatching()).thenReturn("IM");
		when(q2.getZoneIndex()).thenReturn(0);
		when(questionRepository.findById(qOther)).thenReturn(Optional.of(q2));

		// Choices for MC
		UUID ch1 = UUID.randomUUID();
		var choice = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(choice.getChoiceId()).thenReturn(ch1);
		when(choice.getLabel()).thenReturn("A");
		when(choice.getChoiceOrder()).thenReturn(1);
		when(choice.getContent()).thenReturn("Alpha");
		when(choiceRepository.findAllById(anyList())).thenReturn(java.util.List.of(choice));

		// Drag items
		var di = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(di.getDragItemId()).thenReturn(dragId);
		when(di.getContent()).thenReturn("Drag");
		when(dragItemRepository.findAllById(java.util.List.of(dragId))).thenReturn(java.util.List.of(di));

		// Existing answers
		var aa = mock(com.fptu.sep490.listeningservice.model.AnswerAttempt.class);
		when(aa.getQuestion()).thenReturn(q1);
		when(aa.getDragItemId()).thenReturn(dragId);
		when(aa.getDataFilled()).thenReturn("filled");
		when(aa.getDataMatched()).thenReturn("matched");
		when(aa.getChoices()).thenReturn(java.util.List.of(ch1));
		when(answerAttemptRepository.findByAttempt(attempt)).thenReturn(java.util.List.of(aa));

		// Act
		var result = attemptService.loadAttempt(attemptId.toString(), request);

		// Assert
		assertEquals(attemptId, result.attemptId());
		assertEquals(5, result.totalPoints());
		assertEquals(99L, result.duration());
		assertFalse(result.answers().isEmpty());
		assertNotNull(result.attemptResponse());
	}

	@Test
	void submitAttempt_attemptNotFound_throws() {
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> attemptService.submitAttempt(UUID.randomUUID().toString(), request, mock(SavedAnswersRequestList.class)));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void submitAttempt_forbidden_throws() {
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u2");
		AppException ex = assertThrows(AppException.class, () -> attemptService.submitAttempt(UUID.randomUUID().toString(), request, mock(SavedAnswersRequestList.class)));
		assertEquals(Constants.ErrorCode.FORBIDDEN, ex.getBusinessErrorCode());
	}

	@Test
	void submitAttempt_alreadySubmitted_throws() {
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.FINISHED);
		AppException ex = assertThrows(AppException.class, () -> attemptService.submitAttempt(UUID.randomUUID().toString(), request, mock(SavedAnswersRequestList.class)));
		assertEquals(Constants.ErrorCode.ATTEMPT_ALREADY_SUBMITTED, ex.getBusinessErrorCode());
	}

	@Test
	void submitAttempt_success_scoresAndReturnsResponse() throws Exception {
		// Arrange
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.DRAFT);
		when(attempt.getAttemptId()).thenReturn(attemptId);
		when(attempt.getVersion()).thenReturn("ver-json");

		// AttemptVersion with 2 questions
		UUID groupId = UUID.randomUUID();
		UUID qMC = UUID.randomUUID();
		UUID qFB = UUID.randomUUID();
		var qvMC = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder().questionId(qMC).build();
		var qvFB = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder().questionId(qFB).build();
		java.util.Map<UUID, java.util.List<com.fptu.sep490.listeningservice.model.json.QuestionVersion>> groupMapQ = new java.util.HashMap<>();
		groupMapQ.put(groupId, java.util.List.of(qvMC, qvFB));
		var version = com.fptu.sep490.listeningservice.model.json.AttemptVersion.builder()
				.groupMappingQuestion(groupMapQ)
				.build();
		var node = mock(com.fasterxml.jackson.databind.JsonNode.class);
		when(objectMapper.readTree("ver-json")).thenReturn(node);
		when(objectMapper.treeToValue(node, com.fptu.sep490.listeningservice.model.json.AttemptVersion.class)).thenReturn(version);

		// Questions
		var mc = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(mc.getQuestionId()).thenReturn(qMC);
		when(mc.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(mc.getIsOriginal()).thenReturn(true);
		when(mc.getQuestionOrder()).thenReturn(2);
		when(mc.getExplanation()).thenReturn("e1");
		when(mc.getPoint()).thenReturn(2);

		var fb = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(fb.getQuestionId()).thenReturn(qFB);
		when(fb.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.FILL_IN_THE_BLANKS);
		when(fb.getQuestionOrder()).thenReturn(1);
		when(fb.getExplanation()).thenReturn("e2");
		when(fb.getCorrectAnswer()).thenReturn("filled");
		when(fb.getPoint()).thenReturn(3);

		when(questionRepository.findQuestionsByIds(anyList())).thenReturn(java.util.List.of(mc, fb));

		// MC scoring dependencies
		UUID correctChoiceId = UUID.randomUUID();
		var origChoice = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(origChoice.getChoiceId()).thenReturn(correctChoiceId);
		when(choiceRepository.getOriginalChoiceByOriginalQuestion(qMC)).thenReturn(java.util.List.of(origChoice));
		var correctChoice = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(correctChoice.getChoiceId()).thenReturn(correctChoiceId);
		when(correctChoice.getLabel()).thenReturn("A");
		when(choiceRepository.getCurrentCorrectChoice(anyList())).thenReturn(java.util.List.of(correctChoice));
		when(choiceRepository.getChoicesByIds(anyList())).thenReturn(java.util.List.of("A"));

		// Saved answers
		var ansMC = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansMC.questionId()).thenReturn(qMC);
		when(ansMC.choices()).thenReturn(java.util.List.of(correctChoiceId));
		var ansFB = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansFB.questionId()).thenReturn(qFB);
		when(ansFB.dataFilled()).thenReturn("filled");
		var answerList = java.util.List.of(ansMC, ansFB);
		var answers = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList.class);
		when(answers.answers()).thenReturn(answerList);
		when(answers.duration()).thenReturn(77L);

		when(answerAttemptRepository.findAnswerAttemptByAttemptId(any())).thenReturn(java.util.Optional.empty());
		when(answerAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act
		var result = attemptService.submitAttempt(attemptId.toString(), request, answers);

		// Assert
		assertEquals(77L, result.getDuration());
		// total points = 2 (MC correct) + 3 (FB correct)
		assertEquals(5, result.getTotalPoints());
		assertEquals(2, result.getResultSets().size());
		// sorted by questionIndex
		assertEquals(1, result.getResultSets().get(0).getQuestionIndex());
		assertEquals(2, result.getResultSets().get(1).getQuestionIndex());
	}

	@Test
	void submitAttempt_incorrectMatchingAndDragDrop_existingAnswerAttempt() throws Exception {
		// Arrange
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.DRAFT);
		when(attempt.getAttemptId()).thenReturn(attemptId);
		when(attempt.getVersion()).thenReturn("ver-json-2");

		// AttemptVersion with 2 questions: MATCHING and DRAG_AND_DROP
		UUID groupId = UUID.randomUUID();
		UUID qMT = UUID.randomUUID();
		UUID qDD = UUID.randomUUID();
		var qvMT = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder().questionId(qMT).build();
		var qvDD = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder().questionId(qDD).build();
		java.util.Map<UUID, java.util.List<com.fptu.sep490.listeningservice.model.json.QuestionVersion>> groupMapQ = new java.util.HashMap<>();
		groupMapQ.put(groupId, java.util.List.of(qvMT, qvDD));
		var version = com.fptu.sep490.listeningservice.model.json.AttemptVersion.builder()
				.groupMappingQuestion(groupMapQ)
				.build();
		var node = mock(com.fasterxml.jackson.databind.JsonNode.class);
		when(objectMapper.readTree("ver-json-2")).thenReturn(node);
		when(objectMapper.treeToValue(node, com.fptu.sep490.listeningservice.model.json.AttemptVersion.class)).thenReturn(version);

		// Questions
		var qm = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qm.getQuestionId()).thenReturn(qMT);
		when(qm.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MATCHING);
		when(qm.getQuestionOrder()).thenReturn(1);
		when(qm.getExplanation()).thenReturn("em");
		when(qm.getCorrectAnswer()).thenReturn("correct");
		when(qm.getPoint()).thenReturn(2);

		var qd = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(qd.getQuestionId()).thenReturn(qDD);
		when(qd.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.DRAG_AND_DROP);
		when(qd.getQuestionOrder()).thenReturn(2);
		when(qd.getExplanation()).thenReturn("ed");
		var drag = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		UUID correctDragId = UUID.randomUUID();
		when(drag.getDragItemId()).thenReturn(correctDragId);
		when(drag.getContent()).thenReturn("dragContent");
		when(qd.getDragItem()).thenReturn(drag);
		when(qd.getPoint()).thenReturn(3);

		when(questionRepository.findQuestionsByIds(anyList())).thenReturn(java.util.List.of(qm, qd));

		// Saved answers incorrect
		var ansMT = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansMT.questionId()).thenReturn(qMT);
		when(ansMT.dataMatched()).thenReturn("wrong");
		when(ansMT.dataFilled()).thenReturn("wrong");
		var ansDD = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest.class);
		when(ansDD.questionId()).thenReturn(qDD);
		when(ansDD.dragItemId()).thenReturn(UUID.randomUUID()); // not equal to correctDragId
		when(ansDD.dataFilled()).thenReturn("drag");
		var answers = mock(com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList.class);
		when(answers.answers()).thenReturn(java.util.List.of(ansMT, ansDD));
		when(answers.duration()).thenReturn(55L);

		// Existing AnswerAttempt present
		when(answerAttemptRepository.findAnswerAttemptByAttemptId(any())).thenReturn(java.util.Optional.of(mock(com.fptu.sep490.listeningservice.model.AnswerAttempt.class)));
		when(answerAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act
		var result = attemptService.submitAttempt(attemptId.toString(), request, answers);

		// Assert
		assertEquals(55L, result.getDuration());
		assertEquals(0, result.getTotalPoints()); // both incorrect
		assertEquals(2, result.getResultSets().size());
		assertFalse(result.getResultSets().get(0).isCorrect());
		assertFalse(result.getResultSets().get(1).isCorrect());
	}

	@Test
	void getAttemptByUser_mapsPageContent() {
		when(helper.getUserIdFromToken(request)).thenReturn("user-1");
		Attempt a = mock(Attempt.class);
		ListeningTask task = mock(ListeningTask.class);
		when(a.getAttemptId()).thenReturn(UUID.randomUUID());
		when(a.getDuration()).thenReturn(120L);
		when(a.getTotalPoints()).thenReturn(10);
		when(a.getStatus()).thenReturn(Status.DRAFT);
		when(a.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
		when(a.getFinishedAt()).thenReturn(null);
		when(a.getListeningTask()).thenReturn(task);
		when(task.getTaskId()).thenReturn(UUID.randomUUID());
		when(task.getTitle()).thenReturn("Title");
		Page<Attempt> page = new PageImpl<>(List.of(a), PageRequest.of(0, 10), 1);
		when(attemptRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

		Page<?> res = attemptService.getAttemptByUser(0, 10, null, null, null, "createdAt", "DESC", "Title", null, request);
		assertEquals(1, res.getTotalElements());
		UserAttemptResponse first = (UserAttemptResponse) res.getContent().get(0);
		assertEquals("Title", first.title());
	}

	@Test
	void viewResult_attemptNotFound_throws() {
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		AppException ex = assertThrows(AppException.class, () -> attemptService.viewResult(UUID.randomUUID(), request));
		assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
	}

	@Test
	void viewResult_forbidden_throws() {
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u2");
		AppException ex = assertThrows(AppException.class, () -> attemptService.viewResult(UUID.randomUUID(), request));
		assertEquals(Constants.ErrorCode.FORBIDDEN, ex.getBusinessErrorCode());
	}

	@Test
	void viewResult_notFinished_throws() {
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.DRAFT);
		AppException ex = assertThrows(AppException.class, () -> attemptService.viewResult(UUID.randomUUID(), request));
		assertEquals(Constants.ErrorCode.ATTEMPT_NOT_FINISHED, ex.getBusinessErrorCode());
	}

	@Test
	void viewResult_success_buildsResponse() throws Exception {
		// Arrange
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = mock(Attempt.class);
		when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(request)).thenReturn("u1");
		when(attempt.getCreatedBy()).thenReturn("u1");
		when(attempt.getStatus()).thenReturn(Status.FINISHED);
		when(attempt.getAttemptId()).thenReturn(attemptId);
		when(attempt.getVersion()).thenReturn("vr-json");
		when(attempt.getTotalPoints()).thenReturn(7);
		when(attempt.getDuration()).thenReturn(66L);

		// AttemptVersion
		UUID taskId = UUID.randomUUID();
		UUID groupId = UUID.randomUUID();
		UUID qMC = UUID.randomUUID();
		UUID qMT = UUID.randomUUID();
		UUID qDD = UUID.randomUUID();
		UUID choiceAId = UUID.randomUUID();
		UUID choiceBId = UUID.randomUUID();
		UUID dragId = UUID.randomUUID();
		var qvMC = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder()
				.questionId(qMC)
				.choiceMapping(java.util.List.of(choiceAId, choiceBId))
				.build();
		var qvMT = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder()
				.questionId(qMT)
				.choiceMapping(java.util.List.of())
				.build();
		var qvDD = com.fptu.sep490.listeningservice.model.json.QuestionVersion.builder()
				.questionId(qDD)
				.choiceMapping(java.util.List.of())
				.build();
		java.util.Map<UUID, java.util.List<com.fptu.sep490.listeningservice.model.json.QuestionVersion>> groupMapQ = new java.util.HashMap<>();
		groupMapQ.put(groupId, java.util.List.of(qvMC, qvMT, qvDD));
		java.util.Map<UUID, java.util.List<UUID>> groupMapDrag = new java.util.HashMap<>();
		groupMapDrag.put(groupId, java.util.List.of(dragId));
		var version = com.fptu.sep490.listeningservice.model.json.AttemptVersion.builder()
				.taskId(taskId)
				.groupMappingQuestion(groupMapQ)
				.groupMappingDragItem(groupMapDrag)
				.build();
		var node = mock(com.fasterxml.jackson.databind.JsonNode.class);
		when(objectMapper.readTree("vr-json")).thenReturn(node);
		when(objectMapper.treeToValue(node, com.fptu.sep490.listeningservice.model.json.AttemptVersion.class)).thenReturn(version);

		// ListeningTask
		ListeningTask task = mock(ListeningTask.class);
		when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
		when(task.getTaskId()).thenReturn(taskId);
		when(task.getIeltsType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.IeltsType.ACADEMIC);
		when(task.getPartNumber()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.PartNumber.PART_3);
		when(task.getInstruction()).thenReturn("Instr");
		when(task.getTitle()).thenReturn("Title");
		when(task.getAudioFileId()).thenReturn(UUID.randomUUID());

		// Group
		var group = mock(com.fptu.sep490.listeningservice.model.QuestionGroup.class);
		when(group.getGroupId()).thenReturn(groupId);
		when(group.getSectionOrder()).thenReturn(1);
		when(group.getSectionLabel()).thenReturn("Sec");
		when(group.getInstruction()).thenReturn("GInstr");
		when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

		// Questions
		var mc = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(mc.getQuestionId()).thenReturn(qMC);
		when(mc.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MULTIPLE_CHOICE);
		when(mc.getQuestionOrder()).thenReturn(2);
		when(mc.getPoint()).thenReturn(2);
		when(mc.getNumberOfCorrectAnswers()).thenReturn(1);
		when(mc.getInstructionForMatching()).thenReturn("IM");
		when(mc.getZoneIndex()).thenReturn(0);

		var mt = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(mt.getQuestionId()).thenReturn(qMT);
		when(mt.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.MATCHING);
		when(mt.getQuestionOrder()).thenReturn(1);
		when(mt.getPoint()).thenReturn(3);
		when(mt.getNumberOfCorrectAnswers()).thenReturn(1);
		when(mt.getInstructionForMatching()).thenReturn("IM2");
		when(mt.getZoneIndex()).thenReturn(0);
		when(mt.getCorrectAnswerForMatching()).thenReturn("A->1");

		var dd = mock(com.fptu.sep490.listeningservice.model.Question.class);
		when(dd.getQuestionId()).thenReturn(qDD);
		when(dd.getQuestionType()).thenReturn(com.fptu.sep490.listeningservice.model.enumeration.QuestionType.DRAG_AND_DROP);
		when(dd.getQuestionOrder()).thenReturn(3);
		when(dd.getPoint()).thenReturn(1);
		when(dd.getNumberOfCorrectAnswers()).thenReturn(1);
		when(dd.getInstructionForMatching()).thenReturn("IM3");
		when(dd.getZoneIndex()).thenReturn(0);
		var drag = mock(com.fptu.sep490.listeningservice.model.DragItem.class);
		when(drag.getDragItemId()).thenReturn(dragId);
		when(drag.getContent()).thenReturn("DragC");
		when(dd.getDragItem()).thenReturn(drag);

		when(questionRepository.findById(qMC)).thenReturn(Optional.of(mc));
		when(questionRepository.findById(qMT)).thenReturn(Optional.of(mt));
		when(questionRepository.findById(qDD)).thenReturn(Optional.of(dd));

		// Choices for MC including isCorrect
		var cA = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(cA.getChoiceId()).thenReturn(choiceAId);
		when(cA.getLabel()).thenReturn("A");
		when(cA.getChoiceOrder()).thenReturn(1);
		when(cA.getContent()).thenReturn("Alpha");
		when(cA.isCorrect()).thenReturn(true);
		var cB = mock(com.fptu.sep490.listeningservice.model.Choice.class);
		when(cB.getChoiceId()).thenReturn(choiceBId);
		when(cB.getLabel()).thenReturn("B");
		when(cB.getChoiceOrder()).thenReturn(2);
		when(cB.getContent()).thenReturn("Beta");
		when(cB.isCorrect()).thenReturn(false);
		when(choiceRepository.findAllById(anyList())).thenReturn(java.util.List.of(cA, cB));

		// Drag item lookup
		when(dragItemRepository.findAllById(java.util.List.of(dragId))).thenReturn(java.util.List.of(drag));

		// Existing answers
		var aa1 = mock(com.fptu.sep490.listeningservice.model.AnswerAttempt.class);
		when(aa1.getQuestion()).thenReturn(mc);
		when(aa1.getChoices()).thenReturn(java.util.List.of(choiceAId));
		var aa2 = mock(com.fptu.sep490.listeningservice.model.AnswerAttempt.class);
		when(aa2.getQuestion()).thenReturn(mt);
		when(aa2.getDataMatched()).thenReturn("A->2");
		var aa3 = mock(com.fptu.sep490.listeningservice.model.AnswerAttempt.class);
		when(aa3.getQuestion()).thenReturn(dd);
		when(aa3.getDragItemId()).thenReturn(dragId);
		when(answerAttemptRepository.findByAttempt(attempt)).thenReturn(java.util.List.of(aa1, aa2, aa3));

		// Act
		var result = attemptService.viewResult(attemptId, request);

		// Assert
		assertEquals(attemptId, result.attemptId());
		assertEquals(7, result.totalPoints());
		assertEquals(66L, result.duration());
		assertNotNull(result.attemptResponse());
		assertEquals(1, result.attemptResponse().questionGroups().size());
		var qList = result.attemptResponse().questionGroups().get(0).questions();
		assertEquals(3, qList.size());
		// ordered by questionOrder: MATCHING(1), MC(2), DRAG(3)
		assertEquals(1, qList.get(0).questionOrder());
		assertEquals(2, qList.get(1).questionOrder());
		assertEquals(3, qList.get(2).questionOrder());
		// MC choices carry isCorrect
		var mcResp = qList.get(1);
		assertNotNull(mcResp.choices());
		assertTrue(mcResp.choices().stream().anyMatch(c -> c.isCorrect()));
		assertTrue(mcResp.choices().stream().anyMatch(c -> !c.isCorrect()));
		// Matching exposes correctAnswerForMatching
		assertEquals("A->1", qList.get(0).correctAnswerForMatching());
		// Drag and drop exposes dragItemId
		assertEquals(dragId, qList.get(2).dragItemId());
	}
}
