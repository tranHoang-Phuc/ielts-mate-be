package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Choice;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.repository.ChoiceRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.viewmodel.request.ChoiceRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ChoiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChoiceServiceImplTest {

    @Mock
    Helper helper;
    @Mock
    ChoiceRepository choiceRepository;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    HttpServletRequest httpServletRequest;

    @InjectMocks
    ChoiceServiceImpl choiceService;

    UUID questionId;
    UUID choiceId;
    Question question;
    Choice choice;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        questionId = UUID.randomUUID();
        choiceId = UUID.randomUUID();
        question = Question.builder()
                .questionId(questionId)
                .isDeleted(false)
                .numberOfCorrectAnswers(1)
                .build();
        choice = Choice.builder()
                .choiceId(choiceId)
                .content("A")
                .choiceOrder(1)
                .isCorrect(false)
                .isCurrent(true)
                .isDeleted(false)
                .question(question)
                .children(new ArrayList<>())
                .version(1)
                .build();
    }

    // ---------------- createChoice ----------------
    @Test
    void createChoice_ShouldCreate_WhenValid() throws Exception {
        ChoiceRequest req = new ChoiceRequest("label", "B", 2, false);
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(Collections.singletonList(choice));
        when(choiceRepository.save(any(Choice.class))).thenAnswer(inv -> {
            Choice saved = inv.getArgument(0);
            saved.setChoiceId(UUID.randomUUID());
            return saved;
        });

        ChoiceResponse res = choiceService.createChoice(questionId.toString(), req, httpServletRequest);

        assertEquals("B", res.content());
        verify(choiceRepository).save(any(Choice.class));
    }

    @Test
    void createChoice_ShouldThrow_WhenUnauthorized() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(null);
        ChoiceRequest req = new ChoiceRequest("label", "B", 2, false);

        assertThrows(AppException.class,
                () -> choiceService.createChoice(questionId.toString(), req, httpServletRequest));
    }

    @Test
    void createChoice_ShouldThrow_WhenChoiceOrderExists() {
        ChoiceRequest req = new ChoiceRequest("label", "B", 1, false); // cùng order
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(List.of(choice));

        assertThrows(AppException.class,
                () -> choiceService.createChoice(questionId.toString(), req, httpServletRequest));
    }

    @Test
    void createChoice_ShouldThrow_WhenTooManyCorrectAnswers() {
        ChoiceRequest req = new ChoiceRequest("label", "B", 2, true);
        choice.setCorrect(true);
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(List.of(choice));

        assertThrows(AppException.class,
                () -> choiceService.createChoice(questionId.toString(), req, httpServletRequest));
    }
    @Test
    void createChoice_ShouldThrow_WhenQuestionDeleted() {
        // given
        ChoiceRequest req = new ChoiceRequest("label", "content", 1, false);
        question.setIsDeleted(true); // simulate question deleted

        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        // when
        AppException ex = assertThrows(AppException.class, () ->
                choiceService.createChoice(questionId.toString(), req, httpServletRequest));

        // then
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.QUESTION_NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void updateChoice_ShouldUpdate_WhenValid() throws Exception {
        ChoiceRequest req = new ChoiceRequest("newLabel", "new", 1, true);
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(List.of(choice));

        // fix: set ID khi save
        when(choiceRepository.save(any(Choice.class))).thenAnswer(inv -> {
            Choice saved = inv.getArgument(0);
            if (saved.getChoiceId() == null) {
                saved.setChoiceId(UUID.randomUUID());
            }
            return saved;
        });

        ChoiceResponse res = choiceService.updateChoice(questionId.toString(), choiceId.toString(), req, httpServletRequest);

        assertEquals("new", res.content());
        assertTrue(res.isCorrect());
        assertNotNull(res.choiceId()); // thêm check ID không null
    }


    @Test
    void updateChoice_ShouldThrow_WhenChoiceNotFound() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.empty());
        ChoiceRequest req = new ChoiceRequest("label", "B", 2, false);

        assertThrows(AppException.class,
                () -> choiceService.updateChoice(questionId.toString(), choiceId.toString(), req, httpServletRequest));
    }

    @Test
    void updateChoice_ShouldThrow_WhenChoiceOrderConflict() {
        ChoiceRequest req = new ChoiceRequest("label", "B", 2, false);
        Choice another = Choice.builder()
                .choiceId(UUID.randomUUID())
                .choiceOrder(2)
                .isDeleted(false)
                .isCurrent(true)
                .question(question)
                .children(new ArrayList<>())
                .build();

        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(List.of(choice, another));

        assertThrows(AppException.class,
                () -> choiceService.updateChoice(questionId.toString(), choiceId.toString(), req, httpServletRequest));
    }
    @Test
    void updateChoice_ShouldThrow_WhenQuestionDeleted() {
        ChoiceRequest req = new ChoiceRequest("label", "content", 1, false);

        question.setIsDeleted(true); // simulate question deleted

        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        AppException ex = assertThrows(AppException.class, () ->
                choiceService.updateChoice(questionId.toString(), choiceId.toString(), req, httpServletRequest));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.QUESTION_NOT_FOUND, ex.getBusinessErrorCode());
    }
    @Test
    void updateChoice_ShouldThrow_WhenChoiceNotBelongToQuestion() {
        ChoiceRequest req = new ChoiceRequest("label", "content", 1, false);

        Question otherQuestion = new Question();
        otherQuestion.setQuestionId(UUID.randomUUID());
        choice.setQuestion(otherQuestion); // choice không thuộc question

        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        AppException ex = assertThrows(AppException.class, () ->
                choiceService.updateChoice(questionId.toString(), choiceId.toString(), req, httpServletRequest));

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.CHOICE_NOT_FOUND, ex.getBusinessErrorCode());
    }


    @Test
    void updateChoice_ShouldThrow_WhenTooManyCorrectAnswers() {
        choice.setCorrect(true);
        ChoiceRequest req = new ChoiceRequest("label", "B", 1, true);
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(List.of(choice));

        assertThrows(AppException.class,
                () -> choiceService.updateChoice(questionId.toString(), choiceId.toString(), req, httpServletRequest));
    }

    // ---------------- getChoiceById ----------------
    @Test
    void getChoiceById_ShouldReturn_WhenValid() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        ChoiceResponse res = choiceService.getChoiceById(questionId.toString(), choiceId.toString(), httpServletRequest);

        assertEquals(choice.getContent(), res.content());
    }

    @Test
    void getChoiceById_ShouldThrow_WhenDeleted() {
        question.setIsDeleted(true);
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        assertThrows(AppException.class,
                () -> choiceService.getChoiceById(questionId.toString(), choiceId.toString(), httpServletRequest));
    }

    // ---------------- getAllChoicesOfQuestion ----------------
    @Test
    void getAllChoicesOfQuestion_ShouldReturnList() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true))
                .thenReturn(List.of(choice));

        List<ChoiceResponse> res = choiceService.getAllChoicesOfQuestion(questionId.toString(), httpServletRequest);

        assertEquals(1, res.size());
    }

    // ---------------- deleteChoice ----------------
    @Test
    void deleteChoice_ShouldMarkDeleted() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        choiceService.deleteChoice(questionId.toString(), choiceId.toString(), httpServletRequest);

        assertTrue(choice.getIsDeleted());
        verify(choiceRepository).save(choice);
    }
    @Test
    void deleteChoice_ShouldThrow_WhenQuestionOrChoiceDeleted() {
        question.setIsDeleted(true); // hoặc thử choice.setIsDeleted(true)

        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        AppException ex = assertThrows(AppException.class, () ->
                choiceService.deleteChoice(questionId.toString(), choiceId.toString(), httpServletRequest));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.QUESTION_NOT_FOUND, ex.getBusinessErrorCode());
    }
    @Test
    void deleteChoice_ShouldThrow_WhenChoiceNotBelongToQuestion() {
        Question otherQuestion = new Question();
        otherQuestion.setQuestionId(UUID.randomUUID());
        choice.setQuestion(otherQuestion); // choice không thuộc question

        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        AppException ex = assertThrows(AppException.class, () ->
                choiceService.deleteChoice(questionId.toString(), choiceId.toString(), httpServletRequest));

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.CHOICE_NOT_FOUND, ex.getBusinessErrorCode());
    }


    @Test
    void deleteChoice_ShouldDeleteChildrenToo() {
        Choice child = Choice.builder()
                .choiceId(UUID.randomUUID())
                .isDeleted(false)
                .isCurrent(true)
                .question(question)
                .children(new ArrayList<>())
                .build();
        choice.getChildren().add(child);

        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        choiceService.deleteChoice(questionId.toString(), choiceId.toString(), httpServletRequest);

        assertTrue(child.getIsDeleted());
        verify(choiceRepository, atLeastOnce()).save(any(Choice.class));
    }

    // ---------------- switchChoicesOrder ----------------
    @Test
    void switchChoicesOrder_ShouldSwapOrders() {
        Choice choice2 = Choice.builder()
                .choiceId(UUID.randomUUID())
                .choiceOrder(2)
                .isDeleted(false)
                .isCurrent(true)
                .question(question)
                .children(new ArrayList<>())
                .build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(choiceRepository.findById(choice2.getChoiceId())).thenReturn(Optional.of(choice2));

        choiceService.switchChoicesOrder(questionId.toString(), choiceId.toString(), choice2.getChoiceId().toString(), httpServletRequest);

        assertEquals(2, choice.getChoiceOrder());
        assertEquals(1, choice2.getChoiceOrder());
    }

    // ---------------- switchChoicesOrder ----------------

    @Test
    void switchChoicesOrder_ShouldThrow_WhenQuestionDeleted() {
        question.setIsDeleted(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        AppException ex = assertThrows(AppException.class, () ->
                choiceService.switchChoicesOrder(questionId.toString(), choiceId.toString(), UUID.randomUUID().toString(), httpServletRequest));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.QUESTION_NOT_FOUND, ex.getBusinessErrorCode());
    }


    @Test
    void switchChoicesOrder_ShouldThrow_WhenChoicesDeleted() {
        question.setIsDeleted(false);
        choice.setIsDeleted(true); // choice1 deleted
        Choice choice2 = new Choice();
        choice2.setChoiceId(UUID.randomUUID());
        choice2.setIsDeleted(false);
        choice2.setQuestion(question);

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(choiceRepository.findById(choice2.getChoiceId())).thenReturn(Optional.of(choice2));

        AppException ex = assertThrows(AppException.class, () ->
                choiceService.switchChoicesOrder(questionId.toString(), choiceId.toString(), choice2.getChoiceId().toString(), httpServletRequest));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.CHOICE_NOT_FOUND, ex.getBusinessErrorCode());
    }




    // ---------------- helper ----------------
    @Test
    void findCurrentOrChildCurrentChoice_ShouldReturnChild_WhenParentNotCurrent() {
        Choice child = Choice.builder()
                .choiceId(UUID.randomUUID())
                .choiceOrder(2)
                .isDeleted(false)
                .isCurrent(true)
                .question(question)
                .children(new ArrayList<>())
                .build();
        choice.setIsCurrent(false);
        choice.getChildren().add(child);

        Choice found = choiceService.findCurrentOrChildCurrentChoice(choice);

        assertEquals(child.getChoiceId(), found.getChoiceId());
    }

    @Test
    void findCurrentOrChildCurrentChoice_ShouldReturnParent_WhenParentCurrent() {
        Choice found = choiceService.findCurrentOrChildCurrentChoice(choice);
        assertEquals(choice.getChoiceId(), found.getChoiceId());
    }
    // ---------- getChoiceById ----------
    @Test
    void getChoiceById_ShouldThrow_WhenChoiceNotFound() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                choiceService.getChoiceById(questionId.toString(), choiceId.toString(), httpServletRequest));
    }

    @Test
    void getChoiceById_ShouldThrow_WhenQuestionNotFound() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                choiceService.getChoiceById(questionId.toString(), choiceId.toString(), httpServletRequest));
    }

    // ---------- getAllChoicesOfQuestion ----------
    @Test
    void getAllChoicesOfQuestion_ShouldThrow_WhenQuestionNotFound() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                choiceService.getAllChoicesOfQuestion(questionId.toString(), httpServletRequest));
    }

    // ---------- deleteChoice ----------
    @Test
    void deleteChoice_ShouldThrow_WhenChoiceNotFound() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.empty());
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        assertThrows(AppException.class, () ->
                choiceService.deleteChoice(questionId.toString(), choiceId.toString(), httpServletRequest));
    }

    @Test
    void deleteChoice_ShouldThrow_WhenQuestionNotFound() {
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                choiceService.deleteChoice(questionId.toString(), choiceId.toString(), httpServletRequest));
    }

    // ---------- switchChoicesOrder ----------
    @Test
    void switchChoicesOrder_ShouldThrow_WhenQuestionNotFound() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                choiceService.switchChoicesOrder(questionId.toString(), choiceId.toString(), UUID.randomUUID().toString(), httpServletRequest));
    }

    @Test
    void switchChoicesOrder_ShouldThrow_WhenChoice1NotFound() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                choiceService.switchChoicesOrder(questionId.toString(), choiceId.toString(), UUID.randomUUID().toString(), httpServletRequest));
    }

    @Test
    void switchChoicesOrder_ShouldThrow_WhenChoice2NotFound() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(choiceRepository.findById(choiceId)).thenReturn(Optional.of(choice));
        UUID otherId = UUID.randomUUID();
        when(choiceRepository.findById(otherId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                choiceService.switchChoicesOrder(questionId.toString(), choiceId.toString(), otherId.toString(), httpServletRequest));
    }

}
