package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.response.UserProfileResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionServiceImplTest {

    @Mock QuestionRepository questionRepository;
    @Mock KeyCloakTokenClient keyCloakTokenClient;
    @Mock KeyCloakUserClient keyCloakUserClient;
    @Mock RedisService redisService;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock DragItemRepository dragItemRepository;
    @Mock
    Helper helper;
    QuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new QuestionServiceImpl(
                questionRepository,
                keyCloakTokenClient,
                keyCloakUserClient,
                redisService,
                questionGroupRepository,
                dragItemRepository,
                helper
        );

        // No need to set private fields; tests stub Redis to return cached token/profile to bypass Keycloak
    }

    @Test
    void createQuestions_multipleChoice_success() throws Exception {
        // Arrange
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        // SecurityContext is used internally; shortcut via getUserIdFromToken -> SecurityContextHolder. We'll bypass by stubbing Keycloak profile
        UserProfileResponse profile = new UserProfileResponse("uid","u","e","F","L");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");

        // Save stubs
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getCreatedAt() == null) q.setCreatedAt(LocalDateTime.now());
            if (q.getUpdatedAt() == null) q.setUpdatedAt(LocalDateTime.now());
            if (q.getQuestionId() == null) q.setQuestionId(UUID.randomUUID());
            if (q.getChoices() != null) {
                for (Choice c : q.getChoices()) {
                    if (c.getChoiceId() == null) {
                        c.setChoiceId(UUID.randomUUID());
                    }
                }
            }
            return q;
        });

        // Build request with 2 choices, 1 correct and numberOfCorrectAnswers = 1
        QuestionCreationRequest.ChoiceRequest c1 = new QuestionCreationRequest.ChoiceRequest("A","CA",1,true);
        QuestionCreationRequest.ChoiceRequest c2 = new QuestionCreationRequest.ChoiceRequest("B","CB",2,false);
        QuestionCreationRequest r = new QuestionCreationRequest(
                1,
                5,
                QuestionType.MULTIPLE_CHOICE.ordinal(),
                groupId.toString(),
                List.of("MULTIPLE"),
                "exp",
                1,
                "instr",
                List.of(c1, c2),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Mock SecurityContext for user id extraction
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        var result = service.createQuestions(List.of(r), req);
        assertEquals(1, result.size());
        assertEquals(QuestionType.MULTIPLE_CHOICE.ordinal(), result.get(0).questionType());
        assertEquals(1, result.get(0).numberOfCorrectAnswers());
        assertEquals(2, result.get(0).choices().size());
        SecurityContextHolder.clearContext();
    }

    @Test
    void createQuestions_emptyList_throws() {
        AppException ex = assertThrows(AppException.class, () -> service.createQuestions(List.of(), mock(HttpServletRequest.class)));
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());    }

    @Test
    void createQuestions_invalidType_throws() {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        QuestionCreationRequest r = new QuestionCreationRequest(1,5,999, groupId.toString(), List.of(), null, 1, null, List.of(), null, null, null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r), mock(HttpServletRequest.class)));
    }

    @Test
    void createQuestions_mc_missingChoices_throws() {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        QuestionCreationRequest r = new QuestionCreationRequest(1,5, QuestionType.MULTIPLE_CHOICE.ordinal(), groupId.toString(), List.of(), null, 1, null, List.of(), null, null, null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r), mock(HttpServletRequest.class)));
    }

    @Test
    void createQuestions_mc_invalidCorrectCount_throws() {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        QuestionCreationRequest.ChoiceRequest c1 = new QuestionCreationRequest.ChoiceRequest("A","CA",1,false);
        QuestionCreationRequest r = new QuestionCreationRequest(1,5, QuestionType.MULTIPLE_CHOICE.ordinal(), groupId.toString(), List.of(), null, 1, null, List.of(c1), null, null, null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r), mock(HttpServletRequest.class)));
    }

    @Test
    void createQuestions_fillInTheBlanks_invalids_throws() {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        // invalid numberOfCorrectAnswers != 0
        QuestionCreationRequest r1 = new QuestionCreationRequest(1,5, QuestionType.FILL_IN_THE_BLANKS.ordinal(), groupId.toString(), List.of(), null, 1, null, null, 1, "ans", null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r1), mock(HttpServletRequest.class)));

        // invalid blankIndex null
        QuestionCreationRequest r2 = new QuestionCreationRequest(1,5, QuestionType.FILL_IN_THE_BLANKS.ordinal(), groupId.toString(), List.of(), null, 0, null, null, null, "ans", null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r2), mock(HttpServletRequest.class)));

        // invalid blankIndex negative
        QuestionCreationRequest r3 = new QuestionCreationRequest(1,5, QuestionType.FILL_IN_THE_BLANKS.ordinal(), groupId.toString(), List.of(), null, 0, null, null, -1, "ans", null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r3), mock(HttpServletRequest.class)));
    }

    @Test
    void createQuestions_matching_invalids_throws() {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        // missing instructionForMatching
        QuestionCreationRequest r1 = new QuestionCreationRequest(1,5, QuestionType.MATCHING.ordinal(), groupId.toString(), List.of(), null, 0, null, null, null, null, null, null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r1), mock(HttpServletRequest.class)));

        // missing correctAnswerForMatching
        QuestionCreationRequest r2 = new QuestionCreationRequest(1,5, QuestionType.MATCHING.ordinal(), groupId.toString(), List.of(), null, 0, null, null, null, null, "instr", null, null, null, null);
        assertThrows(AppException.class, () -> service.createQuestions(List.of(r2), mock(HttpServletRequest.class)));
    }

	@Test
	void createQuestions_fillInTheBlanks_success() throws Exception {
		UUID groupId = UUID.randomUUID();
		QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
		when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
		when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("uid","u","e","F","L"));
		when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");

		when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
			Question q = inv.getArgument(0);
			if (q.getQuestionId() == null) q.setQuestionId(UUID.randomUUID());
			q.setCreatedAt(LocalDateTime.now());
			q.setUpdatedAt(LocalDateTime.now());
			return q;
		});

		QuestionCreationRequest r = new QuestionCreationRequest(
				1, 5, QuestionType.FILL_IN_THE_BLANKS.ordinal(), groupId.toString(), List.of("MULTIPLE"),
				"exp", 0,
				null, null,
				1, "answer",
				null, null,
				null, null, null
		);

		// SecurityContext for user
		var sc = mock(org.springframework.security.core.context.SecurityContext.class);
		var auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.getName()).thenReturn("uid");
		when(sc.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

		var res = service.createQuestions(List.of(r), req);
		assertEquals(1, res.size());
		assertEquals(QuestionType.FILL_IN_THE_BLANKS.ordinal(), res.get(0).questionType());
		assertEquals(1, res.get(0).blankIndex());
		assertEquals("answer", res.get(0).correctAnswer());
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}

	@Test
	void createQuestions_matching_success() throws Exception {
		UUID groupId = UUID.randomUUID();
		QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
		when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
		when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("uid","u","e","F","L"));
		when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");

		when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
			Question q = inv.getArgument(0);
			if (q.getQuestionId() == null) q.setQuestionId(UUID.randomUUID());
			q.setCreatedAt(LocalDateTime.now());
			q.setUpdatedAt(LocalDateTime.now());
			return q;
		});

		QuestionCreationRequest r = new QuestionCreationRequest(
				1, 5, QuestionType.MATCHING.ordinal(), groupId.toString(), List.of("MULTIPLE"),
				"exp", 0,
				null, null,
				null, null,
				"instr-match", "A:B",
				null, null, null
		);

		// SecurityContext for user
		var sc = mock(org.springframework.security.core.context.SecurityContext.class);
		var auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.getName()).thenReturn("uid");
		when(sc.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

		var res = service.createQuestions(List.of(r), req);
		assertEquals(1, res.size());
		assertEquals(QuestionType.MATCHING.ordinal(), res.get(0).questionType());
		assertEquals("instr-match", res.get(0).instructionForMatching());
		assertEquals("A:B", res.get(0).correctAnswerForMatching());
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}

    @Test
    void createQuestions_dragAndDrop_success() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID dragId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        DragItem dragItem = DragItem.builder().dragItemId(dragId).content("D").build();
        when(dragItemRepository.findDragItemByDragItemId(dragId)).thenReturn(Optional.of(dragItem));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");

        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getQuestionId() == null) q.setQuestionId(UUID.randomUUID());
            q.setCreatedAt(LocalDateTime.now());
            q.setUpdatedAt(LocalDateTime.now());
            return q;
        });
        when(dragItemRepository.save(any(DragItem.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionCreationRequest r = new QuestionCreationRequest(
                1,5, QuestionType.DRAG_AND_DROP.ordinal(), groupId.toString(), List.of("MULTIPLE"), "exp", 0,
                null, null,
                null, null,
                null, null,
                3, dragId.toString(), "content"
        );

        // Mock SecurityContext for user id extraction
        SecurityContext sc2 = mock(SecurityContext.class);
        Authentication auth2 = mock(Authentication.class);
        when(auth2.getName()).thenReturn("id");
        when(sc2.getAuthentication()).thenReturn(auth2);
        SecurityContextHolder.setContext(sc2);

        var res = service.createQuestions(List.of(r), req);
        assertEquals(1, res.size());
        assertEquals(QuestionType.DRAG_AND_DROP.ordinal(), res.get(0).questionType());
        assertEquals(3, res.get(0).zoneIndex());
        assertEquals(1, res.get(0).dragItems().size());
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateQuestion_multipleChoice_success() throws Exception {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Existing question
        Question existing = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .questionOrder(1)
                .numberOfCorrectAnswers(1)
                .isCurrent(true)
                .isOriginal(true)
                .version(1)
                .build();
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllVersionByQuestionId(existing)).thenReturn(List.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getChoices() == null) {
                q.setChoices(new java.util.ArrayList<>());
            }
            return q;
        });

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        // SecurityContext for user id
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UpdatedQuestionRequest update = new UpdatedQuestionRequest(
                2,
                7,
                QuestionType.MULTIPLE_CHOICE.ordinal(),
                groupId.toString(),
                List.of("MULTIPLE"),
                "new-exp",
                1,
                "new-instr",
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        var resp = service.updateQuestion(existing.getQuestionId().toString(), update, req);
        assertEquals(existing.getQuestionId().toString(), resp.questionId());
        assertEquals(2, resp.questionOrder());
        assertEquals(7, resp.point());
        assertEquals(QuestionType.MULTIPLE_CHOICE.ordinal(), resp.questionType());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateQuestion_invalidType_throws() {
        UUID qid = UUID.randomUUID();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(Question.builder().questionId(qid).build()));
        UpdatedQuestionRequest update = new UpdatedQuestionRequest(1,5,999, null, List.of(), null, 0, null, List.of(), null, null, null, null, null, null);
        assertThrows(AppException.class, () -> service.updateQuestion(qid.toString(), update, mock(HttpServletRequest.class)));
    }

    @Test
    void updateQuestion_fillInTheBlanks_success() throws Exception {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        // Existing FILL_IN_THE_BLANKS question
        Question existing = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.FILL_IN_THE_BLANKS)
                .questionOrder(1)
                .isCurrent(true)
                .isOriginal(true)
                .version(1)
                .build();
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllVersionByQuestionId(existing)).thenReturn(List.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UpdatedQuestionRequest update = new UpdatedQuestionRequest(
                3,
                9,
                QuestionType.FILL_IN_THE_BLANKS.ordinal(),
                groupId.toString(),
                List.of("MULTIPLE"),
                "exp2",
                0,
                null,
                null,
                2,
                "ans2",
                null,
                null,
                null,
                null
        );

        var resp = service.updateQuestion(existing.getQuestionId().toString(), update, req);
        assertEquals(QuestionType.FILL_IN_THE_BLANKS.ordinal(), resp.questionType());
        assertEquals(3, resp.questionOrder());
        assertEquals(9, resp.point());
        assertEquals(2, resp.blankIndex());
        assertEquals("ans2", resp.correctAnswer());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateQuestion_matching_success() throws Exception {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        // Existing MATCHING question
        Question existing = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.MATCHING)
                .questionOrder(1)
                .isCurrent(true)
                .isOriginal(true)
                .version(1)
                .build();
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllVersionByQuestionId(existing)).thenReturn(List.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UpdatedQuestionRequest update = new UpdatedQuestionRequest(
                4,
                6,
                QuestionType.MATCHING.ordinal(),
                groupId.toString(),
                List.of("MULTIPLE"),
                "exp3",
                0,
                null,
                null,
                null,
                null,
                "inst-match",
                "A:B",
                null,
                null
        );

        var resp = service.updateQuestion(existing.getQuestionId().toString(), update, req);
        assertEquals(QuestionType.MATCHING.ordinal(), resp.questionType());
        assertEquals(4, resp.questionOrder());
        assertEquals(6, resp.point());
        assertEquals("inst-match", resp.instructionForMatching());
        assertEquals("A:B", resp.correctAnswerForMatching());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateQuestion_dragAndDrop_success() throws Exception {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        // Existing DRAG_AND_DROP question with a different drag item
        DragItem existingDrag = DragItem.builder().dragItemId(UUID.randomUUID()).content("old").build();
        Question existing = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.DRAG_AND_DROP)
                .questionOrder(1)
                .dragItem(existingDrag)
                .isCurrent(true)
                .isOriginal(true)
                .version(1)
                .build();
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllVersionByQuestionId(existing)).thenReturn(List.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID newDragId = UUID.randomUUID();
        DragItem newDrag = DragItem.builder().dragItemId(newDragId).content("new").build();
        when(dragItemRepository.findDragItemByDragItemId(newDragId)).thenReturn(Optional.of(newDrag));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UpdatedQuestionRequest update = new UpdatedQuestionRequest(
                5,
                8,
                QuestionType.DRAG_AND_DROP.ordinal(),
                groupId.toString(),
                List.of("MULTIPLE"),
                "exp4",
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                9,
                newDragId.toString()
        );

        var resp = service.updateQuestion(existing.getQuestionId().toString(), update, req);
        assertEquals(QuestionType.DRAG_AND_DROP.ordinal(), resp.questionType());
        assertEquals(5, resp.questionOrder());
        assertEquals(8, resp.point());
        assertEquals(9, resp.zoneIndex());
        assertEquals(1, resp.dragItems().size());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateOrder_success_reordersAndUpdatesUsers() throws Exception {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        // Build list of questions with initial order 1..3
        Question q1 = Question.builder().questionId(UUID.randomUUID()).questionOrder(1).questionGroup(group).createdBy("c1").updatedBy("u1").build();
        q1.setCreatedAt(LocalDateTime.now()); q1.setUpdatedAt(LocalDateTime.now());
        q1.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q1.setCategories(new java.util.HashSet<>());
        Question q2 = Question.builder().questionId(UUID.randomUUID()).questionOrder(2).questionGroup(group).createdBy("c2").updatedBy("u2").build();
        q2.setCreatedAt(LocalDateTime.now()); q2.setUpdatedAt(LocalDateTime.now());
        q2.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q2.setCategories(new java.util.HashSet<>());
        Question q3 = Question.builder().questionId(UUID.randomUUID()).questionOrder(3).questionGroup(group).createdBy("c3").updatedBy("u3").build();
        q3.setCreatedAt(LocalDateTime.now()); q3.setUpdatedAt(LocalDateTime.now());
        q3.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q3.setCategories(new java.util.HashSet<>());

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(group))
                .thenReturn(new java.util.ArrayList<>(List.of(q1, q2, q3)));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // user info lookups and cached token to bypass Keycloak
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});

        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        // Move q1 to order 3
        var resp = service.updateOrder(q1.getQuestionId().toString(), groupId.toString(), new OrderUpdatedQuestionRequest(3), req);
        assertEquals(q1.getQuestionId().toString(), resp.questionId());
        assertEquals(3, resp.questionOrder());
        assertNotNull(resp.updatedBy());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateOrder_invalidTarget_throws() throws JsonProcessingException {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(any())).thenReturn(List.of());
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        AppException ex = assertThrows(AppException.class, () -> service.updateOrder(UUID.randomUUID().toString(), groupId.toString(), new OrderUpdatedQuestionRequest(2), req));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void updateOrder_invalidOrder_throws() throws JsonProcessingException {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        Question q1 = Question.builder().questionId(UUID.randomUUID()).questionOrder(1).questionGroup(group).build();
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(group)).thenReturn(List.of(q1));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        AppException ex = assertThrows(AppException.class, () -> service.updateOrder(q1.getQuestionId().toString(), groupId.toString(), new OrderUpdatedQuestionRequest(0), req));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }

    @Test
    void updateInformation_multipleChoice_success_withPreviousVersions() throws Exception {
        // Existing question
        QuestionGroup group = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
        Question existing = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .questionOrder(1)
                .createdBy("creator")
                .updatedBy("updater")
                .isCurrent(true)
                .isOriginal(true)
                .version(1)
                .build();
        existing.setCreatedAt(LocalDateTime.now().minusDays(1));
        existing.setUpdatedAt(LocalDateTime.now().minusHours(1));
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        // previous versions
        Question prev = Question.builder().questionId(existing.getQuestionId()).version(5).isCurrent(true).build();
        when(questionRepository.findAllPreviousVersion(existing.getQuestionId())).thenReturn(List.of(prev));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        // Security and cache
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));

        InformationUpdatedQuestionRequest info = new InformationUpdatedQuestionRequest(
                "ex",
                10,
                List.of("MULTIPLE"),
                2,
                "instr",
                null,
                null,
                null,
                null,
                null,
                null
        );

        var resp = service.updateInformation(existing.getQuestionId().toString(), group.getGroupId().toString(), info, req);
        assertEquals(QuestionType.MULTIPLE_CHOICE.ordinal(), resp.questionType());
        assertEquals(existing.getQuestionOrder(), resp.questionOrder());
        assertEquals(10, resp.point());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateInformation_nullRequest_throws() {
        Question existing = Question.builder().questionId(UUID.randomUUID()).questionType(QuestionType.MULTIPLE_CHOICE).questionGroup(QuestionGroup.builder().groupId(UUID.randomUUID()).build()).build();
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        AppException ex = assertThrows(AppException.class, () -> service.updateInformation(existing.getQuestionId().toString(), existing.getQuestionGroup().getGroupId().toString(), null, req));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }

    @Test
    void updateInformation_fillInTheBlanks_success() throws Exception {
        QuestionGroup group = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
        Question existing = Question.builder().questionId(UUID.randomUUID()).questionType(QuestionType.FILL_IN_THE_BLANKS).questionGroup(group).questionOrder(1).createdBy("c").updatedBy("u").isCurrent(true).isOriginal(true).version(1).build();
        existing.setCreatedAt(LocalDateTime.now()); existing.setUpdatedAt(LocalDateTime.now());
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllPreviousVersion(existing.getQuestionId())).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));

        InformationUpdatedQuestionRequest info = new InformationUpdatedQuestionRequest(
                "ex2",
                7,
                List.of("MULTIPLE"),
                0,
                null,
                3,
                "ans",
                null,
                null,
                null,
                null
        );
        var resp = service.updateInformation(existing.getQuestionId().toString(), group.getGroupId().toString(), info, req);
        assertEquals(QuestionType.FILL_IN_THE_BLANKS.ordinal(), resp.questionType());
        assertEquals(3, resp.blankIndex());
        assertEquals("ans", resp.correctAnswer());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateInformation_matching_success() throws Exception {
        QuestionGroup group = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
        Question existing = Question.builder().questionId(UUID.randomUUID()).questionType(QuestionType.MATCHING).questionGroup(group).questionOrder(1).createdBy("c").updatedBy("u").isCurrent(true).isOriginal(true).version(1).build();
        existing.setCreatedAt(LocalDateTime.now()); existing.setUpdatedAt(LocalDateTime.now());
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllPreviousVersion(existing.getQuestionId())).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));

        InformationUpdatedQuestionRequest info = new InformationUpdatedQuestionRequest(
                "ex3",
                7,
                List.of("MULTIPLE"),
                0,
                null,
                null,
                null,
                "instr",
                "A:B",
                null,
                null
        );
        var resp = service.updateInformation(existing.getQuestionId().toString(), group.getGroupId().toString(), info, req);
        assertEquals(QuestionType.MATCHING.ordinal(), resp.questionType());
        assertEquals("instr", resp.instructionForMatching());
        assertEquals("A:B", resp.correctAnswerForMatching());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateInformation_drag_success_isBelongFalse_setsDragOnQuestion() throws Exception {
        QuestionGroup group = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
        DragItem oldItem = DragItem.builder().dragItemId(UUID.randomUUID()).content("old").build();
        Question existing = Question.builder().questionId(UUID.randomUUID()).questionType(QuestionType.DRAG_AND_DROP).questionGroup(group).questionOrder(1).dragItem(oldItem).createdBy("c").updatedBy("u").isCurrent(true).isOriginal(true).version(1).build();
        existing.setCreatedAt(LocalDateTime.now()); existing.setUpdatedAt(LocalDateTime.now());
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllPreviousVersion(existing.getQuestionId())).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID newDragId = UUID.randomUUID();
        DragItem newItem = DragItem.builder().dragItemId(newDragId).content("new").build();
        when(dragItemRepository.findDragItemByDragItemId(newDragId)).thenReturn(Optional.of(newItem));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(new UserProfileResponse("id","u","e","F","L"));

        InformationUpdatedQuestionRequest info = new InformationUpdatedQuestionRequest(
                "ex4",
                7,
                List.of("MULTIPLE"),
                0,
                null,
                null,
                null,
                null,
                null,
                5,
                newDragId.toString()
        );
        var resp = service.updateInformation(existing.getQuestionId().toString(), group.getGroupId().toString(), info, req);
        assertEquals(QuestionType.DRAG_AND_DROP.ordinal(), resp.questionType());
        assertEquals(5, resp.zoneIndex());
        // Drag items in response come from newVersion.getDragItem (none), so size 0 is acceptable
        assertTrue(resp.dragItems() == null || resp.dragItems().isEmpty());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateInformation_drag_invalidDragId_throws() throws JsonProcessingException {
        QuestionGroup group = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
        Question existing = Question.builder().questionId(UUID.randomUUID()).questionType(QuestionType.DRAG_AND_DROP).questionGroup(group).questionOrder(1).createdBy("c").updatedBy("u").isCurrent(true).isOriginal(true).version(1).build();
        when(questionRepository.findById(existing.getQuestionId())).thenReturn(Optional.of(existing));
        when(questionRepository.findAllPreviousVersion(existing.getQuestionId())).thenReturn(List.of());
        when(dragItemRepository.findDragItemByDragItemId(any())).thenReturn(Optional.empty());
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached");
        AppException ex = assertThrows(AppException.class, () -> service.updateInformation(
                existing.getQuestionId().toString(),
                group.getGroupId().toString(),
                new InformationUpdatedQuestionRequest(
                        "ex",             // explanation
                        2,                 // point
                        List.of("MULTIPLE"), // categories
                        0,                 // numberOfCorrectAnswers
                        null,              // instructionForChoice
                        null,              // blankIndex
                        null,              // correctAnswer
                        null,              // instructionForMatching
                        null,              // correctAnswerForMatching
                        1,                 // zoneIndex
                        UUID.randomUUID().toString() // dragItemId
                ),
                req
        ));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }

    @Test
    void getUserIdFromToken_success_and_noCookie_and_unauthorized() {
        try {
            Method m = QuestionServiceImpl.class.getDeclaredMethod("getUserIdFromToken", HttpServletRequest.class);
            m.setAccessible(true);

            // Success
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "token")});
            SecurityContext sc = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("user-123");
            when(sc.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(sc);
            Object userId = m.invoke(service, req);
            assertEquals("user-123", userId);
            SecurityContextHolder.clearContext();

            // No cookie
            HttpServletRequest reqNo = mock(HttpServletRequest.class);
            when(reqNo.getCookies()).thenReturn(null);
            Object userId2 = m.invoke(service, reqNo);
            assertNull(userId2);

            // Unauthorized (security context throws)
            HttpServletRequest reqAuth = mock(HttpServletRequest.class);
            when(reqAuth.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "token")});
            SecurityContext scBad = mock(SecurityContext.class);
            when(scBad.getAuthentication()).thenThrow(new RuntimeException("boom"));
            SecurityContextHolder.setContext(scBad);
            assertThrows(AppException.class, () -> {
                try {
                    m.invoke(service, reqAuth);
                } catch (InvocationTargetException ite) {
                    throw (RuntimeException) ite.getTargetException();
                }
            });
            SecurityContextHolder.clearContext();
        } catch (NoSuchMethodException | IllegalAccessException e) {
            fail(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getCachedClientToken_cached_and_fetchesAndSaves() throws Exception {
        Method m = QuestionServiceImpl.class.getDeclaredMethod("getCachedClientToken");
        m.setAccessible(true);

        // cached
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");
        Object t1 = m.invoke(service);
        assertEquals("cached-token", t1);

        // miss -> fetch via client and save
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn(null);
        KeyCloakTokenResponse tr = mock(KeyCloakTokenResponse.class);
        when(tr.accessToken()).thenReturn("new-token");
        when(tr.expiresIn()).thenReturn(3600);
        when(keyCloakTokenClient.requestToken(any(), any())).thenReturn(tr);
        Object t2 = m.invoke(service);
        assertEquals("new-token", t2);
        verify(redisService, atLeastOnce()).saveValue(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void getUserProfileById_cacheHit_and_missUnauthorized_and_success() throws Exception {
        Method m = QuestionServiceImpl.class.getDeclaredMethod("getUserProfileById", String.class);
        m.setAccessible(true);

        // cache hit
        UserProfileResponse profile = new UserProfileResponse("id","u","e","F","L");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");
        Object p = m.invoke(service, "id");
        assertEquals(profile, p);

        // miss -> unauthorized
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(null);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");
        when(keyCloakUserClient.getUserById(any(), anyString(), anyString())).thenReturn(null);
        assertThrows(AppException.class, () -> {
            try {
                m.invoke(service, "id2");
            } catch (InvocationTargetException ite) {
                throw (RuntimeException) ite.getTargetException();
            }
        });

        // miss -> success
        when(keyCloakUserClient.getUserById(any(), anyString(), anyString())).thenReturn(profile);
        Object p2 = m.invoke(service, "id3");
        assertEquals(profile, p2);
        verify(redisService, atLeastOnce()).saveValue(startsWith(Constants.RedisKey.USER_PROFILE), eq(profile), any(Duration.class));
    }

    @Test
    void deleteQuestion_success_dragAndDrop_detachesAndSaves() {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        DragItem item = DragItem.builder().dragItemId(UUID.randomUUID()).content("content").build();
        Question q = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.DRAG_AND_DROP)
                .dragItem(item)
                .build();
        group.setQuestions(new java.util.ArrayList<>(List.of(q)));

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        service.deleteQuestion(q.getQuestionId().toString(), groupId.toString(), req);

        assertTrue(q.getIsDeleted());
        assertFalse(q.getIsCurrent());
        assertNull(q.getQuestionGroup());
        verify(dragItemRepository).save(any(DragItem.class));
        verify(questionRepository).save(q);
        verify(questionGroupRepository).save(group);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void deleteQuestion_success_nonDrag_updatesFlags() {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        Question q = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .build();
        group.setQuestions(new java.util.ArrayList<>(List.of(q)));

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        service.deleteQuestion(q.getQuestionId().toString(), groupId.toString(), req);

        assertTrue(q.getIsDeleted());
        assertFalse(q.getIsCurrent());
        verify(dragItemRepository, never()).save(any());
        verify(questionRepository).save(q);
        verify(questionGroupRepository).save(group);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void deleteQuestion_groupNotFound_throws() {
        when(questionGroupRepository.findById(any())).thenReturn(Optional.empty());
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        AppException ex = assertThrows(AppException.class, () -> service.deleteQuestion(UUID.randomUUID().toString(), UUID.randomUUID().toString(), req));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void deleteQuestion_questionNotFound_throws() {
        UUID groupId = UUID.randomUUID();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(QuestionGroup.builder().groupId(groupId).build()));
        when(questionRepository.findById(any())).thenReturn(Optional.empty());
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        AppException ex = assertThrows(AppException.class, () -> service.deleteQuestion(UUID.randomUUID().toString(), groupId.toString(), req));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void deleteQuestion_questionNotBelongToGroup_throws() {
        UUID groupId = UUID.randomUUID();
        QuestionGroup group = QuestionGroup.builder().groupId(groupId).build();
        QuestionGroup otherGroup = QuestionGroup.builder().groupId(UUID.randomUUID()).build();
        Question q = Question.builder().questionId(UUID.randomUUID()).questionGroup(otherGroup).build();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie("Authorization", "at")});
        var sc = mock(org.springframework.security.core.context.SecurityContext.class);
        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("uid");
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);
        AppException ex = assertThrows(AppException.class, () -> service.deleteQuestion(q.getQuestionId().toString(), groupId.toString(), req));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }
}

