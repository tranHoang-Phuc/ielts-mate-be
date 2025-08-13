package com.fptu.sep490.listeningservice.service.impl.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.DragItem;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.DragItemRepository;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.service.impl.QuestionServiceImpl;
import com.fptu.sep490.listeningservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.UpdatedQuestionResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateInformationTest {

    @InjectMocks
    private QuestionServiceImpl service;

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionGroupRepository questionGroupRepository; // không dùng trực tiếp nhưng giữ cho đồng bộ
    @Mock private DragItemRepository dragItemRepository;
    @Mock private Helper helper;
    @Mock private HttpServletRequest httpRequest;

    private static final ObjectMapper mapper = new ObjectMapper();

    // ====== ID CỐ ĐỊNH (UUID v4), GIỮ NGUYÊN GIỮA CÁC LẦN CHẠY ======
    private static final String USER_ID = "creatortest123@gmail.com";
    private static final String GROUP_ID = "0f5c3b8a-2d44-4e1f-8b2a-5a6c7d8e9f10";

    private static final String QID     = "123e4567-e89b-42d3-a456-426614174000";
    private static final String QID_MISSING = "9a7b6c5d-1e23-4a5b-8c9d-0e1f2a3b4c5d";

    private QuestionGroup GROUP; // dùng cho question.getQuestionGroup()

    // -------- helpers ----------
    private UserInformationResponse makeUserInfo(String id) {
        return new UserInformationResponse(id, "Creator", "Test", id);
    }

    private void assertAppEx(AppException ex,
                             String bizCode,
                             int httpStatus,
                             String expectedMsgContainsOrExact) {
        assertThat(ex.getBusinessErrorCode()).isEqualTo(bizCode);
        assertThat(ex.getHttpStatusCode()).isEqualTo(httpStatus);
        assertThat(ex.getMessage()).contains(expectedMsgContainsOrExact);
    }

    private Question baseQuestion(UUID id, QuestionType type, String createdBy, Set<QuestionCategory> cats) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setQuestionOrder(1);
        q.setPoint(2);
        q.setQuestionType(type);
        q.setCategories(cats);
        q.setExplanation("exp");
        q.setQuestionGroup(GROUP);
        q.setNumberOfCorrectAnswers(1);
        q.setInstructionForChoice("choose");
        q.setCreatedBy(createdBy);
        q.setUpdatedBy(createdBy);
        q.setCreatedAt(LocalDateTime.now().minusDays(2));
        q.setUpdatedAt(LocalDateTime.now().minusDays(1));
        q.setIsCurrent(true);
        q.setVersion(1);
        return q;
    }

    private Question prevVersion(int version) {
        Question pv = new Question();
        pv.setQuestionId(UUID.randomUUID());
        pv.setVersion(version);
        pv.setIsCurrent(true);
        return pv;
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        GROUP = new QuestionGroup();
        GROUP.setGroupId(UUID.fromString(GROUP_ID));

        // Helper
        lenient().when(helper.getUserIdFromToken(httpRequest)).thenReturn(USER_ID);

        UserProfileResponse profile = mock(UserProfileResponse.class);
        lenient().when(profile.id()).thenReturn(USER_ID);
        lenient().when(helper.getUserProfileById(USER_ID)).thenReturn(profile);

        lenient().when(helper.getUserInformationResponse(anyString()))
                .thenAnswer(inv -> makeUserInfo(inv.getArgument(0)));

        // Repository generic stubs
        lenient().when(questionRepository.save(any(Question.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(questionRepository.saveAll(anyIterable()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- Guard / invalid ----------

    @Test
    void questionNotFound_shouldThrow() {
        when(questionRepository.findById(UUID.fromString(QID_MISSING)))
                .thenReturn(Optional.empty());

        var req = new InformationUpdatedQuestionRequest(
                "new exp",                              // explanation
                5,                                      // point
                List.of(QuestionCategory.PIE_CHART.name()), // questionCategories
                null, null, null, null, null, null, null, null
        );


        AppException ex = assertThrows(AppException.class,
                () -> service.updateInformation(QID_MISSING, GROUP_ID, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Question not found");

//         // DEBUG JSON (request)
//         try {
//             System.out.println(QID_MISSING);
//             System.out.println(GROUP_ID);
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//         } catch (JsonProcessingException ignored) {}
    }

    @Test
    void nullRequest_shouldThrow() {
        var q = baseQuestion(UUID.fromString(QID), QuestionType.MULTIPLE_CHOICE, "creator-1",
                Set.of(QuestionCategory.PIE_CHART));
        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        when(questionRepository.findAllPreviousVersion(q.getQuestionId())).thenReturn(List.of());

        AppException ex = assertThrows(AppException.class,
                () -> service.updateInformation(QID, GROUP_ID, null, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request");

//        // DEBUG JSON (request & response)
//        System.out.println(QID);
//        System.out.println(GROUP_ID);
    }

    // ---------- MULTIPLE_CHOICE ----------

    @Test
    void mc_success_withPreviousVersions() throws JsonProcessingException {
        var q = baseQuestion(UUID.fromString(QID), QuestionType.MULTIPLE_CHOICE, "creator-1",
                Set.of(QuestionCategory.PIE_CHART));

        // previous versions with max version = 3
        var prevs = List.of(prevVersion(1), prevVersion(2), prevVersion(3));

        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        when(questionRepository.findAllPreviousVersion(q.getQuestionId())).thenReturn(prevs);

        var req = new InformationUpdatedQuestionRequest(
                "new exp",                              // explanation
                10,                                     // point
                List.of(QuestionCategory.APOLOGY.name(), QuestionCategory.PIE_CHART.name()),
                2,                                      // numberOfCorrectAnswers
                "Pick the best",                        // instructionForChoice
                null,                                   // blankIndex
                null,                                   // correctAnswer
                null,                                   // instructionForMatching
                null,                                   // correctAnswerForMatching
                null,                                   // zoneIndex
                null                                    // dragItemId
        );


        ArgumentCaptor<Question> saveCap = ArgumentCaptor.forClass(Question.class);

        UpdatedQuestionResponse out = service.updateInformation(QID, GROUP_ID, req, httpRequest);

        // verify previous versions were marked isCurrent=false and saved
        assertThat(prevs).allSatisfy(pv -> assertThat(pv.getIsCurrent()).isFalse());
        verify(questionRepository).saveAll(prevs);

        // verify two saves: original question (isCurrent=false, updatedBy=USER_ID), and newVersion
        verify(questionRepository, times(2)).save(saveCap.capture());
        List<Question> saved = saveCap.getAllValues();
        Question savedOriginal = saved.get(0);
        Question newVersion = saved.get(1);

        assertThat(savedOriginal.getIsCurrent()).isFalse();
        assertThat(savedOriginal.getUpdatedBy()).isEqualTo(USER_ID);

        assertThat(newVersion.getQuestionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(newVersion.getVersion()).isEqualTo(4); // lastVersion(3) + 1
        assertThat(newVersion.getNumberOfCorrectAnswers()).isEqualTo(2);
        assertThat(newVersion.getInstructionForChoice()).isEqualTo("Pick the best");
        assertThat(newVersion.getPoint()).isEqualTo(10);
        assertThat(newVersion.getCategories())
                .containsExactlyInAnyOrder(QuestionCategory.APOLOGY, QuestionCategory.PIE_CHART);

        // response
        assertThat(out.questionId()).isEqualTo(QID);
        assertThat(out.questionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE.ordinal());
        assertThat(out.instructionForChoice()).isEqualTo("Pick the best");
        assertThat(out.numberOfCorrectAnswers()).isEqualTo(2);
        assertThat(out.questionCategories())
                .containsExactlyInAnyOrder(QuestionCategory.APOLOGY.name(), QuestionCategory.PIE_CHART.name());

//         // DEBUG JSON (request & response)
//         try {
//             System.out.println(QID);
//             System.out.println(GROUP_ID);
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
//         } catch (JsonProcessingException ignored) {}
    }

    // ---------- FILL_IN_THE_BLANKS ----------

    @Test
    void fill_success_noPreviousVersions() throws JsonProcessingException {
        var q = baseQuestion(UUID.fromString(QID), QuestionType.FILL_IN_THE_BLANKS, "creator-2",
                Set.of(QuestionCategory.PIE_CHART));

        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        when(questionRepository.findAllPreviousVersion(q.getQuestionId())).thenReturn(List.of());

        var req = new InformationUpdatedQuestionRequest(
                "new exp fill",                         // explanation
                3,                                      // point
                List.of(QuestionCategory.APOLOGY.name()),
                null,                                   // numberOfCorrectAnswers
                null,                                   // instructionForChoice
                2,                                      // blankIndex
                "Paris",                                // correctAnswer
                null,                                   // instructionForMatching
                null,                                   // correctAnswerForMatching
                null,                                   // zoneIndex
                null                                    // dragItemId
        );


        ArgumentCaptor<Question> saveCap = ArgumentCaptor.forClass(Question.class);

        UpdatedQuestionResponse out = service.updateInformation(QID, GROUP_ID, req, httpRequest);

        verify(questionRepository, times(2)).save(saveCap.capture());
        Question newVersion = saveCap.getAllValues().get(1);

        assertThat(newVersion.getQuestionType()).isEqualTo(QuestionType.FILL_IN_THE_BLANKS);
        assertThat(newVersion.getVersion()).isEqualTo(1); // no previous versions -> 0 + 1
        assertThat(newVersion.getBlankIndex()).isEqualTo(2);
        assertThat(newVersion.getCorrectAnswer()).isEqualTo("Paris");
        assertThat(newVersion.getCategories()).containsExactly(QuestionCategory.APOLOGY);

        assertThat(out.questionId()).isEqualTo(QID);
        assertThat(out.questionType()).isEqualTo(QuestionType.FILL_IN_THE_BLANKS.ordinal());
        assertThat(out.blankIndex()).isEqualTo(2);
        assertThat(out.correctAnswer()).isEqualTo("Paris");
        assertThat(out.questionCategories()).containsExactly(QuestionCategory.APOLOGY.name());

//        // DEBUG JSON (request & response)
//        try {
//            System.out.println(QID);
//            System.out.println(GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
//        } catch (JsonProcessingException ignored) {}
    }

    // ---------- MATCHING ----------

    @Test
    void matching_success() throws JsonProcessingException {
        var q = baseQuestion(UUID.fromString(QID), QuestionType.MATCHING, "creator-3",
                Set.of(QuestionCategory.PIE_CHART));

        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        when(questionRepository.findAllPreviousVersion(q.getQuestionId()))
                .thenReturn(List.of(prevVersion(5))); // lastVersion = 5

        var req = new InformationUpdatedQuestionRequest(
                "matching exp",                         // explanation
                4,                                      // point
                List.of(QuestionCategory.APOLOGY.name(), QuestionCategory.PIE_CHART.name()),
                null,                                   // numberOfCorrectAnswers
                null,                                   // instructionForChoice
                null,                                   // blankIndex
                null,                                   // correctAnswer
                "Match",                                // instructionForMatching
                "1-A,2-B",                              // correctAnswerForMatching
                null,                                   // zoneIndex
                null                                    // dragItemId
        );



        ArgumentCaptor<Question> saveCap = ArgumentCaptor.forClass(Question.class);

        UpdatedQuestionResponse out = service.updateInformation(QID, GROUP_ID, req, httpRequest);

        verify(questionRepository, times(2)).save(saveCap.capture());
        Question newVersion = saveCap.getAllValues().get(1);

        assertThat(newVersion.getVersion()).isEqualTo(6);
        assertThat(newVersion.getQuestionType()).isEqualTo(QuestionType.MATCHING);
        assertThat(newVersion.getInstructionForMatching()).isEqualTo("Match");
        assertThat(newVersion.getCorrectAnswerForMatching()).isEqualTo("1-A,2-B");
        assertThat(newVersion.getCategories())
                .containsExactlyInAnyOrder(QuestionCategory.APOLOGY, QuestionCategory.PIE_CHART);

        assertThat(out.questionType()).isEqualTo(QuestionType.MATCHING.ordinal());
        assertThat(out.instructionForMatching()).isEqualTo("Match");
        assertThat(out.correctAnswerForMatching()).isEqualTo("1-A,2-B");
        assertThat(out.questionCategories())
                .containsExactlyInAnyOrder(QuestionCategory.APOLOGY.name(), QuestionCategory.PIE_CHART.name());

//        // DEBUG JSON (request & response)
//        try {
//            System.out.println(QID);
//            System.out.println(GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
//        } catch (JsonProcessingException ignored) {}
    }

    // ---------- DRAG_AND_DROP ----------

    @Test
    void drag_dragItemNotFound_shouldThrow() {
        var q = baseQuestion(UUID.fromString(QID), QuestionType.DRAG_AND_DROP, "creator-4",
                Set.of(QuestionCategory.APOLOGY)); // categories của question

        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        when(questionRepository.findAllPreviousVersion(q.getQuestionId())).thenReturn(List.of());

        String missingDragId = "11111111-1111-4111-8111-111111111111";
        when(dragItemRepository.findDragItemByDragItemId(UUID.fromString(missingDragId)))
                .thenReturn(Optional.empty());

        var req = new InformationUpdatedQuestionRequest(
                "drag exp",                             // explanation
                7,                                      // point
                List.of(QuestionCategory.PIE_CHART.name()), // (sẽ bị override trong code branch DRAG)
                null,                                   // numberOfCorrectAnswers
                null,                                   // instructionForChoice
                null,                                   // blankIndex
                null,                                   // correctAnswer
                null,                                   // instructionForMatching
                null,                                   // correctAnswerForMatching
                2,                                      // zoneIndex
                missingDragId                           // dragItemId
        );



        AppException ex = assertThrows(AppException.class,
                () -> service.updateInformation(QID, GROUP_ID, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request");

//        // DEBUG JSON (request & response)
//        try {
//            System.out.println(QID);
//            System.out.println(GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//        } catch (JsonProcessingException ignored) {}
    }

    @Test
    void drag_success_categoriesFromQuestion_overrideInfo() throws JsonProcessingException {
        var q = baseQuestion(UUID.fromString(QID), QuestionType.DRAG_AND_DROP, "creator-4",
                Set.of(QuestionCategory.APOLOGY)); // categories gốc

        when(questionRepository.findById(q.getQuestionId())).thenReturn(Optional.of(q));
        when(questionRepository.findAllPreviousVersion(q.getQuestionId())).thenReturn(List.of(prevVersion(2)));

        UUID dragId = UUID.fromString("0d9f7c6b-1234-4a5b-8c9d-0123456789ab"); // hợp lệ v4 theo cú pháp
        DragItem di = new DragItem();
        di.setDragItemId(dragId);
        di.setContent("drag-content");
        when(dragItemRepository.findDragItemByDragItemId(dragId)).thenReturn(Optional.of(di));

        var req = new InformationUpdatedQuestionRequest(
                "drag exp",                             // explanation
                7,                                      // point
                List.of(QuestionCategory.PIE_CHART.name()), // (sẽ bị override bởi categories của question)
                null,                                   // numberOfCorrectAnswers
                null,                                   // instructionForChoice
                null,                                   // blankIndex
                null,                                   // correctAnswer
                null,                                   // instructionForMatching
                null,                                   // correctAnswerForMatching
                2,                                      // zoneIndex
                dragId.toString()                       // dragItemId
        );

        ArgumentCaptor<Question> saveCap = ArgumentCaptor.forClass(Question.class);

        UpdatedQuestionResponse out = service.updateInformation(QID, GROUP_ID, req, httpRequest);

        verify(questionRepository, times(2)).save(saveCap.capture());
        Question newVersion = saveCap.getAllValues().get(1);

        assertThat(newVersion.getVersion()).isEqualTo(3);
        assertThat(newVersion.getQuestionType()).isEqualTo(QuestionType.DRAG_AND_DROP);
        assertThat(newVersion.getZoneIndex()).isEqualTo(2);
        assertThat(newVersion.getDragItem()).isNotNull();
        assertThat(newVersion.getDragItem().getDragItemId()).isEqualTo(dragId);

        // Lưu ý: code DRAG đã override categories = categories của question (không lấy từ request)
        assertThat(newVersion.getCategories()).containsExactlyInAnyOrderElementsOf(q.getCategories());
        assertThat(newVersion.getCategories()).containsExactly(QuestionCategory.APOLOGY);

        // response
        assertThat(out.questionType()).isEqualTo(QuestionType.DRAG_AND_DROP.ordinal());
        assertThat(out.zoneIndex()).isEqualTo(2);
        assertThat(out.dragItems()).hasSize(1);
        assertThat(out.dragItems().getFirst().dragItemId()).isEqualTo(dragId.toString());
        assertThat(out.questionCategories()).containsExactly(QuestionCategory.APOLOGY.name());

//         // DEBUG JSON (request & response)
//         try {
//             System.out.println(QID);
//             System.out.println(GROUP_ID);
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
//         } catch (JsonProcessingException ignored) {}
    }
}
