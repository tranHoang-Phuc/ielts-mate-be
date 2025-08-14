package com.fptu.sep490.listeningservice.service.impl.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Choice;
import com.fptu.sep490.listeningservice.model.DragItem;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.DragItemRepository;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.service.impl.QuestionServiceImpl;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CreateQuestionTest {

    @InjectMocks
    private QuestionServiceImpl service;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionGroupRepository questionGroupRepository;

    @Mock
    private DragItemRepository dragItemRepository;

    @Mock
    private Helper helper;

    @Mock
    private HttpServletRequest httpRequest;

    private static final ObjectMapper mapper = new ObjectMapper();

    private final String USER_ID = "creatortest123@gmail.com";
    private UUID GROUP_ID;
    private QuestionGroup GROUP;
    private UserInformationResponse makeUserInfo(String id) {
        return new UserInformationResponse(id, "Creator", "Test", id);
    }

    private void assertAppEx(AppException ex,
                             String bizCode,
                             int httpStatus,
                             String expectedMsgContainsOrExact) {
        assertThat(ex.getBusinessErrorCode()).isEqualTo(bizCode);
        assertThat(ex.getHttpStatusCode()).isEqualTo(httpStatus);
        // Tuỳ bạn: nếu message là i18n, dùng contains cho bền hơn
        assertThat(ex.getMessage()).contains(expectedMsgContainsOrExact);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        GROUP_ID = UUID.randomUUID();
        GROUP = new QuestionGroup();
        GROUP.setGroupId(GROUP_ID);

        lenient().when(helper.getUserIdFromToken(httpRequest)).thenReturn(USER_ID);
        // Nếu 2 lớp này là record/final, ta mock bằng mockito-inline (test scope).
        lenient().when(helper.getUserInformationResponse(anyString()))
                .thenAnswer(inv -> makeUserInfo(inv.getArgument(0)));

        lenient().when(questionGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(GROUP));

        // Giả lập saveAndFlush: gán id + timestamps + id cho choices
        lenient().when(questionRepository.saveAndFlush(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getQuestionId() == null) q.setQuestionId(UUID.randomUUID());
            if (q.getCreatedAt() == null) q.setCreatedAt(LocalDateTime.now());
            q.setUpdatedAt(LocalDateTime.now());
            if (q.getChoices() != null) {
                for (Choice c : q.getChoices()) {
                    if (c.getChoiceId() == null) c.setChoiceId(UUID.randomUUID());
                }
            }
            return q;
        });
    }

    // MULTIPLE_CHOICE
    private QuestionCreationRequest mcReq(
            int numCorrect,
            List<QuestionCreationRequest.ChoiceRequest> choices
    ) {
        return new QuestionCreationRequest(
                1,                                   // questionOrder
                2,                                   // point
                QuestionType.MULTIPLE_CHOICE.ordinal(), // questionType (Integer autobox)
                GROUP_ID.toString(),                 // questionGroupId
                List.of(QuestionCategory.PIE_CHART.name()), // questionCategories
                "exp",                               // explanation
                numCorrect,                          // numberOfCorrectAnswers
                "Choose one",                        // instructionForChoice
                choices,                             // choices
                null,                                // blankIndex
                null,                                // correctAnswer
                null,                                // instructionForMatching
                null,                                // correctAnswerForMatching
                null,                                // zoneIndex
                null                                 // dragItemId
        );
    }

    // FILL_IN_THE_BLANKS
    private QuestionCreationRequest fillReq(
            Integer blankIndex,
            String correctAnswer,
            int point
    ) {
        return new QuestionCreationRequest(
                2,                                      // questionOrder
                point,                                  // point
                QuestionType.FILL_IN_THE_BLANKS.ordinal(), // questionType
                GROUP_ID.toString(),                    // questionGroupId
                List.of(QuestionCategory.PIE_CHART.name()), // questionCategories
                "exp",                                  // explanation
                0,                                      // numberOfCorrectAnswers phải = 0
                null,                                   // instructionForChoice
                null,                                   // choices
                blankIndex,                             // blankIndex
                correctAnswer,                          // correctAnswer
                null,                                   // instructionForMatching
                null,                                   // correctAnswerForMatching
                null,                                   // zoneIndex
                null                                    // dragItemId
        );
    }

    // MATCHING
    private QuestionCreationRequest matchingReq(
            String instructionForMatching,
            String correctAnswerForMatching
    ) {
        return new QuestionCreationRequest(
                3,                                      // questionOrder
                1,                                      // point
                QuestionType.MATCHING.ordinal(),        // questionType
                GROUP_ID.toString(),                    // questionGroupId
                List.of(QuestionCategory.PIE_CHART.name()), // questionCategories
                "exp",                                  // explanation
                0,                                      // numberOfCorrectAnswers phải = 0
                null,                                   // instructionForChoice
                null,                                   // choices
                null,                                   // blankIndex
                null,                                   // correctAnswer
                instructionForMatching,                 // instructionForMatching
                correctAnswerForMatching,               // correctAnswerForMatching
                null,                                   // zoneIndex
                null                                    // dragItemId
        );
    }

    // DRAG_AND_DROP
    private QuestionCreationRequest dragReq(
            UUID dragItemId,
            Integer zoneIndex,
            Integer numCorrect
    ) {
        return new QuestionCreationRequest(
                4,                                      // questionOrder
                1,                                      // point
                QuestionType.DRAG_AND_DROP.ordinal(),   // questionType
                GROUP_ID.toString(),                    // questionGroupId
                List.of(QuestionCategory.PIE_CHART.name()), // questionCategories
                "exp",                                  // explanation
                numCorrect,                             // numberOfCorrectAnswers
                null,                                   // instructionForChoice
                null,                                   // choices
                null,                                   // blankIndex
                null,                                   // correctAnswer
                null,                                   // instructionForMatching
                null,                                   // correctAnswerForMatching
                zoneIndex,                              // zoneIndex
                dragItemId == null ? null : dragItemId.toString() // dragItemId
        );
    }

    // ---------- Guard/invalid ----------
    @Test
    void nullList_shouldThrow() {
        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(null, httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_LIST_EMPTY,
                HttpStatus.BAD_REQUEST.value(),
                "Question list is empty");
    }

    @Test
    void emptyList_shouldThrow() {
        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_LIST_EMPTY,
                HttpStatus.BAD_REQUEST.value(),
                "Question list is empty");
    }

    @Test
    void invalidQuestionType_negative_shouldThrow() {
        var bad = new QuestionCreationRequest(
                1, 1, -1,
                GROUP_ID.toString(), List.of(QuestionCategory.PIE_CHART.name()),
                "exp", 1, null, null, null, null, null, null, null, null
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(bad), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_QUESTION_TYPE,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid question type");
    }

    @Test
    void invalidQuestionType_tooLarge_shouldThrow() {
        int tooLarge = QuestionType.values().length + 10;
        var bad = new QuestionCreationRequest(
                1, 1, tooLarge,
                GROUP_ID.toString(), List.of(QuestionCategory.PIE_CHART.name()),
                "exp", 1, null, null, null, null, null, null, null, null
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(bad), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_QUESTION_TYPE,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid question type");
    }

    @Test
    void groupNotFound_shouldThrow() {
        UUID missing = UUID.randomUUID();
        when(questionGroupRepository.findById(missing)).thenReturn(Optional.empty());

        var req = new QuestionCreationRequest(
                1, 1,
                QuestionType.FILL_IN_THE_BLANKS.ordinal(),
                missing.toString(),
                List.of(QuestionCategory.PIE_CHART.name()),
                "exp", 0, null, null,
                0, "ans",
                null, null,
                null, null
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Question group not found");
    }

    // ---------- MULTIPLE_CHOICE ----------
    @Test
    void mc_missingChoices_shouldThrow() {
        var req = mcReq(1, null);

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.CHOICES_LIST_EMPTY,
                HttpStatus.BAD_REQUEST.value(),
                "Choices list is empty");
    }

    @Test
    void mc_emptyChoices_shouldThrow() {
        var req = mcReq(1, List.of());

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.CHOICES_LIST_EMPTY,
                HttpStatus.BAD_REQUEST.value(),
                "Choices list is empty");
    }

    @Test
    void mc_numCorrect_lt1_shouldThrow() {
        var req = mcReq(0, List.of(
                new QuestionCreationRequest.ChoiceRequest("A", "Apple", 1, true)
        ));

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid number of correct answers");
    }

    @Test
    void mc_mismatchCorrectCount_shouldThrow() {
        var req = mcReq(2, List.of(
                new QuestionCreationRequest.ChoiceRequest("A", "Apple", 1, true),
                new QuestionCreationRequest.ChoiceRequest("B", "Banana", 2, false)
        ));

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid number of correct answers");
    }

    @Test
    void mc_success() throws JsonProcessingException {
        var req = mcReq(1, List.of(
                new QuestionCreationRequest.ChoiceRequest("A", "Apple", 1, true),
                new QuestionCreationRequest.ChoiceRequest("B", "Banana", 2, false)
        ));

        var out = service.createQuestions(List.of(req), httpRequest);

        assertThat(out).hasSize(1);
        var r = out.getFirst();
        assertThat(r.questionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE.ordinal());
        assertThat(r.questionCategories()).containsExactly(QuestionCategory.PIE_CHART.name());
        assertThat(r.numberOfCorrectAnswers()).isEqualTo(1);
        assertThat(r.choices()).hasSize(2);
        assertThat(r.choices().stream().map(QuestionCreationResponse.ChoiceResponse::choiceId))
                .allSatisfy(id -> assertThat(id).isNotBlank());

        verify(questionRepository).saveAndFlush(any(Question.class));
    }

    // ---------- FILL_IN_THE_BLANKS ----------
    @Test
    void fill_numCorrect_notZero_shouldThrow() {
        var bad = new QuestionCreationRequest(
                2, 1,
                QuestionType.FILL_IN_THE_BLANKS.ordinal(),
                GROUP_ID.toString(),
                List.of(QuestionCategory.PIE_CHART.name()),
                "exp",
                1, // sai: phải = 0
                null, null,
                3, "Paris",
                null, null,
                null, null
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(bad), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid number of correct answers");
    }

    @Test
    void fill_blankIndex_null_shouldThrow() {
        var req = fillReq(null, "Paris", 1);

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_BLANK_INDEX,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid blank index");
    }

    @Test
    void fill_blankIndex_negative_shouldThrow() {
        var req = fillReq(-1, "Paris", 1);

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_BLANK_INDEX,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid blank index");
    }

    @Test
    void fill_success() throws JsonProcessingException {
        var req = fillReq(3, "Paris", 1);

        var out = service.createQuestions(List.of(req), httpRequest);

        assertThat(out).hasSize(1);
        var r = out.getFirst();
        assertThat(r.questionType()).isEqualTo(QuestionType.FILL_IN_THE_BLANKS.ordinal());
        assertThat(r.blankIndex()).isEqualTo(3);
        assertThat(r.correctAnswer()).isEqualTo("Paris");

        verify(questionRepository).saveAndFlush(any(Question.class));
    }

    // ---------- MATCHING ----------
    @Test
    void matching_numCorrect_notZero_shouldThrow() {
        var bad = new QuestionCreationRequest(
                3, 1,
                QuestionType.MATCHING.ordinal(),
                GROUP_ID.toString(),
                List.of(QuestionCategory.PIE_CHART.name()),
                "exp",
                1, // sai: phải = 0
                null, null,
                null, null,
                "Match", "1-A,2-B",
                null, null
        );

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(bad), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid number of correct answers");
    }

    @Test
    void matching_missingInstruction_shouldThrow() {
        var bad = matchingReq(null, "1-A,2-B");

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(bad), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request");
    }

    @Test
    void matching_missingCorrectAnswer_shouldThrow() {
        var bad = matchingReq("Match", null);

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(bad), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request");
    }

    @Test
    void matching_success() throws JsonProcessingException {
        var req = matchingReq("Match", "1-A,2-B");

        var out = service.createQuestions(List.of(req), httpRequest);

        assertThat(out).hasSize(1);
        var r = out.getFirst();
        assertThat(r.questionType()).isEqualTo(QuestionType.MATCHING.ordinal());
        assertThat(r.instructionForMatching()).isEqualTo("Match");
        assertThat(r.correctAnswerForMatching()).isEqualTo("1-A,2-B");

        verify(questionRepository).saveAndFlush(any(Question.class));
    }

    // ---------- DRAG_AND_DROP ----------
    @Test
    void drag_dragItemNotFound_shouldThrow() {
        UUID dragId = UUID.randomUUID();
        when(dragItemRepository.findDragItemByDragItemId(dragId)).thenReturn(Optional.empty());
        var req = dragReq(dragId, 2, 1);

        AppException ex = assertThrows(AppException.class,
                () -> service.createQuestions(List.of(req), httpRequest));
        assertAppEx(ex,
                Constants.ErrorCode.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request");
    }

    @Test
    void drag_success() throws JsonProcessingException {
        // service sẽ gọi helper.getUserProfileById(USER_ID) -> mock để có profile.id()
        UserProfileResponse profile = mock(UserProfileResponse.class);
        when(profile.id()).thenReturn(USER_ID);
        when(helper.getUserProfileById(USER_ID)).thenReturn(profile);

        UUID dragId = UUID.randomUUID();
        DragItem di = new DragItem();
        di.setDragItemId(dragId);
        di.setContent("drag-1");
        when(dragItemRepository.findDragItemByDragItemId(dragId)).thenReturn(Optional.of(di));

        var req = dragReq(dragId, 2, 1);

        var out = service.createQuestions(List.of(req), httpRequest);

        assertThat(out).hasSize(1);
        var r = out.getFirst();
        assertThat(r.questionType()).isEqualTo(QuestionType.DRAG_AND_DROP.ordinal());
        assertThat(r.zoneIndex()).isEqualTo(2);
        assertThat(r.dragItems()).hasSize(1);
        assertThat(r.dragItems().getFirst().dragItemId()).isEqualTo(dragId.toString());

        verify(questionRepository).saveAndFlush(any(Question.class));
        verify(dragItemRepository).save(any(DragItem.class));
//        String json = null;
//        String json2 = null;
//        try {
//            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(req));
//            json2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println(json);
//        System.out.println(json2);
    }
}
