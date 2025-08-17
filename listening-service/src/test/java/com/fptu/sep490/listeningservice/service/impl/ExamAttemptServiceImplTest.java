package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamAttemptGetDetail;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ExamAttemptServiceImplTest {

    @InjectMocks
    private ExamAttemptServiceImpl service; // <== đổi sang Impl thực sự chứa createExamAttempt

    // Repos / deps
    @Mock private ListeningExamRepository listeningExamRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ExamAttemptRepository examAttemptRepository;
    @Mock private ChoiceRepository choiceRepository;
    @Mock private DragItemRepository dragItemRepository;
    @Mock private AttemptRepository attemptRepository;
    @Mock private ListeningTaskRepository listeningTaskRepository;

    @Mock private Helper helper;
    @Mock private ListeningTaskService listeningTaskService;

    @Mock private HttpServletRequest httpRequest;

    // Dùng ObjectMapper thật cho (de)serialization history
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    // ====== Constants (cố định để viết báo cáo) ======
    private static final String USER_ID = "studenttest123@gmail.com";

    private static final String URL_SLUG = "listening-exam-slug-001";

    private static final UUID ORIG_EXAM_ID   = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CURR_EXAM_ID   = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID ATTEMPT_ID     = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");

    private static final UUID TASK1_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID TASK2_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID TASK3_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final UUID TASK4_ID = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");

    // ====== Helpers chung ======
    private UserInformationResponse makeUser(String id) {
        return new UserInformationResponse(id, "Student", "Test", id);
    }

    private void assertAppEx(AppException ex, String bizCode, int httpStatus, String expectedMsgContains) {
        assertThat(ex.getBusinessErrorCode()).isEqualTo(bizCode);
        assertThat(ex.getHttpStatusCode()).isEqualTo(httpStatus);
        assertThat(ex.getMessage()).contains(expectedMsgContains);
    }

    private ListeningTask task(UUID id, int partOrdinal) {
        ListeningTask t = new ListeningTask();
        t.setTaskId(id);
        // Nếu entity có enum partNumber/ieltsType thì ở test này ta không dùng trực tiếp,
        // vì service chỉ lấy taskId để gọi listeningTaskService.fromListeningTask(...).
        return t;
    }

    private ListeningExam exam(UUID id, String name, String desc, String slug,
                               ListeningTask p1, ListeningTask p2, ListeningTask p3, ListeningTask p4) {
        ListeningExam e = new ListeningExam();
        e.setListeningExamId(id);
        e.setExamName(name);
        e.setExamDescription(desc);
        e.setUrlSlug(slug);
        e.setPart1(p1);
        e.setPart2(p2);
        e.setPart3(p3);
        e.setPart4(p4);
        return e;
    }

    private CreateExamAttemptResponse.ListeningExamResponse.ListeningTaskResponse fakeTaskResp(UUID id, int partOrdinal) {
        return CreateExamAttemptResponse.ListeningExamResponse.ListeningTaskResponse.builder()
                .taskId(id)
                .partNumber(partOrdinal)
                .instruction("instr " + partOrdinal)
                .title("title " + partOrdinal)
                .audioFileId(UUID.randomUUID())
                .questionGroups(List.of())
                .build();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Common stubs
        lenient().when(helper.getUserIdFromToken(httpRequest)).thenReturn(USER_ID);
        lenient().when(helper.getUserInformationResponse(anyString()))
                .thenAnswer(inv -> makeUser(inv.getArgument(0)));

        // saveAndFlush attempt: gán id + timestamps
        lenient().when(examAttemptRepository.saveAndFlush(any(ExamAttempt.class)))
                .thenAnswer(inv -> {
                    ExamAttempt a = inv.getArgument(0);
                    if (a.getExamAttemptId() == null) a.setExamAttemptId(ATTEMPT_ID);
                    if (a.getCreatedAt() == null) a.setCreatedAt(LocalDateTime.now());
                    a.setUpdatedAt(LocalDateTime.now());
                    return a;
                });

        // save attempt: echo
        lenient().when(examAttemptRepository.save(any(ExamAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ========== createExamAttempt ==========

    @Test
    void createExamAttempt_examNotFound_shouldThrow() {
        when(listeningExamRepository.findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(URL_SLUG))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.createExamAttempt(URL_SLUG, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.LISTENING_EXAM_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Listening exam not found");

//        // DEBUG JSON (request)
//        System.out.println("{\"url_slug\":\"" + URL_SLUG + "\"}");
    }

    @Test
    void createExamAttempt_success_shouldCreateAttemptAndMapTasks() throws JsonProcessingException {
        // Arrange original + current child exam
        var p1 = task(TASK1_ID, 1);
        var p2 = task(TASK2_ID, 2);
        var p3 = task(TASK3_ID, 3);
        var p4 = task(TASK4_ID, 4);

        ListeningExam original = exam(ORIG_EXAM_ID, "IELTS Listening A", "desc", URL_SLUG, p1, p2, p3, p4);
        ListeningExam current  = exam(CURR_EXAM_ID, "IELTS Listening A (current)", "desc current", URL_SLUG, p1, p2, p3, p4);

        when(listeningExamRepository.findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(URL_SLUG))
                .thenReturn(Optional.of(original));
        when(listeningExamRepository.findCurrentChildByParentId(ORIG_EXAM_ID))
                .thenReturn(Optional.of(current));

        // Mock task mapping
        when(listeningTaskService.fromListeningTask(TASK1_ID.toString())).thenReturn(fakeTaskResp(TASK1_ID, 1));
        when(listeningTaskService.fromListeningTask(TASK2_ID.toString())).thenReturn(fakeTaskResp(TASK2_ID, 2));
        when(listeningTaskService.fromListeningTask(TASK3_ID.toString())).thenReturn(fakeTaskResp(TASK3_ID, 3));
        when(listeningTaskService.fromListeningTask(TASK4_ID.toString())).thenReturn(fakeTaskResp(TASK4_ID, 4));

        // Act
        CreateExamAttemptResponse out = service.createExamAttempt(URL_SLUG, httpRequest);

        // Assert
        assertThat(out.examAttemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(out.urlSlug()).isEqualTo(URL_SLUG);
        assertThat(out.createdBy().userId()).isEqualTo(USER_ID);

        assertThat(out.listeningExam().listeningExamId()).isEqualTo(CURR_EXAM_ID);
        assertThat(out.listeningExam().listeningTaskIdPart1().taskId()).isEqualTo(TASK1_ID);
        assertThat(out.listeningExam().listeningTaskIdPart2().taskId()).isEqualTo(TASK2_ID);
        assertThat(out.listeningExam().listeningTaskIdPart3().taskId()).isEqualTo(TASK3_ID);
        assertThat(out.listeningExam().listeningTaskIdPart4().taskId()).isEqualTo(TASK4_ID);

        verify(examAttemptRepository).saveAndFlush(any(ExamAttempt.class));

//        // DEBUG JSON (response)
//        System.out.println(new ObjectMapper()
//                .writerWithDefaultPrettyPrinter()
//                .writeValueAsString(out));
    }

    // ========== getExamAttemptById ==========

    @Test
    void getExamAttemptById_notFound_shouldThrow() {
        UUID missing = UUID.fromString("9a7b6c5d-1e23-4a5b-8c9d-0e1f2a3b4c5d");
        when(examAttemptRepository.findById(missing)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.getExamAttemptById(missing.toString(), httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Can not find this exam attempt");
    }

    @Test
    void getExamAttemptById_wrongOwner_shouldThrowNotFound() {
        ExamAttempt att = new ExamAttempt();
        att.setExamAttemptId(ATTEMPT_ID);
        att.setCreatedBy("someoneelse@example.com");
        att.setUpdatedBy("someoneelse@example.com");
        att.setCreatedAt(LocalDateTime.now().minusDays(1));
        att.setUpdatedAt(LocalDateTime.now());

        att.setListeningExam(new ListeningExam()); // tối thiểu

        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(att));
        when(helper.getUserIdFromToken(httpRequest)).thenReturn(USER_ID);

        AppException ex = assertThrows(AppException.class,
                () -> service.getExamAttemptById(ATTEMPT_ID.toString(), httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Can not find this exam attempt");
    }

    @Test
    void getExamAttemptById_notSubmitted_shouldThrow() {
        ExamAttempt att = new ExamAttempt();
        att.setExamAttemptId(ATTEMPT_ID);
        att.setCreatedBy(USER_ID);
        att.setUpdatedBy(USER_ID);
        att.setCreatedAt(LocalDateTime.now().minusDays(1));
        att.setUpdatedAt(LocalDateTime.now());
        att.setListeningExam(new ListeningExam());
        att.setHistory(null); // chưa submit

        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(att));

        AppException ex = assertThrows(AppException.class,
                () -> service.getExamAttemptById(ATTEMPT_ID.toString(), httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.EXAM_ATTEMPT_NOT_SUBMIT,
                HttpStatus.NOT_FOUND.value(),
                "You cannot view result before submitting");
    }

    @Test
    void getExamAttemptById_success_shouldReturnDetail() throws Exception {
        // Attempt có history
        ExamAttempt att = new ExamAttempt();
        att.setExamAttemptId(ATTEMPT_ID);
        att.setCreatedBy(USER_ID);
        att.setUpdatedBy(USER_ID);
        att.setCreatedAt(LocalDateTime.now().minusDays(1));
        att.setUpdatedAt(LocalDateTime.now());
        att.setDuration(123);
        att.setTotalPoint(40);

        ListeningExam exam = new ListeningExam();
        exam.setListeningExamId(CURR_EXAM_ID);
        exam.setExamName("Exam X");
        exam.setExamDescription("desc");
        exam.setUrlSlug(URL_SLUG);
        att.setListeningExam(exam);

        // History JSON
        ExamAttemptHistory history = ExamAttemptHistory.builder()
                .taskId(List.of(UUID.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee")))
                .questionGroupIds(List.of())
                .userAnswers(Map.of())
                .groupMapItems(Map.of())
                .questionMapChoices(Map.of())
                .questionIds(List.of())
                .build();
        att.setHistory(objectMapper.writeValueAsString(history));

        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(att));

        // Mock fromExamAttemptHistory -> trả ra 4 part responses (cố tình out-of-order để kiểm tra sort)
        var t3 = ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.builder()
                .partNumber(3).build();
        var t1 = ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.builder()
                .partNumber(1).build();
        var t4 = ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.builder()
                .partNumber(4).build();
        var t2 = ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.builder()
                .partNumber(2).build();

        when(listeningTaskService.fromExamAttemptHistory(any(ExamAttemptHistory.class)))
                .thenReturn(List.of(t3, t1, t4, t2));

        ExamAttemptGetDetail out = service.getExamAttemptById(ATTEMPT_ID.toString(), httpRequest);

        assertThat(out.examAttemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(out.readingExam().urlSlug()).isEqualTo(URL_SLUG);
        // đã sort theo partNumber: 1,2,3,4
        assertThat(out.readingExam().listeningTaskIdPart1().partNumber()).isEqualTo(1);
        assertThat(out.readingExam().listeningTaskIdPart2().partNumber()).isEqualTo(2);
        assertThat(out.readingExam().listeningTaskIdPart3().partNumber()).isEqualTo(3);
        assertThat(out.readingExam().listeningTaskIdPart4().partNumber()).isEqualTo(4);

        assertThat(out.duration()).isEqualTo(123L);
        assertThat(out.totalQuestion()).isEqualTo(40);
        assertThat(out.createdBy().userId()).isEqualTo(USER_ID);
        assertThat(out.updatedBy().userId()).isEqualTo(USER_ID);

//        // DEBUG JSON (response)
//        System.out.println(new ObjectMapper()
//                .writerWithDefaultPrettyPrinter()
//                .writeValueAsString(out));
    }

    // ========== getListExamHistory ==========

    @Test
    void getListExamHistory_basicMapping_shouldReturnPagedResult() {
        // two attempts
        ExamAttempt a1 = new ExamAttempt();
        a1.setExamAttemptId(UUID.fromString("99999999-9999-4999-8999-999999999999"));
        a1.setDuration(60);
        a1.setTotalPoint(30);
        a1.setCreatedBy(USER_ID);
        a1.setUpdatedBy(USER_ID);
        a1.setCreatedAt(LocalDateTime.now().minusDays(2));
        a1.setUpdatedAt(LocalDateTime.now().minusDays(1));
        ListeningExam e1 = new ListeningExam();
        e1.setListeningExamId(UUID.fromString("f1f1f1f1-f1f1-4f1f-8f1f-f1f1f1f1f1f1"));
        e1.setExamName("Exam 1");
        e1.setExamDescription("desc 1");
        e1.setUrlSlug("slug-1");
        a1.setListeningExam(e1);

        ExamAttempt a2 = new ExamAttempt();
        a2.setExamAttemptId(UUID.fromString("88888888-8888-4888-8888-888888888888"));
        a2.setDuration(45);
        a2.setTotalPoint(25);
        a2.setCreatedBy(USER_ID);
        a2.setUpdatedBy(USER_ID);
        a2.setCreatedAt(LocalDateTime.now().minusDays(3));
        a2.setUpdatedAt(LocalDateTime.now().minusDays(2));
        ListeningExam e2 = new ListeningExam();
        e2.setListeningExamId(UUID.fromString("e2e2e2e2-e2e2-4e2e-8e2e-e2e2e2e2e2e2"));
        e2.setExamName("Exam 2");
        e2.setExamDescription("desc 2");
        e2.setUrlSlug("slug-2");
        a2.setListeningExam(e2);

        Page<ExamAttempt> repoPage = new PageImpl<>(List.of(a1, a2), PageRequest.of(0, 10), 2);

        when(examAttemptRepository.findAll((Specification<ExamAttempt>) any(), any(Pageable.class))).thenReturn(repoPage);

        var page = service.getListExamHistory(0, 10, null, null, null, httpRequest);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).createdBy().userId()).isEqualTo(USER_ID);
        assertThat(page.getContent().get(0).listeningExam().urlSlug()).isEqualTo("slug-1");
        assertThat(page.getContent().get(1).listeningExam().urlSlug()).isEqualTo("slug-2");

//        // DEBUG JSON (response)
//        try {
//            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter()
//                    .writeValueAsString(page.getContent()));
//        } catch (JsonProcessingException ignored) {}
    }

    // ====================== submittedExam ======================

    @Test
    void submittedExam_attemptNotFound_shouldThrow() {
        String attemptId = "9a7b6c5d-1e23-4a5b-8c9d-0e1f2a3b4c5d";
        when(examAttemptRepository.findById(UUID.fromString(attemptId)))
                .thenReturn(Optional.empty());

        var req = new ExamAttemptAnswersRequest(
                List.of(UUID.randomUUID()),         // taskId (list)
                List.of(UUID.randomUUID()),         // questionGroupIds
                List.of(),                          // itemsIds
                List.of(),                          // answers
                300                                 // duration
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.submittedExam(attemptId, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Can not find this exam attempt");

//        // DEBUG JSON
//        try {
//            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(req));
//        } catch (JsonProcessingException ignored) {}
    }

    @Test
    void submittedExam_success_mixedQuestionTypes_shouldScoreAndPersist() throws Exception {
        // ---- Attempt đã tồn tại ----
        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamAttemptId(ATTEMPT_ID);
        attempt.setCreatedAt(LocalDateTime.now().minusHours(1));
        attempt.setUpdatedAt(LocalDateTime.now().minusMinutes(30));
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attempt));

        // ---- 4 câu hỏi: MCQ (2 điểm), FILL (1 điểm), MATCH (3 điểm), DRAG (1 điểm nhưng code KHÔNG cộng) ----
        UUID Q1 = UUID.fromString("11111111-1111-4111-8111-aaaaaaaaaaaa");
        UUID Q2 = UUID.fromString("22222222-2222-4222-8222-bbbbbbbbbbbb");
        UUID Q3 = UUID.fromString("33333333-3333-4333-8333-cccccccccccc");
        UUID Q4 = UUID.fromString("44444444-4444-4444-8444-dddddddddddd");

        // Q1: MCQ (original = true)
        Question mcq = new Question();
        mcq.setQuestionId(Q1);
        mcq.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        mcq.setPoint(2);
        mcq.setQuestionOrder(1);
        mcq.setExplanation("mcq exp");
        mcq.setIsOriginal(true);

        // Q2: FILL
        Question fill = new Question();
        fill.setQuestionId(Q2);
        fill.setQuestionType(QuestionType.FILL_IN_THE_BLANKS);
        fill.setPoint(1);
        fill.setQuestionOrder(2);
        fill.setExplanation("fill exp");
        fill.setCorrectAnswer("Paris");

        // Q3: MATCH
        Question match = new Question();
        match.setQuestionId(Q3);
        match.setQuestionType(QuestionType.MATCHING);
        match.setPoint(3);
        match.setQuestionOrder(3);
        match.setExplanation("match exp");
        match.setCorrectAnswerForMatching("1-A,2-B");

        // Q4: DRAG
        UUID DRAG_ID = UUID.fromString("0d9f7c6b-1234-4a5b-8c9d-0123456789ab");
        DragItem di = new DragItem();
        di.setDragItemId(DRAG_ID);
        di.setContent("drag-content");
        Question drag = new Question();
        drag.setQuestionId(Q4);
        drag.setQuestionType(QuestionType.DRAG_AND_DROP);
        drag.setPoint(1); // code không cộng điểm cho DRAG
        drag.setQuestionOrder(4);
        drag.setExplanation("drag exp");
        drag.setDragItem(di);

        // Repo trả về danh sách câu hỏi đúng thứ tự questionIds
        when(questionRepository.findQuestionsByIds(anyList()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    List<UUID> ids = (List<UUID>) inv.getArgument(0);
                    Map<UUID, Question> map = Map.of(
                            Q1, mcq, Q2, fill, Q3, match, Q4, drag
                    );
                    return ids.stream().map(map::get).toList();
                });

        // ---- MCQ: user chọn 2 đáp án đúng (C1, C2) ----
        UUID C1 = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-111111111111");
        UUID C2 = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-222222222222");

        // userAnswers -> labels (A, B)
        when(choiceRepository.getChoicesByIds(eq(List.of(C1, C2))))
                .thenReturn(List.of("A", "B"));

        // correctAnswers: từ original question (vì isOriginal=true)
        Choice corr1 = new Choice();
        corr1.setChoiceId(C1);
        corr1.setLabel("A");
        Choice corr2 = new Choice();
        corr2.setChoiceId(C2);
        corr2.setLabel("B");

        // originalChoice ids -> current correct choice subset
        when(choiceRepository.getOriginalChoiceByOriginalQuestion(Q1))
                .thenReturn(List.of(corr1, corr2));
        when(choiceRepository.getCurrentCorrectChoice(eq(List.of(C1, C2))))
                .thenReturn(List.of(corr1, corr2));

        // ---- Request ----
        var req = new ExamAttemptAnswersRequest(
                List.of(UUID.randomUUID()),        // taskId
                List.of(UUID.randomUUID()),        // groupIds
                List.of(),                         // itemsIds
                List.of(
                        new ExamAttemptAnswersRequest.ExamAnswerRequest(
                                Q1, List.of(C1.toString(), C2.toString()), List.of(C1, C2) // MCQ đúng
                        ),
                        new ExamAttemptAnswersRequest.ExamAnswerRequest(
                                Q2, List.of("Paris"), List.of() // FILL đúng
                        ),
                        new ExamAttemptAnswersRequest.ExamAnswerRequest(
                                Q3, List.of("1-A,2-B"), List.of() // MATCH đúng
                        ),
                        new ExamAttemptAnswersRequest.ExamAnswerRequest(
                                Q4, List.of(DRAG_ID.toString()), List.of() // DRAG đúng (code không cộng điểm)
                        )
                ),
                300 // duration
        );

        ArgumentCaptor<ExamAttempt> saveCap = ArgumentCaptor.forClass(ExamAttempt.class);

        var out = service.submittedExam(ATTEMPT_ID.toString(), req, httpRequest);

        // Kiểm tra đã set duration + totalPoint và lưu
        verify(examAttemptRepository).save(saveCap.capture());
        ExamAttempt saved = saveCap.getValue();
        assertThat(saved.getDuration()).isEqualTo(300);
        // Điểm = MCQ(2) + FILL(1) + MATCH(3) = 6 ; DRAG không cộng
        assertThat(saved.getTotalPoint()).isEqualTo(6);

        // Kết quả trả về
        assertThat(out.getDuration()).isEqualTo(300L);
        assertThat(out.getResultSets()).hasSize(4);
        assertThat(out.getResultSets()).anySatisfy(rs -> {
            if (rs.getQuestionIndex() == 1) {
                assertThat(rs.isCorrect()).isTrue();
                assertThat(rs.getExplanation()).isEqualTo("mcq exp");
            }
        });

//        // DEBUG JSON
//        try {
//            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(req));
//            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(out));
//        } catch (JsonProcessingException ignored) {}
    }

// ====================== getOverViewProgress ======================

    @Test
    void getOverViewProgress_success_shouldAggregate() {
        String token = "Bearer token";
        when(helper.getUserIdFromToken(token)).thenReturn(USER_ID);

        // Exams (2 cái trong khoảng 1m)
        ExamAttempt e1 = new ExamAttempt();
        e1.setCreatedAt(LocalDateTime.now().minusDays(10));
        e1.setTotalPoint(30);

        ExamAttempt e2 = new ExamAttempt();
        e2.setCreatedAt(LocalDateTime.now().minusDays(2));
        e2.setTotalPoint(40);

        when(examAttemptRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of(e1, e2));
        when(listeningExamRepository.numberOfActiveExams()).thenReturn(9);

        // Tasks (1 cái mới nhất → lastLearningDate lấy từ đây)
        Attempt t1 = new Attempt();
        t1.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(attemptRepository.findAllByUserId(USER_ID)).thenReturn(List.of(t1));
        when(listeningTaskRepository.numberOfPublishedTasks()).thenReturn(20);

        var body = com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq.builder()
                .timeFrame("1m")
                .build();

        var out = service.getOverViewProgress(body, token);

        assertThat(out.getExam()).isEqualTo(2);
        assertThat(out.getTask()).isEqualTo(1);
        assertThat(out.getTotalExams()).isEqualTo(9);
        assertThat(out.getTotalTasks()).isEqualTo(20);

        assertThat(out.getNumberOfExamsInTimeFrame()).isEqualTo(2);
        assertThat(out.getNumberOfTasksInTimeFrame()).isEqualTo(1);
        assertThat(out.getAverageBandInTimeFrame()).isEqualTo(35.0);

        assertThat(out.getLastLearningDate()).isNotNull();
        assertThat(out.getLastLearningDate()).isAfter(e2.getCreatedAt()); // lấy từ task mới nhất

//        // DEBUG JSON
//        System.out.println(out);
    }

// ====================== getBandChart ======================

    @Test
    void getBandChart_empty_shouldReturnEmptyList() {
        String token = "Bearer token";
        when(helper.getUserIdFromToken(token)).thenReturn(USER_ID);
        when(examAttemptRepository.findByUserAndDateRange(eq(USER_ID), any(), any()))
                .thenReturn(List.of());

        var body = com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq.builder()
                .timeFrame("1w")
                .build();

        var out = service.getBandChart(body, token);
        assertThat(out).isEmpty();
    }

    @Test
    void getBandChart_sameDay_shouldReturnSingleAveragedPoint() {
        String token = "Bearer token";
        when(helper.getUserIdFromToken(token)).thenReturn(USER_ID);

        // 2 exams cùng ngày -> gộp 1 điểm trung bình
        ExamAttempt e1 = new ExamAttempt();
        e1.setCreatedAt(LocalDateTime.now().minusHours(5));
        e1.setTotalPoint(20);
        ExamAttempt e2 = new ExamAttempt();
        e2.setCreatedAt(LocalDateTime.now().minusHours(2));
        e2.setTotalPoint(40);

        when(examAttemptRepository.findByUserAndDateRange(eq(USER_ID), any(), any()))
                .thenReturn(List.of(e1, e2));

        var body = com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq.builder()
                .timeFrame("1w")
                .startDate(null)
                .endDate(null)
                .build();

        var out = service.getBandChart(body, token);

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getValue()).isEqualTo(30.0); // (20 + 40) / 2

//        // DEBUG JSON
//        System.out.println(out.getFirst().getDate() + " -> " + out.getFirst().getValue());
    }
}
