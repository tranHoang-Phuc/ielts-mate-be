package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import com.fptu.sep490.readingservice.model.enumeration.PartNumber;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.model.json.AttemptVersion;
import com.fptu.sep490.readingservice.model.json.QuestionVersion;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.service.impl.AttemptServiceImpl;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

class AttemptServiceImplTest {

	@Mock
	ReadingPassageRepository readingPassageRepository;
	@Mock
	AttemptRepository attemptRepository;
	@Mock
	QuestionGroupRepository questionGroupRepository;
	@Mock
	QuestionRepository questionRepository;
	@Mock
	DragItemRepository dragItemRepository;
	@Mock
    ChoiceRepository choiceRepository;
    @Mock
    AnswerAttemptRepository answerAttemptRepository;
	@Mock
	ObjectMapper objectMapper;
	@Mock
    KeyCloakTokenClient keyCloakTokenClient;
	@Mock
    KeyCloakUserClient keyCloakUserClient;
	@Mock
    RedisService redisService;
	@Mock
	org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
	@Mock
	Helper helper;

	AttemptServiceImpl service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
        service = new AttemptServiceImpl(
				readingPassageRepository,
				attemptRepository,
				questionGroupRepository,
				questionRepository,
				dragItemRepository,
				choiceRepository,
                answerAttemptRepository,
				objectMapper,
				keyCloakTokenClient,
				keyCloakUserClient,
				redisService,
				kafkaTemplate,
				helper
		);
	}

	@Test
	void saveAttempt_allBranches_success() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("user-1");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder()
				.attemptId(attemptId)
				.createdBy("user-1")
				.status(Status.DRAFT)
				.duration(0L)
				.build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(req)).thenReturn("user-1");

		UUID qId1 = UUID.randomUUID();
		UUID qId2 = UUID.randomUUID();
		UUID qId3 = UUID.randomUUID();
		UUID qId4 = UUID.randomUUID();

		Question q1 = Question.builder().questionId(qId1).questionType(QuestionType.MULTIPLE_CHOICE).build();
		Question q2 = Question.builder().questionId(qId2).questionType(QuestionType.FILL_IN_THE_BLANKS).build();
		Question q3 = Question.builder().questionId(qId3).questionType(QuestionType.MATCHING).build();
		Question q4 = Question.builder().questionId(qId4).questionType(QuestionType.DRAG_AND_DROP).build();

		when(questionRepository.findById(eq(qId1))).thenReturn(Optional.of(q1));
		when(questionRepository.findById(eq(qId2))).thenReturn(Optional.of(q2));
		when(questionRepository.findById(eq(qId3))).thenReturn(Optional.of(q3));
		when(questionRepository.findById(eq(qId4))).thenReturn(Optional.of(q4));

		UUID dragItemId = UUID.randomUUID();
		SavedAnswersRequest a1 = new SavedAnswersRequest(qId1, List.of(UUID.randomUUID()), null, null, null);
		SavedAnswersRequest a2 = new SavedAnswersRequest(qId2, null, "filled", null, null);
		SavedAnswersRequest a3 = new SavedAnswersRequest(qId3, null, null, "4-A", null);
		SavedAnswersRequest a4 = new SavedAnswersRequest(qId4, null, null, null, dragItemId);

		SavedAnswersRequestList answers = new SavedAnswersRequestList(List.of(a1, a2, a3, a4), 123L);

		AnswerAttempt existing = AnswerAttempt.builder().id(AnswerAttemptId.builder().attemptId(attemptId).questionId(qId1).build()).build();
		when(answerAttemptRepository.findAnswerAttemptById(any())).thenReturn(existing);

		service.saveAttempt(attemptId.toString(), req, answers);

		verify(answerAttemptRepository, atLeast(4)).save(any(AnswerAttempt.class));
		verify(attemptRepository, atLeast(2)).save(any(Attempt.class));
		assertEquals(123L, attempt.getDuration());
	}

	@Test
	void saveAttempt_attemptNotFound_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(attemptRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(AppException.class, () -> service.saveAttempt(UUID.randomUUID().toString(), req, new com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList(List.of(), 0L)));
	}

	@Test
	void saveAttempt_forbiddenUser_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("owner").status(Status.DRAFT).build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(req)).thenReturn("other");
		assertThrows(AppException.class, () -> service.saveAttempt(attemptId.toString(), req, new com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList(List.of(), 0L)));
	}

	@Test
	void saveAttempt_notDraft_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("u").status(Status.FINISHED).build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));
		when(helper.getUserIdFromToken(req)).thenReturn("u");
		assertThrows(AppException.class, () -> service.saveAttempt(attemptId.toString(), req, new com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList(List.of(), 0L)));
	}

	@Test
	void loadAttempt_success_returnsUserDataAttempt() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("user-x");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder()
				.attemptId(attemptId)
				.createdBy("user-x")
				.status(Status.DRAFT)
				.version("json")
				.duration(0L)
				.build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		UUID passageId = UUID.randomUUID();
		ReadingPassage passage = ReadingPassage.builder()
				.passageId(passageId)
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.instruction("ins")
				.title("title")
				.content("content")
				.passageStatus(Status.PUBLISHED)
				.build();
		when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));

		UUID groupId = UUID.randomUUID();
		QuestionGroup group = QuestionGroup.builder()
				.groupId(groupId)
				.sectionOrder(1)
				.sectionLabel("Sec")
				.instruction("GIns")
				.build();
		when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

		UUID qId = UUID.randomUUID();
		Question question = Question.builder()
				.questionId(qId)
				.questionOrder(1)
				.questionType(QuestionType.MULTIPLE_CHOICE)
				.numberOfCorrectAnswers(1)
				.build();
		when(questionRepository.findById(qId)).thenReturn(Optional.of(question));

		UUID choiceId = UUID.randomUUID();
		Choice ch = Choice.builder().choiceId(choiceId).label("A").content("A").choiceOrder(1).build();
		when(choiceRepository.findAllById(anyList())).thenReturn(List.of(ch));

		UUID dragId = UUID.randomUUID();
		DragItem di = DragItem.builder().dragItemId(dragId).content("d").build();
		when(dragItemRepository.findAllById(anyList())).thenReturn(List.of(di));

		// Mock version parsing
		AttemptVersion av = AttemptVersion.builder()
				.readingPassageId(passageId)
				.build();
		Map<UUID, List<QuestionVersion>> groupMap = new HashMap<>();
		groupMap.put(groupId, List.of(QuestionVersion.builder().questionId(qId).choiceMapping(List.of(choiceId)).build()));
		av.setGroupMappingQuestion(groupMap);
		Map<UUID, List<UUID>> dragMap = new HashMap<>();
		dragMap.put(groupId, List.of(dragId));
		av.setGroupMappingDragItem(dragMap);

		JsonNode node = mock(JsonNode.class);
		when(objectMapper.readTree("json")).thenReturn(node);
		when(objectMapper.treeToValue(node, AttemptVersion.class)).thenReturn(av);

		// Existing answers to include
		AnswerAttempt ans = AnswerAttempt.builder()
				.id(AnswerAttemptId.builder().attemptId(attemptId).questionId(qId).build())
				.question(question)
				.choices(List.of(choiceId))
				.dataFilled("filled")
				.dataMatched("matched")
				.dragItemId(dragId)
				.build();
		when(answerAttemptRepository.findByAttempt(attempt)).thenReturn(List.of(ans));

		var result = service.loadAttempt(attemptId.toString(), req);
		assertEquals(attemptId, result.attemptId());
		assertEquals(1, result.answers().size());
		assertEquals(passageId, result.attemptResponse().passageId());
		assertEquals("title", result.attemptResponse().title());
		assertEquals(1, result.attemptResponse().questionGroups().size());
	}

	@Test
	void loadAttempt_attemptNotFound_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(attemptRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(AppException.class, () -> service.loadAttempt(UUID.randomUUID().toString(), req));
	}

	@Test
	void loadAttempt_forbidden_throws() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		// SecurityContext returns a different user
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("other");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("owner").status(Status.DRAFT).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		assertThrows(AppException.class, () -> service.loadAttempt(attemptId.toString(), req));
	}

	@Test
	void loadAttempt_notDraft_throws() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("user");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("user").status(Status.FINISHED).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		assertThrows(AppException.class, () -> service.loadAttempt(attemptId.toString(), req));
	}

	@Test
	void loadAttempt_passageNotFound_throws() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("user");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("user").status(Status.DRAFT).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		AttemptVersion av = AttemptVersion.builder().readingPassageId(UUID.randomUUID()).build();
		JsonNode node = mock(JsonNode.class);
		when(objectMapper.readTree("json")).thenReturn(node);
		when(objectMapper.treeToValue(node, AttemptVersion.class)).thenReturn(av);

		when(readingPassageRepository.findById(any())).thenReturn(Optional.empty());

		assertThrows(AppException.class, () -> service.loadAttempt(attemptId.toString(), req));
	}

	@Test
	void loadAttempt_unauthorized_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		// Cookie present but SecurityContext has null authentication -> NPE inside getUserIdFromToken -> AppException UNAUTHORIZED
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(null);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("user").status(Status.DRAFT).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		assertThrows(AppException.class, () -> service.loadAttempt(attemptId.toString(), req));
	}

	@Test
	void submitAttempt_success_allTypes() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
        Authentication auth = mock(Authentication.class);
        String uid = UUID.randomUUID().toString();
        when(auth.getName()).thenReturn(uid);
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
        Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy(uid).status(Status.DRAFT).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		// Build AttemptVersion mapping with 4 questions
		UUID gId = UUID.randomUUID();
		UUID qMcOrigId = UUID.randomUUID();
		UUID qMcNonId = UUID.randomUUID();
		UUID qFillId = UUID.randomUUID();
		UUID qMatchId = UUID.randomUUID();
		UUID qDragId = UUID.randomUUID();

		AttemptVersion av = AttemptVersion.builder().build();
		Map<UUID, List<QuestionVersion>> gm = new HashMap<>();
		gm.put(gId, List.of(
				QuestionVersion.builder().questionId(qMcOrigId).build(),
				QuestionVersion.builder().questionId(qMcNonId).build(),
				QuestionVersion.builder().questionId(qFillId).build(),
				QuestionVersion.builder().questionId(qMatchId).build(),
				QuestionVersion.builder().questionId(qDragId).build()
		));
		av.setGroupMappingQuestion(gm);
		JsonNode node = mock(JsonNode.class);
		when(objectMapper.readTree("json")).thenReturn(node);
		when(objectMapper.treeToValue(node, AttemptVersion.class)).thenReturn(av);

		// Questions
		Question mcOriginal = Question.builder().questionId(qMcOrigId).isOriginal(true).questionType(QuestionType.MULTIPLE_CHOICE).questionOrder(1).build();
		Question mcNonOriginal = Question.builder().questionId(qMcNonId).isOriginal(false).questionType(QuestionType.MULTIPLE_CHOICE).questionOrder(2).build();
		Question mcParent = Question.builder().questionId(UUID.randomUUID()).isOriginal(true).questionType(QuestionType.MULTIPLE_CHOICE).build();
		mcNonOriginal.setParent(mcParent);
		Question qFill = Question.builder().questionId(qFillId).questionType(QuestionType.FILL_IN_THE_BLANKS).questionOrder(3).correctAnswer("filled").build();
		Question qMatch = Question.builder().questionId(qMatchId).questionType(QuestionType.MATCHING).questionOrder(4).correctAnswerForMatching("4-A").build();
		Question qDrag = Question.builder().questionId(qDragId).questionType(QuestionType.DRAG_AND_DROP).questionOrder(5).build();
		DragItem drag = DragItem.builder().dragItemId(UUID.randomUUID()).content("drag-content").build();
		qDrag.setDragItem(drag);
		when(questionRepository.findQuestionsByIds(anyList())).thenReturn(List.of(mcOriginal, mcNonOriginal, qFill, qMatch, qDrag));

		// Multiple choice mocks
		UUID c1 = UUID.randomUUID();
		UUID c2 = UUID.randomUUID();
		when(choiceRepository.getChoicesByIds(anyList())).thenReturn(List.of("A", "B"));
		Choice orig1 = Choice.builder().choiceId(c1).label("A").build();
		Choice orig2 = Choice.builder().choiceId(c2).label("B").build();
		when(choiceRepository.getOriginalChoiceByOriginalQuestion(eq(qMcOrigId))).thenReturn(List.of(orig1, orig2));
		when(choiceRepository.getOriginalChoiceByOriginalQuestion(eq(mcParent.getQuestionId()))).thenReturn(List.of(orig1, orig2));
		when(choiceRepository.getCurrentCorrectChoice(anyList())).thenReturn(List.of(orig1));

		// Drag item content lookup
		when(dragItemRepository.findById(any())).thenReturn(Optional.of(drag));

		// Existing answer attempt fetch should be empty
		when(answerAttemptRepository.findAnswerAttemptByAttemptId(any())).thenReturn(Optional.empty());

		// Build answers
		SavedAnswersRequest aMcO = new SavedAnswersRequest(qMcOrigId, List.of(c1), null, null, null);
		SavedAnswersRequest aMcN = new SavedAnswersRequest(qMcNonId, List.of(c1), null, null, null);
		SavedAnswersRequest aFill = new SavedAnswersRequest(qFillId, null, "filled", null, null);
		SavedAnswersRequest aMatch = new SavedAnswersRequest(qMatchId, null, null, "4-A", null);
		SavedAnswersRequest aDrag = new SavedAnswersRequest(qDragId, null, null, null, drag.getDragItemId());
		SavedAnswersRequestList answers = new SavedAnswersRequestList(List.of(aMcO, aMcN, aFill, aMatch, aDrag), 222L);

		var resp = service.submitAttempt(attemptId.toString(), req, answers);
		assertEquals(222L, resp.getDuration());
		assertEquals(5, resp.getResultSets().size());
		assertTrue(resp.getResultSets().stream().allMatch(r -> r.getQuestionIndex() > 0));
        verify(kafkaTemplate).send(any(), any());
		assertEquals(Status.FINISHED, attempt.getStatus());
	}

	@Test
	void submitAttempt_attemptNotFound_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(attemptRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(AppException.class, () -> service.submitAttempt(UUID.randomUUID().toString(), req, new SavedAnswersRequestList(List.of(), 0L)));
	}

	@Test
	void submitAttempt_forbidden_throws() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("other");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("owner").status(Status.DRAFT).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		assertThrows(AppException.class, () -> service.submitAttempt(attemptId.toString(), req, new SavedAnswersRequestList(List.of(), 0L)));
	}

	@Test
	void submitAttempt_alreadyFinished_throws() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("user");
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
		Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy("user").status(Status.FINISHED).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		assertThrows(AppException.class, () -> service.submitAttempt(attemptId.toString(), req, new SavedAnswersRequestList(List.of(), 0L)));
	}

	@Test
	void submitAttempt_skipsWhenNoAnswers() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
        Authentication auth = mock(Authentication.class);
        String uid2 = UUID.randomUUID().toString();
        when(auth.getName()).thenReturn(uid2);
		SecurityContext sc = mock(SecurityContext.class);
		when(sc.getAuthentication()).thenReturn(auth);
		SecurityContextHolder.setContext(sc);

		UUID attemptId = UUID.randomUUID();
        Attempt attempt = Attempt.builder().attemptId(attemptId).createdBy(uid2).status(Status.DRAFT).version("json").build();
		when(attemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

		UUID gId = UUID.randomUUID();
		UUID qMcId = UUID.randomUUID();
		UUID qFillId = UUID.randomUUID();
		UUID qMatchId = UUID.randomUUID();
		UUID qDragId = UUID.randomUUID();
		AttemptVersion av = AttemptVersion.builder().build();
		av.setGroupMappingQuestion(Map.of(gId, List.of(
				QuestionVersion.builder().questionId(qMcId).build(),
				QuestionVersion.builder().questionId(qFillId).build(),
				QuestionVersion.builder().questionId(qMatchId).build(),
				QuestionVersion.builder().questionId(qDragId).build()
		)));
		JsonNode node = mock(JsonNode.class);
		when(objectMapper.readTree("json")).thenReturn(node);
		when(objectMapper.treeToValue(node, AttemptVersion.class)).thenReturn(av);
		Question mc = Question.builder().questionId(qMcId).isOriginal(true).questionType(QuestionType.MULTIPLE_CHOICE).questionOrder(1).build();
		Question qf = Question.builder().questionId(qFillId).questionType(QuestionType.FILL_IN_THE_BLANKS).questionOrder(2).build();
		Question qm = Question.builder().questionId(qMatchId).questionType(QuestionType.MATCHING).questionOrder(3).build();
		Question qd = Question.builder().questionId(qDragId).questionType(QuestionType.DRAG_AND_DROP).questionOrder(4).build();
		when(questionRepository.findQuestionsByIds(anyList())).thenReturn(List.of(mc, qf, qm, qd));

		SavedAnswersRequest aMc = new SavedAnswersRequest(qMcId, null, null, null, null);
		SavedAnswersRequest aF = new SavedAnswersRequest(qFillId, null, null, null, null);
		SavedAnswersRequest aM = new SavedAnswersRequest(qMatchId, null, null, null, null);
		SavedAnswersRequest aD = new SavedAnswersRequest(qDragId, null, null, null, null);
		SavedAnswersRequestList answers = new SavedAnswersRequestList(List.of(aMc, aF, aM, aD), 10L);

		var resp = service.submitAttempt(attemptId.toString(), req, answers);
		// No results because all were skipped
		assertEquals(0, resp.getResultSets().size());
	}

    @Test
    void getAttemptByUser_mapsFields() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user-x");

        UUID passageId = UUID.randomUUID();
        ReadingPassage passage = ReadingPassage.builder().passageId(passageId).title("My Passage").build();
        UUID attemptId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(5);
        Attempt attempt = Attempt.builder()
                .attemptId(attemptId)
                .readingPassage(passage)
                .createdAt(start)
                .finishedAt(end)
                .status(Status.DRAFT)
                .duration(321L)
                .totalPoints(10)
                .build();

        PageImpl<Attempt> page = new PageImpl<>(List.of(attempt), org.springframework.data.domain.PageRequest.of(0, 2), 10);
        when(attemptRepository.findAll(Mockito.<Specification<Attempt>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        var resPage = service.getAttemptByUser(0, 2, List.of(), List.of(), List.of(), null, null, null, null, req);
        assertEquals(1, resPage.getContent().size());
        assertEquals(10, resPage.getTotalElements());
        var dto = resPage.getContent().get(0);
        assertEquals(attemptId, dto.attemptId());
        assertEquals(321L, dto.duration());
        assertEquals(10, dto.totalPoints());
        assertEquals(Status.DRAFT.ordinal(), dto.status());
        assertEquals(start, dto.startAt());
        assertEquals(end, dto.finishedAt());
        assertEquals(passageId, dto.readingPassageId());
        assertEquals("My Passage", dto.title());
    }

    @Test
    void getAttemptByUser_empty_returnsEmpty() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user-x");

        PageImpl<Attempt> page = new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(1, 5), 0);
        when(attemptRepository.findAll(Mockito.<Specification<Attempt>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        var resPage = service.getAttemptByUser(1, 5, List.of(), List.of(), List.of(), "createdAt", "DESC", "keyword", null, req);
        assertTrue(resPage.getContent().isEmpty());
        assertEquals(0, resPage.getTotalElements());
    }

	@Test
	void createAttempt_success_buildsResponse() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn("user-1");

		UUID pid = UUID.randomUUID();
		ReadingPassage passage = ReadingPassage.builder()
				.passageId(pid)
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.instruction("ins")
				.title("title")
				.content("content")
				.passageStatus(Status.PUBLISHED)
				.build();
		when(readingPassageRepository.findById(pid)).thenReturn(Optional.of(passage));
		when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(Optional.of(passage));

		QuestionGroup originalGroup = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
		when(questionGroupRepository.findOriginalVersionByTaskId(pid)).thenReturn(List.of(originalGroup));

		DragItem di = DragItem.builder().dragItemId(UUID.randomUUID()).content("drag").build();
		when(dragItemRepository.findCurrentVersionByGroupId(any())).thenReturn(List.of(di));
        QuestionGroup latestGroup = QuestionGroup.builder().groupId(originalGroup.getGroupId()).sectionLabel("sec").sectionOrder(1).instruction("gi").build();
		when(questionGroupRepository.findLatestVersionByOriginalId(any())).thenReturn(latestGroup);

		UUID qid = UUID.randomUUID();
		when(questionRepository.findOriginalVersionByGroupId(originalGroup.getGroupId())).thenReturn(List.of(qid));
		Question q = Question.builder()
				.questionId(qid)
				.isOriginal(true)
				.questionOrder(1)
				.questionType(QuestionType.MULTIPLE_CHOICE)
				.numberOfCorrectAnswers(1)
				.build();
		when(questionRepository.findAllCurrentVersion(anyList())).thenReturn(List.of(q));

		Choice c = Choice.builder().choiceId(UUID.randomUUID()).label("A").content("A").choiceOrder(1).build();
		when(choiceRepository.findCurrentVersionByQuestionId(qid)).thenReturn(List.of(c));

		when(objectMapper.writeValueAsString(any())).thenReturn("json");
		when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

		var resp = service.createAttempt(pid.toString(), req);
		assertEquals(pid, resp.readingPassageId());
		assertEquals("title", resp.title());
		assertEquals(1, resp.questionGroups().size());
		var group = resp.questionGroups().get(0);
		assertEquals(latestGroup.getGroupId(), group.questionGroupId());
		assertEquals(1, group.questions().size());
		assertEquals(1, group.questions().get(0).choices().size());
		assertEquals(1, group.dragItems().size());
	}

	@Test
	void createAttempt_passageNotFound_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn("u");
		when(readingPassageRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(AppException.class, () -> service.createAttempt(UUID.randomUUID().toString(), req));
	}

	@Test
	void createAttempt_notPublished_throws() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn("u");
		UUID pid = UUID.randomUUID();
		ReadingPassage passage = ReadingPassage.builder().passageId(pid).passageStatus(null).build();
		when(readingPassageRepository.findById(pid)).thenReturn(Optional.of(passage));
		when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(Optional.of(passage));
		assertThrows(AppException.class, () -> service.createAttempt(pid.toString(), req));
	}

	@Test
	void createAttempt_success_handlesNonOriginalQuestion() throws Exception {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn("user-2");

		UUID pid = UUID.randomUUID();
		ReadingPassage passage = ReadingPassage.builder()
				.passageId(pid)
				.ieltsType(IeltsType.ACADEMIC)
				.partNumber(PartNumber.PART_1)
				.instruction("ins")
				.title("title")
				.content("content")
				.passageStatus(Status.PUBLISHED)
				.build();
		when(readingPassageRepository.findById(pid)).thenReturn(Optional.of(passage));
		when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(Optional.of(passage));

		QuestionGroup originalGroup = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
		when(questionGroupRepository.findOriginalVersionByTaskId(pid)).thenReturn(List.of(originalGroup));

		DragItem di = DragItem.builder().dragItemId(UUID.randomUUID()).content("drag").build();
		when(dragItemRepository.findCurrentVersionByGroupId(any())).thenReturn(List.of(di));
		QuestionGroup latestGroup = QuestionGroup.builder().groupId(originalGroup.getGroupId()).sectionLabel("sec").sectionOrder(1).instruction("gi").build();
		when(questionGroupRepository.findLatestVersionByOriginalId(any())).thenReturn(latestGroup);

		UUID qidOriginal = UUID.randomUUID();
		when(questionRepository.findOriginalVersionByGroupId(originalGroup.getGroupId())).thenReturn(List.of(qidOriginal));
		Question qOriginal = Question.builder()
				.questionId(qidOriginal)
				.isOriginal(true)
				.questionOrder(1)
				.questionType(QuestionType.MULTIPLE_CHOICE)
				.numberOfCorrectAnswers(1)
				.build();
        Question qNonOriginal = Question.builder()
                .questionId(UUID.randomUUID())
                .isOriginal(false)
                .questionOrder(2)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .numberOfCorrectAnswers(1)
                .build();
        // Set parent to itself so the map key matches questionId during response building
        qNonOriginal.setParent(qNonOriginal);
		when(questionRepository.findAllCurrentVersion(anyList())).thenReturn(List.of(qOriginal, qNonOriginal));

		Choice c = Choice.builder().choiceId(UUID.randomUUID()).label("A").content("A").choiceOrder(1).build();
        when(choiceRepository.findCurrentVersionByQuestionId(qidOriginal)).thenReturn(List.of(c));
        when(choiceRepository.findCurrentVersionByQuestionId(qNonOriginal.getQuestionId())).thenReturn(List.of(c));

		when(objectMapper.writeValueAsString(any())).thenReturn("json");
		when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

		var resp = service.createAttempt(pid.toString(), req);
		var group = resp.questionGroups().get(0);
		assertEquals(2, group.questions().size());
		var ids = group.questions().stream().map(q -> q.questionId()).toList();
		assertTrue(ids.contains(qidOriginal));
        assertTrue(ids.contains(qNonOriginal.getQuestionId()));
        verify(choiceRepository).findCurrentVersionByQuestionId(eq(qidOriginal));
        verify(choiceRepository).findCurrentVersionByQuestionId(eq(qNonOriginal.getQuestionId()));
	}
}
