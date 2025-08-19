package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.Vocabulary;
import com.fptu.sep490.personalservice.model.enumeration.LangGuage;
import com.fptu.sep490.personalservice.repository.VocabularyRepository;
import com.fptu.sep490.personalservice.service.impl.AIServiceImpl;
import com.fptu.sep490.personalservice.service.impl.VocabularyServiceImpl;
import com.fptu.sep490.personalservice.viewmodel.request.VocabularyRequest;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VocabularyServiceImplTest {

    @Mock
    Helper helper;
    @Mock
    VocabularyRepository vocabularyRepository;
    @Mock
    AIServiceImpl aiServiceImpl;

    @InjectMocks
    VocabularyServiceImpl service;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // ===== createVocabulary tests =====

    @Test
    void createVocabulary_nullWord_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        VocabularyRequest request = new VocabularyRequest(null, "context", null, true, LangGuage.ENGLISH);

        AppException ex = assertThrows(AppException.class,
                () -> service.createVocabulary(request, req));

        assertEquals(Constants.ErrorCode.INVALID_REQUEST, ex.getBusinessErrorCode());
    }

    @Test
    void createVocabulary_blankWord_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        VocabularyRequest request = new VocabularyRequest(" ", "context", null, true, LangGuage.ENGLISH);

        AppException ex = assertThrows(AppException.class,
                () -> service.createVocabulary(request, req));

        assertEquals(Constants.ErrorCode.INVALID_REQUEST, ex.getBusinessErrorCode());
    }

    @Test
    void createVocabulary_userUnauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        VocabularyRequest request = new VocabularyRequest("word", "context", null, true, LangGuage.ENGLISH);

        AppException ex = assertThrows(AppException.class,
                () -> service.createVocabulary(request, req));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void createVocabulary_meaningAutoFilled_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        VocabularyRequest request = new VocabularyRequest("word", "context", null, true, LangGuage.ENGLISH);

        // Mock AI service
        when(aiServiceImpl.getVocabularyDefinition("word", "context", LangGuage.ENGLISH)).thenReturn("auto meaning");

        VocabularyResponse response = service.createVocabulary(request, req);

        assertNotNull(response);
        assertEquals("word", response.word());
        assertEquals("context", response.context());
        assertEquals("auto meaning", response.meaning());
        assertEquals("user1", response.createdBy());

        verify(vocabularyRepository, times(1)).save(any(Vocabulary.class));
    }

    @Test
    void createVocabulary_withMeaningProvided_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        VocabularyRequest request = new VocabularyRequest("word", "context", "given meaning", true, LangGuage.ENGLISH);

        VocabularyResponse response = service.createVocabulary(request, req);

        assertNotNull(response);
        assertEquals("given meaning", response.meaning());
        verify(aiServiceImpl, never()).getVocabularyDefinition(anyString(), anyString(), eq(LangGuage.ENGLISH));
        verify(vocabularyRepository, times(1)).save(any(Vocabulary.class));
    }
    // ===== getVocabularyById tests =====

    @Test
    void getVocabularyById_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.getVocabularyById(UUID.randomUUID().toString(), req));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void getVocabularyById_notFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        UUID vocabId = UUID.randomUUID();
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.getVocabularyById(vocabId.toString(), req));

        assertEquals(Constants.ErrorCodeMessage.NOT_FOUND, ex.getMessage());
    }

    @Test
    void getVocabularyById_deleted_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setIsDeleted(true);

        when(vocabularyRepository.findById(vocab.getWordId())).thenReturn(Optional.of(vocab));

        AppException ex = assertThrows(AppException.class,
                () -> service.getVocabularyById(vocab.getWordId().toString(), req));

        assertEquals(Constants.ErrorCodeMessage.NOT_FOUND, ex.getMessage());
    }

    @Test
    void getVocabularyById_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("word");
        vocab.setContext("context");
        vocab.setMeaning("meaning");
        vocab.setCreatedBy("user1");
        vocab.setIsDeleted(false);

        when(vocabularyRepository.findById(vocab.getWordId())).thenReturn(Optional.of(vocab));

        VocabularyResponse response = service.getVocabularyById(vocab.getWordId().toString(), req);

        assertNotNull(response);
        assertEquals(vocab.getWord(), response.word());
        assertEquals(vocab.getMeaning(), response.meaning());
        assertEquals(vocab.getCreatedBy(), response.createdBy());
    }

