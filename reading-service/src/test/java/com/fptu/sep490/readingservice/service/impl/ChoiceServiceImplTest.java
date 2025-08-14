package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.repository.ChoiceRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.viewmodel.request.ChoiceCreation;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedChoiceRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChoiceServiceImplTest {

    @Mock
    ChoiceRepository choiceRepository;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    KeyCloakTokenClient keyCloakTokenClient;
    @Mock
    KeyCloakUserClient keyCloakUserClient;
    @Mock
    RedisService redisService;

    ChoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ChoiceServiceImpl(choiceRepository, questionRepository, keyCloakTokenClient, keyCloakUserClient, redisService);
    }

    // getAllChoicesOfQuestion
    @Test
    void getAllChoices_questionNotFound_throws() {
        when(questionRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.getAllChoicesOfQuestion(UUID.randomUUID().toString()));
    }

    @Test
    void getAllChoices_emptyChoices_throws() {
        UUID qid = UUID.randomUUID();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(Question.builder().questionId(qid).build()));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(any(), eq(false))).thenReturn(List.of());
        assertThrows(AppException.class, () -> service.getAllChoicesOfQuestion(qid.toString()));
    }

    @Test
    void getAllChoices_success_maps() {
        UUID qid = UUID.randomUUID();
        Question q = Question.builder().questionId(qid).build();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(q));
        Choice c1 = Choice.builder().choiceId(UUID.randomUUID()).content("A").choiceOrder(1).label("A").isCorrect(true).build();
        Choice c2 = Choice.builder().choiceId(UUID.randomUUID()).content("B").choiceOrder(2).label("B").isCorrect(false).build();
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(q), eq(false))).thenReturn(List.of(c1, c2));
        var list = service.getAllChoicesOfQuestion(qid.toString());
        assertEquals(2, list.size());
        assertEquals("A", list.get(0).content());
        assertEquals(2, list.get(1).choiceOrder());
    }

    // createChoice
    private void seedAuth(HttpServletRequest req, String userId) {
        when(req.getCookies()).thenReturn(new Cookie[]{ new Cookie("Authorization", "token") });
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);
    }

    @Test
    void createChoice_questionNotFound_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        when(questionRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.createChoice(UUID.randomUUID().toString(), new ChoiceCreation("A", "x", 1, true), req));
    }

    @Test
    void createChoice_invalidNumberCorrect_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        UUID qid = UUID.randomUUID();
        Question question = Question.builder().questionId(qid).numberOfCorrectAnswers(1).build();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(question), eq(false)))
                .thenReturn(List.of(Choice.builder().isCorrect(true).build()));
        ChoiceCreation creation = new ChoiceCreation("A", "content", 2, true);
        assertThrows(AppException.class, () -> service.createChoice(qid.toString(), creation, req));
    }

    @Test
    void createChoice_success() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String user = UUID.randomUUID().toString();
        seedAuth(req, user);
        UUID qid = UUID.randomUUID();
        Question question = Question.builder().questionId(qid).numberOfCorrectAnswers(2).build();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(question), eq(false)))
                .thenReturn(List.of(Choice.builder().isCorrect(true).build()));
        Choice saved = Choice.builder().choiceId(UUID.randomUUID()).content("C").choiceOrder(3).isCorrect(false).label("C").build();
        when(choiceRepository.save(any(Choice.class))).thenReturn(saved);
        ChoiceCreation creation = new ChoiceCreation("C", "C", 3, false);
        QuestionCreationResponse.ChoiceResponse resp = service.createChoice(qid.toString(), creation, req);
        assertEquals(saved.getChoiceId().toString(), resp.choiceId());
        assertEquals("C", resp.content());
        assertEquals(3, resp.choiceOrder());
        assertFalse(resp.isCorrect());
        assertEquals("C", resp.label());
    }

    // updateChoice
    @Test
    void updateChoice_choiceNotFound_throws() {
        when(choiceRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.updateChoice(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new UpdatedChoiceRequest("L", "C", 1, true),
                Mockito.mock(HttpServletRequest.class)
        ));
    }

    @Test
    void updateChoice_choiceNotInQuestion_throws() {
        UUID qid = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Question q = Question.builder().questionId(other).build();
        Choice existing = Choice.builder().choiceId(UUID.randomUUID()).question(q).build();
        when(choiceRepository.findById(existing.getChoiceId())).thenReturn(Optional.of(existing));
        assertThrows(AppException.class, () -> service.updateChoice(
                qid.toString(),
                existing.getChoiceId().toString(),
                new UpdatedChoiceRequest("L", "C", 1, true),
                Mockito.mock(HttpServletRequest.class)
        ));
    }

    @Test
    void updateChoice_invalidLabel_throws() {
        UUID qid = UUID.randomUUID();
        Question q = Question.builder().questionId(qid).numberOfCorrectAnswers(1).build();
        Choice existing = Choice.builder().choiceId(UUID.randomUUID()).question(q).version(1).build();
        when(choiceRepository.findById(existing.getChoiceId())).thenReturn(Optional.of(existing));
        when(choiceRepository.findAllVersion(existing.getChoiceId())).thenReturn(List.of(existing));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(q), eq(false))).thenReturn(List.of());
        assertThrows(AppException.class, () -> service.updateChoice(
                qid.toString(),
                existing.getChoiceId().toString(),
                new UpdatedChoiceRequest(null, "C", 1, true),
                Mockito.mock(HttpServletRequest.class)
        ));
    }

    @Test
    void updateChoice_invalidContent_throws() {
        UUID qid = UUID.randomUUID();
        Question q = Question.builder().questionId(qid).numberOfCorrectAnswers(1).build();
        Choice existing = Choice.builder().choiceId(UUID.randomUUID()).question(q).version(1).build();
        when(choiceRepository.findById(existing.getChoiceId())).thenReturn(Optional.of(existing));
        when(choiceRepository.findAllVersion(existing.getChoiceId())).thenReturn(List.of(existing));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(q), eq(false))).thenReturn(List.of());
        assertThrows(AppException.class, () -> service.updateChoice(
                qid.toString(),
                existing.getChoiceId().toString(),
                new UpdatedChoiceRequest("L", "", 1, true),
                Mockito.mock(HttpServletRequest.class)
        ));
    }

    @Test
    void updateChoice_invalidOrder_throws() {
        UUID qid = UUID.randomUUID();
        Question q = Question.builder().questionId(qid).numberOfCorrectAnswers(1).build();
        Choice existing = Choice.builder().choiceId(UUID.randomUUID()).question(q).version(1).build();
        when(choiceRepository.findById(existing.getChoiceId())).thenReturn(Optional.of(existing));
        when(choiceRepository.findAllVersion(existing.getChoiceId())).thenReturn(List.of(existing));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(q), eq(false))).thenReturn(List.of());
        assertThrows(AppException.class, () -> service.updateChoice(
                qid.toString(),
                existing.getChoiceId().toString(),
                new UpdatedChoiceRequest("L", "C", -1, true),
                Mockito.mock(HttpServletRequest.class)
        ));
    }

    @Test
    void updateChoice_isCorrectNull_throws() {
        UUID qid = UUID.randomUUID();
        Question q = Question.builder().questionId(qid).numberOfCorrectAnswers(1).build();
        Choice existing = Choice.builder().choiceId(UUID.randomUUID()).question(q).version(1).build();
        when(choiceRepository.findById(existing.getChoiceId())).thenReturn(Optional.of(existing));
        when(choiceRepository.findAllVersion(existing.getChoiceId())).thenReturn(List.of(existing));
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(q), eq(false))).thenReturn(List.of());
        assertThrows(NullPointerException.class, () -> service.updateChoice(
                qid.toString(),
                existing.getChoiceId().toString(),
                new UpdatedChoiceRequest("L", "C", 1, null),
                Mockito.mock(HttpServletRequest.class)
        ));
    }

    @Test
    void updateChoice_success_createsNewVersionAndUpdatesQuestionCount() {
        UUID qid = UUID.randomUUID();
        Question q = Question.builder().questionId(qid).numberOfCorrectAnswers(0).build();
        Choice existing = Choice.builder().choiceId(UUID.randomUUID()).question(q).version(2).build();
        when(choiceRepository.findById(existing.getChoiceId())).thenReturn(Optional.of(existing));
        when(choiceRepository.findAllVersion(existing.getChoiceId())).thenReturn(List.of(existing));
        // 1 existing correct, new isCorrect true => total 2
        Choice corr = Choice.builder().isCorrect(true).build();
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(q), eq(false))).thenReturn(List.of(corr));
        when(choiceRepository.save(any(Choice.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdatedChoiceRequest updateReq = new UpdatedChoiceRequest("Lx", "Cx", 5, true);
        HttpServletRequest httpReq = Mockito.mock(HttpServletRequest.class);
        QuestionCreationResponse.ChoiceResponse resp = service.updateChoice(qid.toString(), existing.getChoiceId().toString(), updateReq, httpReq);
        assertEquals("Cx", resp.content());
        assertEquals(5, resp.choiceOrder());
        assertTrue(resp.isCorrect());
        assertEquals("Lx", resp.label());
        verify(questionRepository).save(any(Question.class));
    }

    // deleteChoice
    @Test
    void deleteChoice_invalidQuestionUuid_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        assertThrows(AppException.class, () -> service.deleteChoice("not-a-uuid", UUID.randomUUID().toString(), req));
    }

    @Test
    void deleteChoice_questionNotFound_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        UUID qid = UUID.randomUUID();
        when(questionRepository.findById(qid)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.deleteChoice(qid.toString(), UUID.randomUUID().toString(), req));
    }

    @Test
    void deleteChoice_invalidChoiceUuid_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        UUID qid = UUID.randomUUID();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(Question.builder().questionId(qid).build()));
        assertThrows(AppException.class, () -> service.deleteChoice(qid.toString(), "bad-uuid", req));
    }

    @Test
    void deleteChoice_choiceNotFound_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        UUID qid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(Question.builder().questionId(qid).build()));
        when(choiceRepository.findById(cid)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.deleteChoice(qid.toString(), cid.toString(), req));
    }

    @Test
    void deleteChoice_choiceNotInQuestion_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        seedAuth(req, UUID.randomUUID().toString());
        UUID qid = UUID.randomUUID();
        UUID otherQid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        when(questionRepository.findById(qid)).thenReturn(Optional.of(Question.builder().questionId(qid).build()));
        Choice choice = Choice.builder().choiceId(cid).question(Question.builder().questionId(otherQid).build()).build();
        when(choiceRepository.findById(cid)).thenReturn(Optional.of(choice));
        assertThrows(AppException.class, () -> service.deleteChoice(qid.toString(), cid.toString(), req));
    }

    @Test
    void deleteChoice_success_reordersAndUpdates() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String user = UUID.randomUUID().toString();
        seedAuth(req, user);
        UUID qid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        Question question = Question.builder().questionId(qid).build();
        question.setChoices(new java.util.ArrayList<>());
        when(questionRepository.findById(qid)).thenReturn(Optional.of(question));
        Choice deleted = Choice.builder().choiceId(cid).question(question).build();
        when(choiceRepository.findById(cid)).thenReturn(Optional.of(deleted));
        Choice r1 = Choice.builder().choiceId(UUID.randomUUID()).choiceOrder(5).build();
        Choice r2 = Choice.builder().choiceId(UUID.randomUUID()).choiceOrder(2).build();
        when(choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(eq(question), eq(false)))
                .thenReturn(new java.util.ArrayList<>(java.util.List.of(r1, r2)));

        service.deleteChoice(qid.toString(), cid.toString(), req);
        verify(choiceRepository).save(deleted);
        verify(choiceRepository).saveAll(anyList());
        verify(questionRepository).save(question);
        assertNotNull(question.getUpdatedBy());
    }
}

