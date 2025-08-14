package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.service.PassageService;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.response.SubmittedAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserGetHistoryExamAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExamAttemptServiceImplTest {

    @Mock
    QuestionRepository questionRepository;
    @Mock
    ExamAttemptRepository examAttemptRepository;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    ChoiceRepository choiceRepository;
    @Mock
    DragItemRepository dragItemRepository;
    @Mock
    Helper helper;
    @Mock
    PassageService passageService;
    @Mock
    ReadingExamRepository readingExamRepository;
    @Mock
    AttemptRepository attemptRepository;
    @Mock
    ReadingPassageRepository readingPassageRepository;
    @Mock
    ReportDataRepository reportDataRepository;

    ExamAttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ExamAttemptServiceImpl(
                questionRepository,
                examAttemptRepository,
                objectMapper,
                choiceRepository,
                dragItemRepository,
                helper,
                passageService,
                readingExamRepository,
                attemptRepository,
                readingPassageRepository,
                reportDataRepository
        );
    }

    @Test
    void createExamAttempt_notFound_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(readingExamRepository.findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.createExamAttempt("slug", req));
    }

    @Test
    void createExamAttempt_success_buildsResponse() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String userId = UUID.randomUUID().toString();
        when(helper.getUserIdFromToken(req)).thenReturn(userId);
        UserInformationResponse user = UserInformationResponse.builder().userId(userId).email("a@b.com").firstName("A").lastName("B").build();
        when(helper.getUserInformationResponse(userId)).thenReturn(user);

        UUID examId = UUID.randomUUID();
        ReadingExam original = ReadingExam.builder().readingExamId(examId).examName("Exam").examDescription("Desc").urlSlug("slug").build();
        when(readingExamRepository.findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse("slug")).thenReturn(Optional.of(original));
        // No current child -> fallback to original
        when(readingExamRepository.findCurrentChildByParentId(examId)).thenReturn(Optional.empty());

        ReadingPassage p1 = ReadingPassage.builder().passageId(UUID.randomUUID()).build();
        ReadingPassage p2 = ReadingPassage.builder().passageId(UUID.randomUUID()).build();
        ReadingPassage p3 = ReadingPassage.builder().passageId(UUID.randomUUID()).build();
        original.setPart1(p1);
        original.setPart2(p2);
        original.setPart3(p3);

        CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse r1 = CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse.builder().passageId(p1.getPassageId()).build();
        CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse r2 = CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse.builder().passageId(p2.getPassageId()).build();
        CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse r3 = CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse.builder().passageId(p3.getPassageId()).build();
        when(passageService.fromReadingPassage(p1.getPassageId().toString())).thenReturn(r1);
        when(passageService.fromReadingPassage(p2.getPassageId().toString())).thenReturn(r2);
        when(passageService.fromReadingPassage(p3.getPassageId().toString())).thenReturn(r3);

        // Save exam attempt
        when(examAttemptRepository.saveAndFlush(any(ExamAttempt.class))).thenAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            if (a.getExamAttemptId() == null) a.setExamAttemptId(UUID.randomUUID());
            if (a.getCreatedAt() == null) a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        CreateExamAttemptResponse resp = service.createExamAttempt("slug", req);
        assertNotNull(resp.examAttemptId());
        assertEquals("slug", resp.urlSlug());
        assertNotNull(resp.createdBy());
        assertNotNull(resp.createdAt());
        assertEquals(examId, resp.readingExam().readingExamId());
        assertEquals(p1.getPassageId(), resp.readingExam().readingPassageIdPart1().passageId());
        assertEquals(p2.getPassageId(), resp.readingExam().readingPassageIdPart2().passageId());
        assertEquals(p3.getPassageId(), resp.readingExam().readingPassageIdPart3().passageId());
    }

    @Test
    void submittedExam_notFound_throws() {
        when(examAttemptRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.submittedExam(UUID.randomUUID().toString(),
                new ExamAttemptAnswersRequest(List.of(), List.of(), List.of(), List.of(), 1),
                Mockito.mock(HttpServletRequest.class)));
    }

    @Test
    void submittedExam_success_allTypes_andGroupMap() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        UUID attemptId = UUID.randomUUID();
        ExamAttempt attempt = ExamAttempt.builder().examAttemptId(attemptId).duration(0).build();
        when(examAttemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

        // Build questions
        UUID qMcId = UUID.randomUUID();
        UUID qMcNonId = UUID.randomUUID();
        UUID qMcParentId = UUID.randomUUID();
        UUID qFillId = UUID.randomUUID();
        UUID qMatchId = UUID.randomUUID();
        UUID qDragId = UUID.randomUUID();

        Question mc = Question.builder().questionId(qMcId).questionType(QuestionType.MULTIPLE_CHOICE).point(2).isOriginal(true).questionOrder(1).explanation("e").build();
        Question mcParent = Question.builder().questionId(qMcParentId).questionType(QuestionType.MULTIPLE_CHOICE).isOriginal(true).build();
        Question mcNon = Question.builder().questionId(qMcNonId).questionType(QuestionType.MULTIPLE_CHOICE).isOriginal(false).questionOrder(2).explanation("e").parent(mcParent).build();
        Question qf = Question.builder().questionId(qFillId).questionType(QuestionType.FILL_IN_THE_BLANKS).point(1).questionOrder(3).correctAnswer("filled").explanation("e").build();
        Question qm = Question.builder().questionId(qMatchId).questionType(QuestionType.MATCHING).point(1).questionOrder(4).correctAnswerForMatching("4-A").explanation("e").build();
        DragItem drag = DragItem.builder().dragItemId(UUID.randomUUID()).content("drag-content").build();
        Question qd = Question.builder().questionId(qDragId).questionType(QuestionType.DRAG_AND_DROP).questionOrder(5).dragItem(drag).explanation("e").build();
        when(questionRepository.findQuestionsByIds(anyList())).thenReturn(List.of(mc, mcNon, qf, qm, qd));

        // Multiple choice correctness setup
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        when(choiceRepository.getChoicesByIds(anyList())).thenReturn(List.of("A", "B"));
        Choice orig1 = Choice.builder().choiceId(c1).label("A").build();
        Choice orig2 = Choice.builder().choiceId(c2).label("B").build();
        when(choiceRepository.getOriginalChoiceByOriginalQuestion(eq(qMcId))).thenReturn(List.of(orig1, orig2));
        when(choiceRepository.getOriginalChoiceByOriginalQuestion(eq(qMcParentId))).thenReturn(List.of(orig1, orig2));
        when(choiceRepository.getCurrentCorrectChoice(anyList())).thenReturn(List.of(orig1));

        // Drag items mapping for group map
        UUID groupId = UUID.randomUUID();
        DragItem i1 = DragItem.builder().dragItemId(UUID.randomUUID()).questionGroup(QuestionGroup.builder().groupId(groupId).build()).build();
        DragItem i2 = DragItem.builder().dragItemId(UUID.randomUUID()).questionGroup(QuestionGroup.builder().groupId(groupId).build()).build();
        when(dragItemRepository.findAllById(anyList())).thenReturn(List.of(i1, i2));
        when(dragItemRepository.findById(eq(drag.getDragItemId()))).thenReturn(Optional.of(drag));

        // Answers
        List<UUID> passageIds = List.of(UUID.randomUUID());
        List<UUID> groupIds = List.of(groupId);
        List<UUID> itemIds = List.of(i1.getDragItemId(), i2.getDragItemId());
        List<ExamAttemptAnswersRequest.ExamAnswerRequest> answers = List.of(
                new ExamAttemptAnswersRequest.ExamAnswerRequest(qMcId, List.of(c1.toString()), List.of(c1, c2)),
                new ExamAttemptAnswersRequest.ExamAnswerRequest(qMcNonId, List.of(c1.toString()), List.of(c1, c2)),
                new ExamAttemptAnswersRequest.ExamAnswerRequest(qFillId, List.of("filled"), null),
                new ExamAttemptAnswersRequest.ExamAnswerRequest(qMatchId, List.of("4-A"), null),
                new ExamAttemptAnswersRequest.ExamAnswerRequest(qDragId, List.of(drag.getDragItemId().toString()), null)
        );
        ExamAttemptAnswersRequest body = new ExamAttemptAnswersRequest(passageIds, groupIds, itemIds, answers, 120);

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmittedAttemptResponse resp = service.submittedExam(attemptId.toString(), body, req);
        assertEquals(120L, resp.getDuration());
        assertEquals(5, resp.getResultSets().size());
        verify(reportDataRepository).saveAll(anyList());
        verify(examAttemptRepository).save(any(ExamAttempt.class));
    }

    @Test
    void submittedExam_handlesEmptyItems_skipsUnknownQuestion() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        UUID attemptId = UUID.randomUUID();
        ExamAttempt attempt = ExamAttempt.builder().examAttemptId(attemptId).duration(0).build();
        when(examAttemptRepository.findById(eq(attemptId))).thenReturn(Optional.of(attempt));

        // Questions list contains one that won't appear in answers (skipped branch)
        UUID qMcId = UUID.randomUUID();
        Question mc = Question.builder().questionId(qMcId).questionType(QuestionType.MULTIPLE_CHOICE).point(1).isOriginal(true).questionOrder(1).explanation("e").build();
        when(questionRepository.findQuestionsByIds(anyList())).thenReturn(List.of(mc));

        // MC setup
        UUID c1 = UUID.randomUUID();
        when(choiceRepository.getChoicesByIds(anyList())).thenReturn(List.of("A"));
        Choice orig1 = Choice.builder().choiceId(c1).label("A").build();
        when(choiceRepository.getOriginalChoiceByOriginalQuestion(eq(qMcId))).thenReturn(List.of(orig1));
        when(choiceRepository.getCurrentCorrectChoice(anyList())).thenReturn(List.of(orig1));

        // Answers includes no entries for qMcId → skip
        ExamAttemptAnswersRequest body = new ExamAttemptAnswersRequest(List.of(), List.of(), List.of(), List.of(), 5);

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmittedAttemptResponse resp = service.submittedExam(attemptId.toString(), body, req);
        assertEquals(5L, resp.getDuration());
        assertEquals(0, resp.getResultSets().size());
        verify(reportDataRepository).saveAll(anyList());
    }

    @Test
    void getListExamHistory_mapsFields() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user-xyz");

        // Build exam attempts
        UUID attemptId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime updated = LocalDateTime.now();
        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .examName("Exam A")
                .examDescription("Desc A")
                .urlSlug("exam-a")
                .build();
        ExamAttempt attempt = ExamAttempt.builder()
                .examAttemptId(attemptId)
                .readingExam(exam)
                .duration(123)
                .totalPoint(40)
                .createdBy("creator")
                .updatedBy("updater")
                .createdAt(created)
                .updatedAt(updated)
                .build();

        UserInformationResponse creatorUser = UserInformationResponse.builder().userId("creator").email("c@x").build();
        UserInformationResponse updaterUser = UserInformationResponse.builder().userId("updater").email("u@x").build();
        when(helper.getUserInformationResponse("creator")).thenReturn(creatorUser);
        when(helper.getUserInformationResponse("updater")).thenReturn(updaterUser);

        org.springframework.data.domain.PageImpl<ExamAttempt> page = new org.springframework.data.domain.PageImpl<>(
                List.of(attempt), org.springframework.data.domain.PageRequest.of(0, 10), 25
        );
        when(examAttemptRepository.findAll(org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<ExamAttempt>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        var result = service.getListExamHistory(0, 10, null, null, null, req);
        assertEquals(25, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        UserGetHistoryExamAttemptResponse dto = result.getContent().get(0);
        assertEquals(attemptId, dto.examAttemptId());
        assertEquals(123, dto.duration());
        assertEquals(40, dto.totalQuestion());
        assertEquals("exam-a", dto.readingExam().urlSlug());
        assertEquals(exam.getReadingExamId(), dto.readingExam().readingExamId());
        assertEquals(creatorUser, dto.createdBy());
        assertEquals(updaterUser, dto.updatedBy());
        assertEquals(created.toString(), dto.createdAt());
        assertEquals(updated.toString(), dto.updatedAt());
    }

    @Test
    void getListExamHistory_empty_returnsEmpty() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user-xyz");

        org.springframework.data.domain.PageImpl<ExamAttempt> page = new org.springframework.data.domain.PageImpl<>(
                List.of(), org.springframework.data.domain.PageRequest.of(1, 5), 0
        );
        when(examAttemptRepository.findAll(org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<ExamAttempt>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        var result = service.getListExamHistory(1, 5, "Exam", "createdAt", "DESC", req);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAttemptResultHistory_returnsEmpty() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        var result = service.getAttemptResultHistory(req);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