// ===== deleteVocabularyById tests =====

    @Test
    void deleteVocabularyById_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.deleteVocabularyById(UUID.randomUUID().toString(), req));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void deleteVocabularyById_notFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        UUID vocabId = UUID.randomUUID();
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.deleteVocabularyById(vocabId.toString(), req));

        assertEquals(Constants.ErrorCodeMessage.NOT_FOUND, ex.getMessage());
    }

    @Test
    void deleteVocabularyById_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setIsDeleted(false);

        when(vocabularyRepository.findById(vocab.getWordId())).thenReturn(Optional.of(vocab));

        service.deleteVocabularyById(vocab.getWordId().toString(), req);

        assertTrue(vocab.getIsDeleted());
        verify(vocabularyRepository, times(1)).save(vocab);
    }
    @Test
    void getAllVocabulary_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.getAllVocabulary(req, 0, 10, null, null, "keyword"));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void getAllVocabulary_defaultSorting_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("word");
        vocab.setContext("context");
        vocab.setMeaning("meaning");
        vocab.setCreatedBy("user1");

        Page<Vocabulary> vocabPage = new PageImpl<>(List.of(vocab));
        when(vocabularyRepository.searchVocabulary(eq("keyword"), any(Pageable.class), eq("user1")))
                .thenReturn(vocabPage);

        Page<VocabularyResponse> result = service.getAllVocabulary(req, 0, 10, null, null, "keyword");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("word", result.getContent().get(0).word());
    }

    @Test
    void getAllVocabulary_descendingSorting_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("word");
        vocab.setContext("context");
        vocab.setMeaning("meaning");
        vocab.setCreatedBy("user1");

        Page<Vocabulary> vocabPage = new PageImpl<>(List.of(vocab));
        when(vocabularyRepository.searchVocabulary(eq("keyword"), any(Pageable.class), eq("user1")))
                .thenReturn(vocabPage);

        Page<VocabularyResponse> result = service.getAllVocabulary(req, 0, 10, "createdAt", "desc", "keyword");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("word", result.getContent().get(0).word());
    }

    @Test
    void getAllVocabulary_emptyResult_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Page<Vocabulary> vocabPage = new PageImpl<>(List.of());
        when(vocabularyRepository.searchVocabulary(eq("empty"), any(Pageable.class), eq("user1")))
                .thenReturn(vocabPage);

        Page<VocabularyResponse> result = service.getAllVocabulary(req, 0, 10, null, null, "empty");

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAllVocabulary_repositoryThrows_throwsAppException() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        when(vocabularyRepository.searchVocabulary(eq("keyword"), any(Pageable.class), eq("user1")))
                .thenThrow(new RuntimeException("DB error"));

        AppException ex = assertThrows(AppException.class,
                () -> service.getAllVocabulary(req, 0, 10, null, null, "keyword"));

        assertEquals(Constants.ErrorCode.INTERNAL_SERVER_ERROR, ex.getBusinessErrorCode());
    }

    @Test
    void updateVocabulary_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateVocabulary(UUID.randomUUID().toString(),
                        new VocabularyRequest("word", "context", "meaning", true, LangGuage.ENGLISH),
                        req));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void updateVocabulary_notFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        UUID vocabId = UUID.randomUUID();
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.updateVocabulary(vocabId.toString(),
                        new VocabularyRequest("word", "context", "meaning", true, LangGuage.ENGLISH),
                        req));

        assertEquals(Constants.ErrorCodeMessage.NOT_FOUND, ex.getMessage());
    }

    @Test
    void updateVocabulary_forbidden_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user2");
        UUID vocabId = UUID.randomUUID();

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(vocabId);
        vocab.setCreatedBy("user1"); // different user
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));

        AppException ex = assertThrows(AppException.class,
                () -> service.updateVocabulary(vocabId.toString(),
                        new VocabularyRequest("word", "context", "meaning", true, LangGuage.ENGLISH),
                        req));

        assertEquals(Constants.ErrorCodeMessage.FORBIDDEN, ex.getMessage());
    }

    @Test
    void updateVocabulary_invalidWord_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        UUID vocabId = UUID.randomUUID();

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(vocabId);
        vocab.setCreatedBy("user1");
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));

        VocabularyRequest request = new VocabularyRequest("", "context", "meaning", true, LangGuage.ENGLISH);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateVocabulary(vocabId.toString(), request, req));

        assertEquals(Constants.ErrorCode.INVALID_REQUEST, ex.getBusinessErrorCode());
    }

    @Test
    void updateVocabulary_duplicateWord_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        UUID vocabId = UUID.randomUUID();

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(vocabId);
        vocab.setCreatedBy("user1");
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));

        Vocabulary duplicate = new Vocabulary();
        duplicate.setWordId(UUID.randomUUID()); // different id, same word
        when(vocabularyRepository.findByWordAndCreatedBy("word", "user1")).thenReturn(duplicate);

        VocabularyRequest request = new VocabularyRequest("word", "context", "meaning", true, LangGuage.ENGLISH);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateVocabulary(vocabId.toString(), request, req));

        assertEquals(Constants.ErrorCode.VOCABULARY_ALREADY_EXISTS, ex.getBusinessErrorCode());
    }

    @Test
    void updateVocabulary_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        UUID vocabId = UUID.randomUUID();

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(vocabId);
        vocab.setCreatedBy("user1");
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));
        when(vocabularyRepository.findByWordAndCreatedBy("newWord", "user1")).thenReturn(null);

        VocabularyRequest request = new VocabularyRequest("newWord", "newContext", "newMeaning", true, LangGuage.ENGLISH);

        VocabularyResponse response = service.updateVocabulary(vocabId.toString(), request, req);

        assertNotNull(response);
        assertEquals("newWord", response.word());
        assertEquals("newContext", response.context());
        assertEquals("newMeaning", response.meaning());
        verify(vocabularyRepository, times(1)).save(vocab);
    }



}
