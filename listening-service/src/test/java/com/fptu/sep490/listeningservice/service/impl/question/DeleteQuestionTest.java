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
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteQuestionTest {
    @InjectMocks
    private QuestionServiceImpl service;

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionGroupRepository questionGroupRepository;
    @Mock private Helper helper;
    @Mock private HttpServletRequest httpRequest;

    private static final ObjectMapper mapper = new ObjectMapper();

    // ====== ID CỐ ĐỊNH (UUID v4) ======
    private static final String USER_ID           = "creatortest123@gmail.com";
    private static final String GROUP_ID         = "0f5c3b8a-2d44-4e1f-8b2a-5a6c7d8e9f10";
    private static final String OTHER_GROUP_ID   = "9c2a1d3e-5b67-4c89-8def-0123456789ab";
    private static final String MISSING_GROUP_ID = "7b6a5c4d-3e2f-4a1b-9c0d-b1a2c3d4e5f6";

    private static final String QUESTION_ID      = "123e4567-e89b-42d3-a456-426614174000";
    private static final String MISSING_QUESTION_ID = "9a7b6c5d-1e23-4a5b-8c9d-0e1f2a3b4c5d";

    private QuestionGroup GROUP;       // ứng với GROUP_ID

    private void assertAppEx(AppException ex,
                             String bizCode,
                             int httpStatus,
                             String expectedMsgContainsOrExact) {
        assertThat(ex.getBusinessErrorCode()).isEqualTo(bizCode);
        assertThat(ex.getHttpStatusCode()).isEqualTo(httpStatus);
        assertThat(ex.getMessage()).contains(expectedMsgContainsOrExact);
    }

    private Question makeQuestion(UUID id, QuestionGroup group, String createdBy) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setQuestionOrder(1);
        q.setPoint(1);
        q.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q.setCategories(Set.of()); // không ảnh hưởng test
        q.setExplanation("exp");
        q.setQuestionGroup(group);
        q.setNumberOfCorrectAnswers(1);
        q.setInstructionForChoice("choose");
        q.setCreatedBy(createdBy);
        q.setUpdatedBy(createdBy);
        q.setCreatedAt(LocalDateTime.now().minusDays(2));
        q.setUpdatedAt(LocalDateTime.now().minusDays(1));
        q.setIsCurrent(true);
        q.setIsDeleted(false);
        return q;
    }

    @BeforeEach
    void setUp() {
        GROUP = new QuestionGroup();
        GROUP.setGroupId(UUID.fromString(GROUP_ID));

        // helper
        lenient().when(helper.getUserIdFromToken(httpRequest)).thenReturn(USER_ID);

        // repo: groups
        lenient().when(questionGroupRepository.findById(UUID.fromString(GROUP_ID)))
                .thenReturn(Optional.of(GROUP));
        lenient().when(questionGroupRepository.findById(UUID.fromString(MISSING_GROUP_ID)))
                .thenReturn(Optional.empty());

        // repo: save echo
        lenient().when(questionRepository.save(any(Question.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(questionGroupRepository.save(any(QuestionGroup.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- Guard / invalid ----------

    @Test
    void groupNotFound_shouldThrow() {
        var ex = assertThrows(AppException.class,
                () -> service.deleteQuestion(QUESTION_ID, MISSING_GROUP_ID, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Question group not found");

         // DEBUG JSON (request)
//         try {
//             var reqJson = mapper.createObjectNode()
//                     .put("question_id", QUESTION_ID)
//                     .put("group_id", MISSING_GROUP_ID);
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqJson));
//         } catch (JsonProcessingException ignored) {}
    }

    @Test
    void questionNotFound_shouldThrow() {
        when(questionRepository.findById(UUID.fromString(MISSING_QUESTION_ID)))
                .thenReturn(Optional.empty());

        var ex = assertThrows(AppException.class,
                () -> service.deleteQuestion(MISSING_QUESTION_ID, GROUP_ID, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "Question not found");

//         // DEBUG JSON (request)
//         try {
//             var reqJson = mapper.createObjectNode()
//                     .put("question_id", MISSING_QUESTION_ID)
//                     .put("group_id", GROUP_ID);
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqJson));
//         } catch (JsonProcessingException ignored) {}
    }

    @Test
    void questionNotBelongToGroup_shouldThrow() {
        QuestionGroup OTHER_GROUP = new QuestionGroup();
        OTHER_GROUP.setGroupId(UUID.fromString(OTHER_GROUP_ID));

        // Question phải có id = QUESTION_ID (để khớp với tham số truyền vào service)
        Question question = new Question();
        question.setQuestionId(UUID.fromString(QUESTION_ID));
        question.setQuestionGroup(OTHER_GROUP); // khác GROUP

        when(questionRepository.findById(UUID.fromString(QUESTION_ID)))
                .thenReturn(Optional.of(question));

        AppException ex = assertThrows(AppException.class,
                () -> service.deleteQuestion(QUESTION_ID, GROUP_ID, httpRequest));

        assertAppEx(ex,
                Constants.ErrorCode.QUESTION_NOT_BELONG_TO_GROUP,
                HttpStatus.BAD_REQUEST.value(),
                "Question does not belong to this group");

        // Không được phép gọi save khi fail ở bước belong-to-group check
        verify(questionRepository, never()).save(any(Question.class));
        verify(questionGroupRepository, never()).save(any(QuestionGroup.class));

//        // DEBUG JSON (request)
//        try {
//            var reqJson = mapper.createObjectNode()
//                    .put("question_id", QUESTION_ID)
//                    .put("group_id", GROUP_ID);
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqJson));
//        } catch (JsonProcessingException ignored) {}
    }
    // ---------- Success ----------

    @Test
    void success_shouldSoftDeleteAndUpdateGroup() {
        var question = makeQuestion(UUID.fromString(QUESTION_ID), GROUP, "creator");
        when(questionRepository.findById(question.getQuestionId()))
                .thenReturn(Optional.of(question));

        service.deleteQuestion(QUESTION_ID, GROUP_ID, httpRequest);

        // verify question được soft delete + isCurrent=false và được save
        ArgumentCaptor<Question> qCap = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(qCap.capture());
        Question savedQ = qCap.getValue();
        assertThat(savedQ.getIsDeleted()).isTrue();
        assertThat(savedQ.getIsCurrent()).isFalse();

        // verify group.updatedBy = USER_ID
        ArgumentCaptor<QuestionGroup> gCap = ArgumentCaptor.forClass(QuestionGroup.class);
        verify(questionGroupRepository).save(gCap.capture());
        QuestionGroup savedG = gCap.getValue();
        assertThat(savedG.getUpdatedBy()).isEqualTo(USER_ID);

//         // DEBUG JSON (request & final state)
//         try {
//             var reqJson = mapper.createObjectNode()
//                     .put("question_id", QUESTION_ID)
//                     .put("group_id", GROUP_ID);
//             System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqJson));
//         } catch (JsonProcessingException ignored) {}
    }
}
