package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.*;
import com.fptu.sep490.personalservice.model.Module;
import com.fptu.sep490.personalservice.model.enumeration.LearningStatus;
import com.fptu.sep490.personalservice.model.enumeration.ModuleUserStatus;
import com.fptu.sep490.personalservice.repository.*;
import com.fptu.sep490.personalservice.repository.client.AuthClient;
import com.fptu.sep490.personalservice.viewmodel.request.*;
import com.fptu.sep490.personalservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ModuleServiceImplTest {
    @Mock
    FlashCardRepository flashCardRepository;
    @Mock
    ModuleRepository moduleRepository;
    @Mock
    VocabularyRepository vocabularyRepository;
    @Mock
    FlashCardModuleRepository flashCardModuleRepository;
    @Mock
    ModuleUsersRepository moduleUsersRepository;
    @Mock
    FlashCardProgressRepository flashCardProgressRepository;

    @Mock
    AuthClient authClient;

    @Mock
    Helper helper;

    @InjectMocks
    ModuleServiceImpl service;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void createModule_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        ModuleRequest request = ModuleRequest.builder()
                .moduleName("Module1")
                .moduleDescription("Description")
                .vocabularyIds(List.of())
                .isPublic(true)
                .build();

        Exception ex = assertThrows(Exception.class,
                () -> service.createModule(request, req));

        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void createModule_vocabularyNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID vocabId = UUID.randomUUID();
        ModuleRequest request = ModuleRequest.builder()
                .moduleName("Module1")
                .moduleDescription("Description")
                .vocabularyIds(List.of(vocabId))
                .isPublic(true)
                .build();

        Module module = new Module();
        module.setModuleId(UUID.randomUUID());
        module.setModuleName(request.moduleName());
        module.setDescription(request.moduleDescription());
        module.setCreatedBy("user1");
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> service.createModule(request, req));

        assertTrue(ex.getMessage().contains("Vocabulary not found"));
    }

    @Test
    void createModule_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID vocabId = UUID.randomUUID();

        // Module request with one vocabulary
        ModuleRequest request = ModuleRequest.builder()
                .moduleName("Module1")
                .moduleDescription("Description")
                .vocabularyIds(List.of(vocabId))
                .isPublic(true)
                .build();

        // Mock module repository save
        Module module = new Module();
        module.setModuleId(UUID.randomUUID());
        module.setModuleName(request.moduleName());
        module.setDescription(request.moduleDescription());
        module.setCreatedBy("user1");
        module.setFlashCardModules(new ArrayList<>()); // initialize list
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        // Mock vocabulary repository
        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(vocabId);
        vocab.setWord("word");
        vocab.setContext("context");
        vocab.setMeaning("meaning");
        vocab.setCreatedBy("user1");
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));

        // Mock flashCardRepository: save generates a cardId
        when(flashCardRepository.findByVocabularyId(vocabId, "user1")).thenReturn(Optional.empty());
        when(flashCardRepository.save(any(FlashCard.class))).thenAnswer(invocation -> {
            FlashCard card = invocation.getArgument(0);
            card.setCardId(UUID.randomUUID()); // <-- important: avoid NPE
            card.setFlashCardModules(new ArrayList<>()); // initialize list
            return card;
        });

        // Mock ModuleUsersRepository save
        when(moduleUsersRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock FlashCardModuleRepository save
        when(flashCardModuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Call service
        ModuleResponse response = service.createModule(request, req);

        // Verify results
        assertNotNull(response);
        assertEquals("Module1", response.moduleName());
        assertEquals("Description", response.description());
        assertEquals("user1", response.createdBy());
        assertEquals(1, response.flashCardIds().size());
        assertEquals("word", response.flashCardIds().get(0).vocabularyResponse().word());

        // Verify that repositories were called
        verify(moduleRepository, times(1)).save(any());
        verify(moduleUsersRepository, times(1)).save(any());
        verify(flashCardRepository, times(1)).save(any());
        verify(flashCardModuleRepository, times(1)).save(any());
    }

    @Test
    void getAllModules_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.getAllModules(req, 0, 10, "moduleName", "asc", ""));

        assertEquals("Unauthorized", ex.getMessage());
    }
    @Test
    void getAllModules_success_returnsPage() throws Exception {
        // Mock request
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Prepare Module entity
        Module module = new Module();
        module.setModuleId(UUID.randomUUID());
        module.setModuleName("Test Module");
        module.setModuleUsers(new HashSet<>());
        module.setFlashCardModules(new ArrayList<>());
        // Prepare Vocabulary
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWordId(UUID.randomUUID());
        vocabulary.setWord("test");
        vocabulary.setContext("context");
        vocabulary.setMeaning("meaning");
        vocabulary.setCreatedBy(userId);
        vocabulary.setCreatedAt(LocalDateTime.now());
        // Prepare FlashCard and link via FlashCardModule
        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(UUID.randomUUID());
        flashCard.setVocabulary(vocabulary); // <--- attach vocabulary here


        FlashCardModule flashCardModule = new FlashCardModule();
        flashCardModule.setModule(module);
        flashCardModule.setFlashCard(flashCard);

        module.getFlashCardModules().add(flashCardModule);

        // Prepare ModuleUsers
        ModuleUsers moduleUser = new ModuleUsers();
        moduleUser.setUserId(userId);
        moduleUser.setModule(module);
        module.getModuleUsers().add(moduleUser);

        // Mock repository to return a page of modules
        Page<Module> modulePage = new PageImpl<>(List.of(module));
        when(moduleRepository.searchModuleByUser(eq(""), any(Pageable.class), eq(userId)))
                .thenReturn(modulePage);

        // Call service
        Page<ModuleResponse> responsePage = service.getAllModules(
                requestMock, 0, 10, null, null, ""
        );

        // Assertions
        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());

        ModuleResponse response = responsePage.getContent().get(0);
        assertEquals(module.getModuleId(), response.moduleId());
        assertEquals(module.getModuleName(), response.moduleName());
        assertEquals(1, response.flashCardIds().size()); // One flash card
    }


    @Test
    void getAllPublicModules_success_returnsPage() throws Exception {
        // Mock request
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Prepare Module entity
        Module module = new Module();
        module.setModuleId(UUID.randomUUID());
        module.setModuleName("Public Module");
        module.setDescription("Description");
        module.setIsPublic(true);
        module.setFlashCardModules(new ArrayList<>());

        // Prepare Vocabulary
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWordId(UUID.randomUUID());
        vocabulary.setWord("test");
        vocabulary.setContext("context");
        vocabulary.setMeaning("meaning");
        vocabulary.setCreatedBy(userId);
        vocabulary.setCreatedAt(LocalDateTime.now());

        // Prepare FlashCard and attach Vocabulary
        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(UUID.randomUUID());
        flashCard.setVocabulary(vocabulary);

        // Link FlashCard via FlashCardModule
        FlashCardModule flashCardModule = new FlashCardModule();
        flashCardModule.setModule(module);
        flashCardModule.setFlashCard(flashCard);
        module.getFlashCardModules().add(flashCardModule);

        // Mock repository to return a page of modules
        Page<Module> modulePage = new PageImpl<>(List.of(module));
        when(moduleRepository.searchMyAndPublicModules(eq(""), any(Pageable.class), eq(userId)))
                .thenReturn(modulePage);

        // Call service
        Page<ModuleResponse> responsePage = service.getAllPublicModules(
                0, 10, null, null, "", requestMock
        );

        // Assertions
        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());

        ModuleResponse response = responsePage.getContent().get(0);
        assertEquals(module.getModuleId(), response.moduleId());
        assertEquals(module.getModuleName(), response.moduleName());
        assertTrue(response.isPublic());
        assertEquals(1, response.flashCardIds().size());

        FlashCardResponse flashCardResponse = response.flashCardIds().get(0);
        assertEquals(flashCard.getCardId().toString(), flashCardResponse.flashCardId());
        assertEquals(vocabulary.getWordId(), flashCardResponse.vocabularyResponse().vocabularyId());
        assertEquals(vocabulary.getWord(), flashCardResponse.vocabularyResponse().word());
    }

    @Test
    void deleteModuleById_success() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setCreatedBy(userId);
        module.setIsDeleted(false);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenReturn(module);

        service.deleteModuleById(moduleId.toString(), requestMock);

        // Verify that the module is marked as deleted and saved
        assertTrue(module.getIsDeleted());
        verify(moduleRepository).save(module);
    }

    @Test
    void deleteModuleById_unauthorized_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);

        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () ->
                service.deleteModuleById(UUID.randomUUID().toString(), requestMock)
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getHttpStatusCode());
    }
    @Test
    void deleteModuleById_notFound_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                service.deleteModuleById(moduleId.toString(), requestMock)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }
    @Test
    void deleteModuleById_forbidden_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setCreatedBy("anotherUser");

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        AppException ex = assertThrows(AppException.class, () ->
                service.deleteModuleById(moduleId.toString(), requestMock)
        );

        assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpStatusCode());
    }
    @Test
    void getModuleById_success_returnsModule() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setModuleName("Test Module");
        module.setCreatedBy(userId);
        module.setIsPublic(false);
        module.setIsDeleted(false);

        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(UUID.randomUUID());

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("Hello");
        vocab.setMeaning("Xin chào");
        vocab.setContext("Greeting");
        vocab.setCreatedBy(userId);
        vocab.setCreatedAt(LocalDateTime.now());
        flashCard.setVocabulary(vocab);

        FlashCardModule fcm = new FlashCardModule();
        fcm.setModule(module);
        fcm.setFlashCard(flashCard);
        module.getFlashCardModules().add(fcm);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(new ModuleUsers()));

        ModuleResponse response = service.getModuleById(moduleId.toString(), requestMock);

        assertNotNull(response);
        assertEquals(module.getModuleId(), response.moduleId());
        assertEquals(1, response.flashCardIds().size());
        assertEquals("Hello", response.flashCardIds().get(0).vocabularyResponse().word());
    }

    @Test
    void getModuleById_unauthorized_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () ->
                service.getModuleById(UUID.randomUUID().toString(), requestMock)
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getHttpStatusCode());
    }

    @Test
    void getModuleById_notFound_throwsException_whenModuleMissing() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                service.getModuleById(moduleId.toString(), requestMock)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void getModuleById_notFound_throwsException_whenModuleDeleted() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(true);

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        AppException ex = assertThrows(AppException.class, () ->
                service.getModuleById(moduleId.toString(), requestMock)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void getModuleById_forbidden_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setCreatedBy("anotherUser");
        module.setIsPublic(false);
        module.setIsDeleted(false);

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                service.getModuleById(moduleId.toString(), requestMock)
        );

        assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpStatusCode());
    }
    @Test
    void updateModule_success_updatesModule() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID vocabId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setCreatedBy(userId);
        module.setIsDeleted(false);
        module.setFlashCardModules(new ArrayList<>());

        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(vocabId);
        vocab.setWord("Hello");
        vocab.setMeaning("Xin chào");
        vocab.setCreatedBy(userId);
        vocab.setCreatedAt(LocalDateTime.now());

        ModuleRequest request = new ModuleRequest("New Name", "New Desc", List.of(vocabId), true);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));
        when(flashCardRepository.findByVocabularyId(vocabId, userId)).thenReturn(Optional.empty());
        when(flashCardRepository.save(any(FlashCard.class))).thenAnswer(i -> i.getArgument(0));
        when(flashCardModuleRepository.existsByFlashCardAndModule(any(), any())).thenReturn(false);
        when(flashCardModuleRepository.save(any(FlashCardModule.class))).thenAnswer(i -> i.getArgument(0));
        when(moduleRepository.save(any(Module.class))).thenAnswer(i -> i.getArgument(0));
        // Fix: assign UUID to FlashCard when saving
        when(flashCardRepository.save(any(FlashCard.class))).thenAnswer(invocation -> {
            FlashCard flashCard = invocation.getArgument(0);
            if (flashCard.getCardId() == null) {
                flashCard.setCardId(UUID.randomUUID());
            }
            return flashCard;
        });
        ModuleResponse response = service.updateModule(moduleId.toString(), request, requestMock);

        assertNotNull(response);
        assertEquals("New Name", response.moduleName());
        assertEquals("New Desc", response.description());
        assertTrue(response.isPublic());
        assertEquals(1, response.flashCardIds().size());
        assertEquals(vocabId, response.flashCardIds().get(0).vocabularyResponse().vocabularyId());
    }

    @Test
    void updateModule_unauthorized_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () ->
                service.updateModule(UUID.randomUUID().toString(),
                        new ModuleRequest("Name", "Desc", List.of(), true), requestMock)
        );

        assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getHttpStatusCode());
    }

    @Test
    void updateModule_notFound_throwsException_whenModuleMissing() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                service.updateModule(moduleId.toString(),
                        new ModuleRequest("Name", "Desc", List.of(),true), requestMock)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void updateModule_forbidden_throwsException() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setCreatedBy("anotherUser");
        module.setIsDeleted(false);

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        AppException ex = assertThrows(AppException.class, () ->
                service.updateModule(moduleId.toString(),
                        new ModuleRequest("Name", "Desc", List.of(), true), requestMock)
        );

        assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpStatusCode());
    }
    @Test
    void getAllSharedModules_success_returnsPage() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Prepare Module
        Module module = new Module();
        module.setModuleId(UUID.randomUUID());
        module.setModuleName("Shared Module");
        module.setDescription("Shared Desc");
        module.setIsPublic(true);
        module.setFlashCardModules(new ArrayList<>());

        // Prepare Vocabulary
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWordId(UUID.randomUUID());
        vocabulary.setWord("Hello");
        vocabulary.setMeaning("Xin chào");
        vocabulary.setCreatedBy("user2");
        vocabulary.setCreatedAt(LocalDateTime.now());

        // Prepare FlashCard
        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(UUID.randomUUID());
        flashCard.setVocabulary(vocabulary);

        // Link via FlashCardModule
        FlashCardModule flashCardModule = new FlashCardModule();
        flashCardModule.setModule(module);
        flashCardModule.setFlashCard(flashCard);
        module.getFlashCardModules().add(flashCardModule);

        // Prepare ModuleUsers
        ModuleUsers moduleUser = new ModuleUsers();
        moduleUser.setModule(module);
        moduleUser.setUserId("user3");
        moduleUser.setStatus(1);

        // Mock repository
        Page<ModuleUsers> modulePage = new PageImpl<>(List.of(moduleUser));
        when(moduleUsersRepository.searchShareModules(eq(""), any(Pageable.class), eq(userId), eq(1)))
                .thenReturn(modulePage);

        // Mock helper profile
        UserProfileResponse creatorProfile = UserProfileResponse.builder().email("creator@example.com").build();
        UserProfileResponse shareToProfile = UserProfileResponse.builder().email("shareto@example.com").build();
        when(helper.getUserProfileById(module.getCreatedBy())).thenReturn(creatorProfile);
        when(helper.getUserProfileById(moduleUser.getUserId())).thenReturn(shareToProfile);

        // Call service
        Page<ModuleUserResponse> responsePage = service.getAllSharedModules(requestMock, 0, 10, "moduleName", "asc", "", 1);

        // Assertions
        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());

        ModuleUserResponse response = responsePage.getContent().get(0);
        assertEquals(module.getModuleId(), response.moduleId());
        assertEquals("Shared Module", response.moduleName());
        assertEquals(1, response.flashCardIds().size());
        assertEquals(flashCard.getCardId().toString(), response.flashCardIds().get(0).flashCardId());
        assertEquals(vocabulary.getWordId(), response.flashCardIds().get(0).vocabularyResponse().vocabularyId());
        assertEquals("creator@example.com", response.createdBy());
        assertEquals("shareto@example.com", response.shareTo());
        assertEquals(1, response.status());
    }

    @Test
    void updateSharedModuleRequest_success_acceptsModule() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUser = new ModuleUsers();
        moduleUser.setUserId(userId);
        moduleUser.setStatus(0); // pending
        module.setModuleUsers(Set.of(moduleUser));

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Accept module
        service.updateSharedModuleRequest(moduleId.toString(), ModuleUserStatus.ACCEPTED.ordinal(), requestMock);

        assertEquals(1, moduleUser.getStatus()); // 1 = accepted
        verify(moduleUsersRepository, times(1)).save(moduleUser);
    }

    @Test
    void updateSharedModuleRequest_success_rejectsModule() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUser = new ModuleUsers();
        moduleUser.setUserId(userId);
        moduleUser.setStatus(0); // pending
        module.setModuleUsers(Set.of(moduleUser));

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Reject module
        service.updateSharedModuleRequest(moduleId.toString(), ModuleUserStatus.REJECTED.ordinal(), requestMock);

        assertEquals(2, moduleUser.getStatus()); // 2 = rejected
        verify(moduleUsersRepository, times(1)).save(moduleUser);
    }

    @Test
    void updateSharedModuleRequest_unauthorized_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        UUID moduleId = UUID.randomUUID();

        AppException ex = assertThrows(AppException.class,
                () -> service.updateSharedModuleRequest(moduleId.toString(), ModuleUserStatus.ACCEPTED.ordinal(), requestMock));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void updateSharedModuleRequest_alreadyAccepted_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUser = new ModuleUsers();
        moduleUser.setUserId(userId);
        moduleUser.setStatus(1); // already accepted
        module.setModuleUsers(Set.of(moduleUser));

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        AppException ex = assertThrows(AppException.class,
                () -> service.updateSharedModuleRequest(moduleId.toString(), ModuleUserStatus.ACCEPTED.ordinal(), requestMock));

    }
    @Test
    void getAllMySharedModules_success_returnsPage() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Prepare Module
        Module module = new Module();
        module.setModuleId(UUID.randomUUID());
        module.setModuleName("Shared Module");
        module.setDescription("Description");
        module.setIsPublic(true);
        module.setFlashCardModules(new ArrayList<>());

        // Prepare Vocabulary
        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("test");
        vocab.setContext("context");
        vocab.setMeaning("meaning");
        vocab.setCreatedBy(userId);
        vocab.setCreatedAt(LocalDateTime.now());

        // Prepare FlashCard
        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(UUID.randomUUID());
        flashCard.setVocabulary(vocab);

        // Link FlashCard via FlashCardModule
        FlashCardModule flashCardModule = new FlashCardModule();
        flashCardModule.setModule(module);
        flashCardModule.setFlashCard(flashCard);
        module.getFlashCardModules().add(flashCardModule);

        // Prepare ModuleUsers
        ModuleUsers moduleUser = new ModuleUsers();
        moduleUser.setUserId(userId);
        moduleUser.setModule(module);
        moduleUser.setStatus(1);
        module.setModuleUsers(Set.of(moduleUser));

        // Mock repository to return a page
        Page<ModuleUsers> modulePage = new PageImpl<>(List.of(moduleUser));
        when(moduleUsersRepository.searchMyShareModules(eq(""), any(Pageable.class), eq(userId)))
                .thenReturn(modulePage);

        // Mock helper to return user profiles
        when(helper.getUserProfileById(module.getCreatedBy()))
                .thenReturn(UserProfileResponse.builder().email("creator@test.com").build());
        when(helper.getUserProfileById(moduleUser.getUserId()))
                .thenReturn(UserProfileResponse.builder().email("user@test.com").build());

        // Call service
        Page<ModuleUserResponse> responsePage = service.getAllMySharedModules(
                requestMock, 0, 10, null, null, ""
        );

        // Assertions
        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());

        ModuleUserResponse response = responsePage.getContent().get(0);
        assertEquals(module.getModuleId(), response.moduleId());
        assertEquals(module.getModuleName(), response.moduleName());
        assertEquals("creator@test.com", response.createdBy());
        assertEquals("user@test.com", response.shareTo());
        assertEquals(1, response.flashCardIds().size());
        assertEquals(vocab.getWordId(), response.flashCardIds().get(0).vocabularyResponse().vocabularyId());
    }

    @Test
    void getAllMySharedModules_unauthorized_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.getAllMySharedModules(requestMock, 0, 10, "moduleName", "asc", "")
        );

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void cloneModule_success_clonesModule() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Original module
        Module originalModule = new Module();
        originalModule.setModuleId(moduleId);
        originalModule.setModuleName("Original Module");
        originalModule.setDescription("Desc");
        originalModule.setIsPublic(true);
        originalModule.setFlashCardModules(new ArrayList<>());

        // Vocabulary and FlashCard
        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("Word");
        vocab.setMeaning("Meaning");
        vocab.setCreatedBy("otherUser");
        vocab.setCreatedAt(LocalDateTime.now());

        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(UUID.randomUUID());
        flashCard.setVocabulary(vocab);

        FlashCardModule flashCardModule = new FlashCardModule();
        flashCardModule.setModule(originalModule);
        flashCardModule.setFlashCard(flashCard);
        originalModule.getFlashCardModules().add(flashCardModule);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(originalModule));

        // Clone new module
        when(moduleRepository.save(any(Module.class))).thenAnswer(i -> {
            Module m = i.getArgument(0);
            m.setModuleId(UUID.randomUUID());
            m.setFlashCardModules(new ArrayList<>());
            return m;
        });

        when(flashCardRepository.findByVocabularyId(vocab.getWordId(), userId)).thenReturn(Optional.empty());
        when(flashCardRepository.save(any(FlashCard.class))).thenAnswer(i -> {
            FlashCard c = i.getArgument(0);
            c.setCardId(UUID.randomUUID());
            c.setFlashCardModules(new ArrayList<>());
            return c;
        });

        when(flashCardModuleRepository.save(any(FlashCardModule.class))).thenAnswer(i -> i.getArgument(0));

        // Call service
        ModuleResponse response = service.cloneModule(moduleId.toString(), requestMock);

        assertNotNull(response);
        assertTrue(response.moduleName().contains("Copy"));
        assertEquals(1, response.flashCardIds().size());
        assertEquals(vocab.getWordId(), response.flashCardIds().get(0).vocabularyResponse().vocabularyId());

        // Verify repository calls
        verify(moduleRepository, times(1)).save(any());
        verify(flashCardRepository, times(1)).save(any());
        verify(flashCardModuleRepository, times(1)).save(any());
    }

    @Test
    void cloneModule_unauthorized_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.cloneModule(UUID.randomUUID().toString(), requestMock)
        );

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void cloneModule_forbidden_throwsIfNotPublicAndNoAccess() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsPublic(false);
        module.setFlashCardModules(new ArrayList<>());

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.cloneModule(moduleId.toString(), requestMock)
        );

    }
    @Test
    void getModuleProgress_success() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID flashCardId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Module setup
        Module module = new Module();
        module.setModuleId(moduleId);
        module.setModuleName("Test Module");
        module.setIsDeleted(false);
        module.setFlashCardModules(new ArrayList<>());

        // Vocabulary & FlashCard
        Vocabulary vocab = new Vocabulary();
        vocab.setWordId(UUID.randomUUID());
        vocab.setWord("Word");
        vocab.setMeaning("Meaning");
        vocab.setCreatedBy("otherUser");
        vocab.setCreatedAt(LocalDateTime.now());

        FlashCard flashCard = new FlashCard();
        flashCard.setCardId(flashCardId);
        flashCard.setVocabulary(vocab);

        FlashCardModule flashCardModule = new FlashCardModule();
        flashCardModule.setModule(module);
        flashCardModule.setFlashCard(flashCard);
        module.getFlashCardModules().add(flashCardModule);

        // ModuleUsers
        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(UUID.randomUUID().toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setModule(module);
        moduleUsers.setFlashcardProgressList(new ArrayList<>());
        moduleUsers.setStatus(1); // accepted

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));
        when(flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUsers.getId(), flashCardId.toString()))
                .thenReturn(Optional.empty());
        when(flashCardProgressRepository.save(any(FlashCardProgress.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(flashCardRepository.findById(flashCardId)).thenReturn(Optional.of(flashCard));

        ModuleProgressResponse response = service.getModuleProgress(moduleId.toString(), requestMock);

        assertNotNull(response);
        assertEquals(moduleId.toString(), response.moduleId());
        assertEquals(userId, response.userId());
        assertEquals(1, response.flashcardProgresses().size());
        assertEquals(flashCardId, response.flashcardProgresses().get(0).flashcardDetail().flashCardId() != null
                ? UUID.fromString(response.flashcardProgresses().get(0).flashcardDetail().flashCardId())
                : null);
    }

    @Test
    void getModuleProgress_unauthorized_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.getModuleProgress(UUID.randomUUID().toString(), requestMock));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void getModuleProgress_forbiddenIfNotAccepted() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(UUID.randomUUID().toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setStatus(2); // denied

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));

        AppException ex = assertThrows(AppException.class,
                () -> service.getModuleProgress(moduleId.toString(), requestMock));

        assertTrue(ex.getMessage().contains("denied or still not accept"));
    }

    @Test
    void getModuleProgress_moduleNotFound_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn("user1");

        when(moduleRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.getModuleProgress(UUID.randomUUID().toString(), requestMock));

    }
    @Test
    void updateModuleProgress_success() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID flashCardId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Module setup
        Module module = new Module();
        module.setModuleId(moduleId);
        module.setModuleName("Test Module");
        module.setIsDeleted(false);

        // ModuleUsers setup
        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(UUID.randomUUID().toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setFlashcardProgressList(new ArrayList<>());
        moduleUsers.setStatus(1);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(i -> i.getArgument(0));
        when(flashCardProgressRepository.findByModuleUserIdAndFlashcardId(any(), any())).thenReturn(Optional.empty());
        when(flashCardProgressRepository.save(any(FlashCardProgress.class))).thenAnswer(i -> i.getArgument(0));


        ModuleProgressRequest progressRequest = ModuleProgressRequest.builder()
                .status(1)
                .timeSpent(120L)
                .progress(50.0)
                .lastIndexRead(2)
                .highlightedFlashcardIds(List.of(flashCardId.toString()))
                .learningStatus("LEARNING")
                .build();

        ModuleProgressResponse response = service.updateModuleProgress(moduleId.toString(), progressRequest, requestMock);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(120L, response.timeSpent());
        assertEquals(50, response.progress());
        assertEquals(2, response.lastIndexRead());
        assertEquals(1, response.status());
        assertEquals(LearningStatus.LEARNING, response.learningStatus());
        assertEquals(1, response.flashcardProgresses().size());
    }

    @Test
    void updateModuleProgress_unauthorized_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateModuleProgress(UUID.randomUUID().toString(), mock(ModuleProgressRequest.class), requestMock));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void updateModuleProgress_forbiddenIfDenied() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(UUID.randomUUID().toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setStatus(2); // denied

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));

        AppException ex = assertThrows(AppException.class,
                () -> service.updateModuleProgress(moduleId.toString(), mock(ModuleProgressRequest.class), requestMock));

        assertTrue(ex.getMessage().contains("denied or still not accept"));
    }

    @Test
    void updateModuleProgress_moduleNotFound_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn("user1");

        when(moduleRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.updateModuleProgress(UUID.randomUUID().toString(), mock(ModuleProgressRequest.class), requestMock));

    }
    @Test
    void updateFlashcardProgress_successCorrectAnswer() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID moduleUserId = UUID.randomUUID();
        String flashcardId = UUID.randomUUID().toString();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(moduleUserId.toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setFlashcardProgressList(new ArrayList<>());
        moduleUsers.setLastIndexRead(0);
        moduleUsers.setProgress(0.0);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(i -> i.getArgument(0));
        when(flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUserId.toString(), flashcardId)).thenReturn(Optional.empty());
        when(flashCardProgressRepository.save(any(FlashCardProgress.class))).thenAnswer(i -> i.getArgument(0));

        FlashcardProgressRequest progressRequest = FlashcardProgressRequest.builder()
                .flashcardId(flashcardId)
                .isCorrect(true)
                .isHighlighted(false)
                .build();

        service.updateFlashcardProgress(moduleId.toString(), progressRequest, requestMock);

        // Verify lastIndexRead incremented
        assertEquals(1, moduleUsers.getLastIndexRead());
        // Verify progress updated
        assertEquals(100.0, moduleUsers.getProgress());
        // Verify learningStatus set to MASTERED
        assertEquals(LearningStatus.MASTERED, moduleUsers.getLearningStatus());
    }

    @Test
    void updateFlashcardProgress_unauthorized_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn(null);

        FlashcardProgressRequest request = mock(FlashcardProgressRequest.class);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateFlashcardProgress(UUID.randomUUID().toString(), request, requestMock));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void updateFlashcardProgress_moduleNotFound_throws() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(requestMock)).thenReturn("user1");

        when(moduleRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        FlashcardProgressRequest request = mock(FlashcardProgressRequest.class);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateFlashcardProgress(UUID.randomUUID().toString(), request, requestMock));

        assertEquals("NOT_FOUND", ex.getMessage());
    }

    @Test
    void updateFlashcardProgress_forbiddenIfDenied() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID moduleUserId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(moduleUserId.toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setStatus(2); // Denied

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));

        FlashcardProgressRequest request = mock(FlashcardProgressRequest.class);

        AppException ex = assertThrows(AppException.class,
                () -> service.updateFlashcardProgress(moduleId.toString(), request, requestMock));

        assertTrue(ex.getMessage().contains("denied access"));
    }

    @Test
    void updateFlashcardProgress_highlightedFlashcard() throws Exception {
        // Mocks and IDs
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID moduleUserId = UUID.randomUUID();
        String flashcardId = UUID.randomUUID().toString();

        // Mock user token
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Module setup
        Module module = new Module();
        module.setModuleId(moduleId);
        module.setIsDeleted(false);

        // ModuleUser setup
        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(moduleUserId.toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setFlashcardProgressList(new ArrayList<>());
        moduleUsers.setLastIndexRead(0);

        // Mock repository responses
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(i -> i.getArgument(0));

        // Mock flashCardProgressRepository: first call (no existing progress)
        when(flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUserId.toString(), flashcardId))
                .thenReturn(Optional.empty());

        // Mock flashCardProgressRepository save
        when(flashCardProgressRepository.save(any(FlashCardProgress.class)))
                .thenAnswer(i -> {
                    FlashCardProgress fcp = i.getArgument(0);
                    fcp.setModuleUsers(moduleUsers); // ensure association
                    moduleUsers.getFlashcardProgressList().add(fcp);
                    return fcp;
                });

        // After saving, next lookup for highlighting returns the saved object
        FlashCardProgress savedFlashcardProgress = new FlashCardProgress();
        savedFlashcardProgress.setFlashcardId(flashcardId);
        savedFlashcardProgress.setModuleUsers(moduleUsers);
        savedFlashcardProgress.setStatus(1); // incorrect
        savedFlashcardProgress.setIsHighlighted(false);
        when(flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUserId.toString(), flashcardId))
                .thenReturn(Optional.of(savedFlashcardProgress));

        // Create request
        FlashcardProgressRequest progressRequest = FlashcardProgressRequest.builder()
                .flashcardId(flashcardId)
                .isCorrect(false)
                .isHighlighted(true)
                .build();

        // Call the service
        service.updateFlashcardProgress(moduleId.toString(), progressRequest, requestMock);

        // Assert results
        FlashCardProgress result = moduleUsers.getFlashcardProgressList().get(0);
        assertTrue(result.getIsHighlighted());
        assertEquals(1, result.getStatus()); // incorrect
    }

    @Test
    void refreshModuleProgress_success() {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID moduleId = UUID.randomUUID();
        UUID moduleUserId = UUID.randomUUID();
        String flashCardId1 = UUID.randomUUID().toString();
        String flashCardId2 = UUID.randomUUID().toString();

        // Mock token extraction
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Setup module
        Module module = new Module();
        module.setModuleId(moduleId);
        module.setModuleName("Test Module");
        module.setIsDeleted(false);

        // Setup ModuleUsers with flashcards
        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setId(moduleUserId.toString());
        moduleUsers.setUserId(userId);
        moduleUsers.setProgress(50.0);
        moduleUsers.setLearningStatus(LearningStatus.LEARNING);
        moduleUsers.setFlashcardProgressList(new ArrayList<>());

        FlashCardProgress fcp1 = new FlashCardProgress();
        fcp1.setFlashcardId(flashCardId1);
        fcp1.setStatus(2);
        fcp1.setIsHighlighted(true);

        FlashCardProgress fcp2 = new FlashCardProgress();
        fcp2.setFlashcardId(flashCardId2);
        fcp2.setStatus(1);
        fcp2.setIsHighlighted(true);

        moduleUsers.getFlashcardProgressList().addAll(List.of(fcp1, fcp2));

        // Mock repository calls
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.findByModuleIdAndUserId(moduleId, userId)).thenReturn(Optional.of(moduleUsers));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(flashCardProgressRepository.save(any(FlashCardProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Prepare request
        ModuleFlashCardRequest request = ModuleFlashCardRequest.builder()
                .learningStatus(LearningStatus.NEW)
                .build();

        // Call service
        ModuleProgressResponse response = service.refreshModuleProgress(moduleId.toString(), request, requestMock);

        // Assertions
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(moduleId.toString(), response.moduleId());
        assertEquals(0.0, response.progress());
        assertEquals(LearningStatus.NEW, response.learningStatus());
        assertEquals(2, response.flashcardProgresses().size());
        assertTrue(response.flashcardProgresses().stream().allMatch(fcp -> fcp.status() == 0 && !fcp.isHighlighted()));
    }

    @Test
    void shareModule_success() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "ownerUser";
        String accessToken = "dummyToken";
        UUID moduleId = UUID.randomUUID();
        String newUserEmail = "newuser@example.com";
        String newUserId = "user2";

        // Mock token and access token extraction
        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);
        when(helper.getAccessToken(requestMock)).thenReturn(accessToken);

        // Module setup
        Module module = new Module();
        module.setModuleId(moduleId);
        module.setCreatedBy(userId);
        module.setIsDeleted(false);
        module.setModuleUsers(new HashSet<>());

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(moduleUsersRepository.save(any(ModuleUsers.class))).thenAnswer(i -> i.getArgument(0));
        when(moduleRepository.save(any(Module.class))).thenAnswer(i -> i.getArgument(0));

        // Mock authClient response
        UserAccessInfo.AccessInfo accessInfo = new UserAccessInfo.AccessInfo(false, true, false, false, false);
        UserAccessInfo userAccessInfo = new UserAccessInfo(
                newUserId, "username", "First", "Last", newUserEmail, true,
                System.currentTimeMillis(), true, false, accessInfo
        );
        BaseResponse<UserAccessInfo> baseResponse = new BaseResponse<>(null,"success", userAccessInfo, null);
        ResponseEntity<BaseResponse<UserAccessInfo>> responseEntity = ResponseEntity.ok(baseResponse);
        when(authClient.getUserInfoByEmail(eq(newUserEmail), anyString())).thenReturn(responseEntity);

        // Prepare request
        ShareModuleRequest shareRequest = ShareModuleRequest.builder()
                .users(List.of(newUserEmail))
                .build();

        // Call service
        service.shareModule(moduleId.toString(), shareRequest, requestMock);

        // Verify new ModuleUsers added
        assertEquals(1, module.getModuleUsers().size());
        ModuleUsers sharedUser = module.getModuleUsers().iterator().next();
        assertEquals(newUserId, sharedUser.getUserId());
        assertEquals(0, sharedUser.getStatus()); // default status
        assertEquals(userId, sharedUser.getCreatedBy());

        // Verify repository interactions
        verify(moduleUsersRepository, times(1)).save(sharedUser);
        verify(moduleRepository, times(1)).save(module);
    }




}
