package com.fptu.sep490.listeningservice.service.impl.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.service.impl.QuestionServiceImpl;
import com.fptu.sep490.listeningservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.UpdatedQuestionResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UpdateOrderTest {
    @InjectMocks
    private QuestionServiceImpl service;

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionGroupRepository questionGroupRepository;
    @Mock private Helper helper;
    @Mock private HttpServletRequest httpRequest;

    private static final ObjectMapper mapper = new ObjectMapper();

    // ====== ID CỐ ĐỊNH DÙNG CHUNG (ĐỒNG BỘ CHO BÁO CÁO) ======
    private static final String USER_ID = "creatortest123@gmail.com";

    // groupId
    private static final String VALID_GROUP_ID   = UUID.randomUUID().toString();
    private static final String MISSING_GROUP_ID = UUID.randomUUID().toString();

    // questionId
    private static final String VALID_QID_IN_LIST     = UUID.randomUUID().toString();
    private static final String VALID_QID_NOT_IN_LIST = UUID.randomUUID().toString();
    private static final String INVALID_QID           = "not-a-uuid";

    private QuestionGroup GROUP; // ứng với VALID_GROUP_ID

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

    private Question q(UUID id, int order, String createdBy) {
        Question qq = new Question();
        qq.setQuestionId(id);
        qq.setQuestionOrder(order);
        qq.setPoint(1);
        qq.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        qq.setCategories(Set.of(QuestionCategory.PIE_CHART));
        qq.setExplanation("exp");
        qq.setQuestionGroup(GROUP);
        qq.setNumberOfCorrectAnswers(1);
        qq.setInstructionForChoice("choose");
        qq.setCreatedBy(createdBy);
        qq.setUpdatedBy(createdBy);
        qq.setCreatedAt(LocalDateTime.now().minusDays(1));
        qq.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return qq;
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.openMocks(this);
        // Group cố định
        GROUP = new QuestionGroup();
        GROUP.setGroupId(UUID.fromString(VALID_GROUP_ID));

        // helper
        lenient().when(helper.getUserIdFromToken(httpRequest)).thenReturn(USER_ID);

        UserProfileResponse profile = mock(UserProfileResponse.class);
        lenient().when(profile.id()).thenReturn(USER_ID);
        lenient().when(helper.getUserProfileById(USER_ID)).thenReturn(profile);

        lenient().when(helper.getUserInformationResponse(anyString()))
                .thenAnswer(inv -> makeUserInfo(inv.getArgument(0)));

        // repo: group
        lenient().when(questionGroupRepository.findById(UUID.fromString(VALID_GROUP_ID)))
                .thenReturn(Optional.of(GROUP));
        lenient().when(questionGroupRepository.findById(UUID.fromString(MISSING_GROUP_ID)))
                .thenReturn(Optional.empty());

        // saveAll: echo iterable
        lenient().when(questionRepository.saveAll(anyIterable()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- Guard / invalid ----------

    @Test
    void groupNotFound_shouldThrow() {
        var req = new OrderUpdatedQuestionRequest(2);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateOrder(VALID_QID_IN_LIST, MISSING_GROUP_ID, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Question group not found");

        // DEBUG JSON (request)
        try {
            System.out.println(VALID_QID_IN_LIST);
            System.out.println(MISSING_GROUP_ID);
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
        } catch (JsonProcessingException ignored) {}
    }

    @Test
    void emptyQuestionList_shouldThrow() {
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(GROUP))
                .thenReturn(List.of());

        var req = new OrderUpdatedQuestionRequest(1);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateOrder(VALID_QID_IN_LIST, VALID_GROUP_ID, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_LIST_EMPTY,
                HttpStatus.NOT_FOUND.value(),
                "Question list is empty");

//        // DEBUG JSON (request)
//        try {
//            System.out.println(VALID_QID_IN_LIST);
//            System.out.println(VALID_GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//        } catch (JsonProcessingException ignored) {}
    }

    @Test
    void targetQuestionNotFound_shouldThrow() {
        var q1 = q(UUID.fromString("33333333-3333-3333-3333-333333333333"), 1, "c1");
        var q2 = q(UUID.fromString("44444444-4444-4444-4444-444444444444"), 2, "c2");
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(GROUP))
                .thenReturn(List.of(q1, q2));

        var req = new OrderUpdatedQuestionRequest(1);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateOrder(VALID_QID_NOT_IN_LIST, VALID_GROUP_ID, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Question not found");

//        // DEBUG JSON (request)
//        try {
//            System.out.println(VALID_QID_NOT_IN_LIST);
//            System.out.println(VALID_GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//        } catch (JsonProcessingException ignored) {}
    }

    @Test
    void newOrderLessThan1_shouldThrow() {
        var target = q(UUID.fromString(VALID_QID_IN_LIST), 2, "c1");
        var other  = q(UUID.fromString("44444444-4444-4444-4444-444444444444"), 1, "c2");
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(GROUP))
                .thenReturn(List.of(other, target)); // order asc

        var req = new OrderUpdatedQuestionRequest(0);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateOrder(VALID_QID_IN_LIST, VALID_GROUP_ID, req, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request");

//        // DEBUG JSON (request)
//        try {
//            System.out.println(VALID_QID_IN_LIST);
//            System.out.println(VALID_GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//        } catch (JsonProcessingException ignored) {}
    }

    // ---------- Success paths ----------

    @Test
    void success_moveMiddleToFirst_shouldReorderAndSave() throws JsonProcessingException {
        // given 3 questions: [q1(order1), q2(order2)=target, q3(order3)]
        var q1 = q(UUID.fromString("33333333-3333-3333-3333-333333333333"), 1, "creator-1");
        var q2 = q(UUID.fromString(VALID_QID_IN_LIST), 2, "creator-2"); // target (cố định)
        var q3 = q(UUID.fromString("44444444-4444-4444-4444-444444444444"), 3, "creator-3");
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(GROUP))
                .thenReturn(new ArrayList<>(List.of(q1, q2, q3)));

        var req = new OrderUpdatedQuestionRequest(1);

        // when
        UpdatedQuestionResponse out = service.updateOrder(VALID_QID_IN_LIST, VALID_GROUP_ID, req, httpRequest);

        // then
        assertThat(out).isNotNull();
        assertThat(out.questionId()).isEqualTo(VALID_QID_IN_LIST);
        assertThat(out.questionOrder()).isEqualTo(1);
        assertThat(out.questionType()).isEqualTo(q2.getQuestionType().ordinal());
        assertThat(out.questionCategories()).containsExactly(QuestionCategory.PIE_CHART.name());
        assertThat(out.questionGroupId()).isEqualTo(VALID_GROUP_ID);

        // verify saveAll is called with changed items (q1->2, q2->1)
        ArgumentCaptor<Iterable<Question>> cap = ArgumentCaptor.forClass(Iterable.class);
        verify(questionRepository).saveAll(cap.capture());
        List<Question> saved = new ArrayList<>();
        cap.getValue().forEach(saved::add);

        // both q1 and q2 must be in toSave (orders changed); q3 unchanged
        assertThat(saved).extracting(Question::getQuestionId)
                .containsExactlyInAnyOrder(q1.getQuestionId(), q2.getQuestionId());
        // updated orders
        assertThat(q1.getQuestionOrder()).isEqualTo(2);
        assertThat(q2.getQuestionOrder()).isEqualTo(1);
        assertThat(q3.getQuestionOrder()).isEqualTo(3);
        // updatedBy set to acting user
        assertThat(q1.getUpdatedBy()).isEqualTo(USER_ID);
        assertThat(q2.getUpdatedBy()).isEqualTo(USER_ID);

//        // DEBUG JSON (request & response)
//        try {
//            System.out.println(VALID_QID_IN_LIST);
//            System.out.println(VALID_GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
//        } catch (JsonProcessingException ignored) {}
    }

    @Test
    void success_newOrderBiggerThanSize_shouldMoveToEnd() throws JsonProcessingException {
        // given 3 questions: [q1(order1)=target, q2(order2), q3(order3)]
        var q1 = q(UUID.fromString(VALID_QID_IN_LIST), 1, "creator-1"); // target (cố định)
        var q2 = q(UUID.fromString("33333333-3333-3333-3333-333333333333"), 2, "creator-2");
        var q3 = q(UUID.fromString("44444444-4444-4444-4444-444444444444"), 3, "creator-3");
        when(questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(GROUP))
                .thenReturn(new ArrayList<>(List.of(q1, q2, q3)));

        // new order very large -> insertIndex = size() after removal -> move to end
        var req = new OrderUpdatedQuestionRequest(999);

        UpdatedQuestionResponse out = service.updateOrder(VALID_QID_IN_LIST, VALID_GROUP_ID, req, httpRequest);

        assertThat(out.questionId()).isEqualTo(VALID_QID_IN_LIST);
        assertThat(out.questionOrder()).isEqualTo(3); // now at end

        // verify orders
        ArgumentCaptor<Iterable<Question>> cap = ArgumentCaptor.forClass(Iterable.class);
        verify(questionRepository).saveAll(cap.capture());
        List<Question> saved = new ArrayList<>();
        cap.getValue().forEach(saved::add);

        // q1 và q2 đổi (q1->3, q2->1), q3 -> 2
        assertThat(q1.getQuestionOrder()).isEqualTo(3);
        assertThat(q2.getQuestionOrder()).isEqualTo(1);
        assertThat(q3.getQuestionOrder()).isEqualTo(2);

        assertThat(saved).extracting(Question::getQuestionId)
                .containsExactlyInAnyOrder(q1.getQuestionId(), q2.getQuestionId(), q3.getQuestionId());

        assertThat(q1.getUpdatedBy()).isEqualTo(USER_ID);
        assertThat(q2.getUpdatedBy()).isEqualTo(USER_ID);
        assertThat(q3.getUpdatedBy()).isEqualTo(USER_ID);

//        // DEBUG JSON (request & response)
//        try {
//            System.out.println(VALID_QID_IN_LIST);
//            System.out.println(VALID_GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
//        } catch (JsonProcessingException ignored) {}
    }
}
