package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.repository.client.MarkupClient;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.PassageCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserProfileResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedPassageRequest;
import com.fptu.sep490.readingservice.viewmodel.response.PassageDetailResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.viewmodel.response.MarkedUpResponse;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.Cookie;
import com.fptu.sep490.readingservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.readingservice.viewmodel.response.ExamAttemptGetDetail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PassageServiceImplTest {

    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock ReadingPassageRepository readingPassageRepository;
    @Mock QuestionRepository questionRepository;
    @Mock ChoiceRepository choiceRepository;
    @Mock KeyCloakTokenClient keyCloakTokenClient;
    @Mock KeyCloakUserClient keyCloakUserClient;
    @Mock DragItemRepository dragItemRepository;
    @Mock com.fptu.sep490.commonlibrary.redis.RedisService redisService;
    @Mock MarkupClient markupClient;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock Helper helper;

    PassageServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PassageServiceImpl(
                questionGroupRepository,
                readingPassageRepository,
                questionRepository,
                choiceRepository,
                keyCloakTokenClient,
                keyCloakUserClient,
                dragItemRepository,
                redisService,
                markupClient,
                kafkaTemplate,
                helper
        );
    }

    @Test
    void createPassage_success() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String userId = UUID.randomUUID().toString();
        when(helper.getUserIdFromToken(req)).thenReturn(userId);

        PassageCreationRequest request = new PassageCreationRequest(
                0, // ieltsType
                0, // partNumber
                "ins",
                "Title",
                "Content",
                "Highlight",
                1 // passageStatus
        );

        ReadingPassage saved = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .ieltsType(com.fptu.sep490.readingservice.model.enumeration.IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED)
                .content("Content")
                .contentWithHighlightKeyword("Highlight")
                .title("Title")
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(readingPassageRepository.save(any(ReadingPassage.class))).thenReturn(saved);

        // avoid calling Keycloak token endpoint by returning cached token
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");
        // return cached user profile to bypass external call
        UserProfileResponse profile = new UserProfileResponse("id","username","email","A","B");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        // fallback if code ever requests token
        KeyCloakTokenResponse tokenResp = mock(KeyCloakTokenResponse.class);
        when(tokenResp.accessToken()).thenReturn("token");
        when(tokenResp.expiresIn()).thenReturn(3600);
        when(keyCloakTokenClient.requestToken(any(), anyString())).thenReturn(tokenResp);

        // fallback if bypass is not used (shouldn't be called, but safe)
        when(keyCloakUserClient.getUserById(any(), any(), eq(userId))).thenReturn(profile);

        PassageCreationResponse resp = service.createPassage(request, req);
        assertEquals(saved.getPassageId().toString(), resp.passageId());
        assertEquals(0, resp.ieltsType());
        assertEquals(0, resp.partNumber());
        assertEquals(saved.getPassageStatus().ordinal(), resp.passageStatus());
        assertEquals("Content", resp.content());
        assertEquals("Highlight", resp.contentWithHighlightKeyword());
        assertEquals("Title", resp.title());
        assertNotNull(resp.createdBy());
        assertNotNull(resp.updatedBy());
        verify(kafkaTemplate).send(any(), any());
    }

    @Test
    void createPassage_invalidEnums_throw() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());

        // invalid ieltsType
        PassageCreationRequest r1 = new PassageCreationRequest(99, 0, "i","t","c","h", 1);
        assertThrows(AppException.class, () -> service.createPassage(r1, req));

        // invalid part number
        PassageCreationRequest r2 = new PassageCreationRequest(0, 99, "i","t","c","h", 1);
        assertThrows(AppException.class, () -> service.createPassage(r2, req));

        // invalid status
        PassageCreationRequest r3 = new PassageCreationRequest(0, 0, "i","t","c","h", 99);
        assertThrows(AppException.class, () -> service.createPassage(r3, req));
    }

    @Test
    void getPassages_updatesWithLatestVersion() throws Exception {
        // Prepare original passage
        UUID pid = UUID.randomUUID();
        ReadingPassage original = ReadingPassage.builder()
                .passageId(pid)
                .ieltsType(IeltsType.GENERAL_TRAINING)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_2)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.DRAFT)
                .title("Old Title")
                .content("Content")
                .contentWithHighlightKeyword("Highlight")
                .createdBy("creator")
                .updatedBy("updater")
                .build();
        original.setCreatedAt(LocalDateTime.now().minusDays(1));
        original.setUpdatedAt(LocalDateTime.now().minusHours(1));

        // Latest version referencing parent
        ReadingPassage latest = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .ieltsType(com.fptu.sep490.readingservice.model.enumeration.IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_3)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED)
                .title("New Title")
                .build();
        latest.setParent(original);

        // Page with original
        PageImpl<ReadingPassage> page = new PageImpl<>(java.util.List.of(original), PageRequest.of(0, 10), 1);
        when(readingPassageRepository.findAll(Mockito.<Specification<ReadingPassage>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(readingPassageRepository.findCurrentVersionsByIds(java.util.List.of(pid)))
                .thenReturn(java.util.List.of(latest));

        // Profile cache for mapping in toPassageGetResponse
        UserProfileResponse profile = new UserProfileResponse("id","username","email","A","B");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        // cached client token to bypass Keycloak token call
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");

        var respPage = service.getPassages(0, 10, java.util.List.of(), java.util.List.of(), java.util.List.of(), null, null, null, null, null);
        assertEquals(1, respPage.getContent().size());
        var resp = respPage.getContent().get(0);
        // Updated fields should come from latest
        assertEquals("New Title", resp.title());
        assertEquals(latest.getIeltsType().ordinal(), resp.ieltsType());
        assertEquals(latest.getPartNumber().ordinal(), resp.partNumber());
        assertEquals(latest.getPassageStatus().ordinal(), resp.passageStatus());
    }

    @Test
    void getPassages_noLatestVersion_keepsOriginalFields() throws Exception {
        UUID pid = UUID.randomUUID();
        ReadingPassage original = ReadingPassage.builder()
                .passageId(pid)
                .ieltsType(IeltsType.GENERAL_TRAINING)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.DRAFT)
                .title("Keep Title")
                .content("Content")
                .contentWithHighlightKeyword("Highlight")
                .createdBy("creator")
                .updatedBy("updater")
                .build();
        original.setCreatedAt(LocalDateTime.now().minusDays(2));
        original.setUpdatedAt(LocalDateTime.now().minusDays(1));

        PageImpl<ReadingPassage> page = new PageImpl<>(java.util.List.of(original), PageRequest.of(0, 5), 1);
        when(readingPassageRepository.findAll(Mockito.<Specification<ReadingPassage>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        // No latest versions
        when(readingPassageRepository.findCurrentVersionsByIds(java.util.List.of(pid)))
                .thenReturn(java.util.List.of());

        UserProfileResponse profile = new UserProfileResponse("id","username","email","A","B");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");

        var respPage = service.getPassages(0, 5, java.util.List.of(), java.util.List.of(), java.util.List.of(), null, null, null, null, null);
        var resp = respPage.getContent().get(0);
        // Should keep original values
        assertEquals("Keep Title", resp.title());
        assertEquals(original.getIeltsType().ordinal(), resp.ieltsType());
        assertEquals(original.getPartNumber().ordinal(), resp.partNumber());
        assertEquals(original.getPassageStatus().ordinal(), resp.passageStatus());
    }

    @Test
    void updatePassage_success() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String userId = UUID.randomUUID().toString();
        when(helper.getUserIdFromToken(req)).thenReturn(userId);

        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder()
                .passageId(pid)
                .ieltsType(IeltsType.GENERAL_TRAINING)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.DRAFT)
                .title("Old")
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        entity.setCreatedAt(LocalDateTime.now().minusDays(3));
        entity.setUpdatedAt(LocalDateTime.now().minusDays(2));
		entity.setVersion(1);

        ReadingPassage current = ReadingPassage.builder()
                .passageId(pid)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.DRAFT)
                .build();

        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(current));
        when(readingPassageRepository.save(any(ReadingPassage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(readingPassageRepository.saveAll(anyList())).thenReturn(java.util.List.of(entity));

        // Cache user profiles
        UserProfileResponse profile = new UserProfileResponse("id","username","email","A","B");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");

        UpdatedPassageRequest up = new UpdatedPassageRequest(
                "New Title", // title required
                null, // ieltsType keep
                null, // part keep
                null, // content must be null per validation
                "Highlight", // contentWithHighlightKeywords required
                null, // instruction must be null per validation
                null // passageStatus keep
        );

        PassageDetailResponse resp = service.updatePassage(pid, up, req);
        assertEquals(pid.toString(), resp.passageId());
        assertEquals("New Title", resp.title());
        assertNotNull(resp.updatedAt());
        verify(kafkaTemplate).send(any(), any());
    }

    @Test
    void updatePassage_testStatusChangeConflict_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");

        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        ReadingPassage current = ReadingPassage.builder().passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.TEST).build();
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(current));

        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, null, null, "H", null, com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED.ordinal());
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_titleNull_invalid() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest(null, null, null, null, null, "H", null);
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_invalidIeltsOrdinal_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest("title", 999, null, null, "H", null, null);
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_invalidPartOrdinal_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, 999, null, "H", null, null);
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_contentNotNull_invalid() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, null, "content-not-allowed", "H", null, null);
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_contentWithHighlightNull_invalid() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, null, null, null, null, null);
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_instructionNotNull_invalid() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, null, null, "H", Status.DRAFT.name(), Status.DRAFT.ordinal());
        // Note: instruction must be null -> expecting AppException regardless of status
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_invalidStatusOrdinal_throws() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, null, null, "H", null, 999);
        assertThrows(AppException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void updatePassage_profileJsonError_internalServer() throws Exception {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u");
        UUID pid = UUID.randomUUID();
        ReadingPassage entity = ReadingPassage.builder().passageId(pid).createdBy("u").updatedBy("u").build();
        entity.setVersion(1);
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.findAllVersion(pid)).thenReturn(java.util.List.of(entity));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(entity));
        when(readingPassageRepository.save(any(ReadingPassage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(readingPassageRepository.saveAll(anyList())).thenReturn(java.util.List.of(entity));

        // Force JsonProcessingException inside getUserProfileById via redis
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenThrow(new JsonProcessingException("boom"){});

        UpdatedPassageRequest up = new UpdatedPassageRequest("T", null, null, null, "H", null, null);
        assertThrows(com.fptu.sep490.commonlibrary.exceptions.InternalServerErrorException.class, () -> service.updatePassage(pid, up, req));
    }

    @Test
    void getPassageById_success_buildsDetail() throws Exception {
        UUID pid = UUID.randomUUID();

        ReadingPassage original = ReadingPassage.builder()
                .passageId(pid)
                .title("Title-Orig")
                .ieltsType(IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.DRAFT)
                .createdBy("creator")
                .updatedBy("creator")
                .build();
        original.setCreatedAt(LocalDateTime.now().minusDays(1));
        original.setUpdatedAt(LocalDateTime.now().minusDays(1));

        ReadingPassage current = ReadingPassage.builder()
                .passageId(pid)
                .title("Title-Current")
                .ieltsType(IeltsType.GENERAL_TRAINING)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_2)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED)
                .updatedBy("updater")
                .build();
        current.setCreatedAt(LocalDateTime.now().minusHours(2));
        current.setUpdatedAt(LocalDateTime.now().minusHours(1));

        QuestionGroup group = QuestionGroup.builder()
                .groupId(UUID.randomUUID())
                .readingPassage(original)
                .sectionOrder(1)
                .sectionLabel("Sec")
                .instruction("Instr")
                .questionType(QuestionType.DRAG_AND_DROP)
                .isCurrent(true)
                .build();

        Question qMc = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionOrder(1)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .numberOfCorrectAnswers(1)
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .build();
        Choice c1 = Choice.builder()
                .choiceId(UUID.randomUUID())
                .label("A")
                .content("A1")
                .choiceOrder(1)
                .isCurrent(true)
                .isDeleted(false)
                .isCorrect(true)
                .build();
        qMc.setChoices(new java.util.ArrayList<>(java.util.List.of(c1)));

        Question qDrag = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionOrder(2)
                .questionType(QuestionType.DRAG_AND_DROP)
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .build();
        qDrag.setChoices(new java.util.ArrayList<>());

        group.setQuestions(new java.util.ArrayList<>(java.util.List.of(qMc, qDrag)));
        original.setQuestionGroups(new java.util.ArrayList<>(java.util.List.of(group)));

        DragItem di = DragItem.builder()
                .dragItemId(UUID.randomUUID())
                .content("DI")
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .build();

        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(original));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(current));
        when(dragItemRepository.findCurrentVersionsByGroupId(group.getGroupId()))
                .thenReturn(java.util.List.of(di));
        when(dragItemRepository.findByQuestionId(qDrag.getQuestionId())).thenReturn(di);

        UserProfileResponse creator = new UserProfileResponse("creator","creatorU","c@e","C","R");
        UserProfileResponse updater = new UserProfileResponse("updater","updaterU","u@e","U","P");
        when(redisService.getValue(contains(Constants.RedisKey.USER_PROFILE), eq(UserProfileResponse.class)))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0);
                    return key.contains("creator") ? creator : updater;
                });
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");

        PassageDetailResponse resp = service.getPassageById(pid);

        assertEquals(pid.toString(), resp.passageId());
        assertEquals("Title-Current", resp.title());
        assertEquals(current.getIeltsType().ordinal(), resp.ieltsType());
        assertEquals(current.getPartNumber().ordinal(), resp.partNumber());
        assertEquals(current.getPassageStatus().ordinal(), resp.passageStatus());
        assertNotNull(resp.createdAt());
        assertNotNull(resp.updatedAt());
        assertEquals(1, resp.questionGroups().size());
        var gResp = resp.questionGroups().get(0);
        assertEquals(1, gResp.sectionOrder());
        assertEquals(2, gResp.questions().size());
        var mcResp = gResp.questions().stream().filter(q -> q.questionType() == QuestionType.MULTIPLE_CHOICE.ordinal()).findFirst().orElseThrow();
        assertEquals(1, mcResp.choices().size());
        var dragResp = gResp.questions().stream().filter(q -> q.questionType() == QuestionType.DRAG_AND_DROP.ordinal()).findFirst().orElseThrow();
        assertNotNull(dragResp.dragItemId());
    }

    @Test
    void getPassageById_notFound_original() {
        UUID pid = UUID.randomUUID();
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.empty());
        assertThrows(AppException.class, () -> service.getPassageById(pid));
    }

    @Test
    void getPassageById_notFound_currentVersion() {
        UUID pid = UUID.randomUUID();
        ReadingPassage original = ReadingPassage.builder().passageId(pid).build();
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(original));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.empty());
        assertThrows(AppException.class, () -> service.getPassageById(pid));
    }

    @Test
    void getPassageById_profileJsonError_internalServer() throws Exception {
        UUID pid = UUID.randomUUID();
        ReadingPassage original = ReadingPassage.builder()
                .passageId(pid)
                .title("T")
                .ieltsType(IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.DRAFT)
                .createdBy("creator")
                .updatedBy("updater")
                .build();
        original.setCreatedAt(LocalDateTime.now().minusDays(2));
        original.setUpdatedAt(LocalDateTime.now().minusDays(2));

        ReadingPassage current = ReadingPassage.builder()
                .passageId(pid)
                .title("T2")
                .ieltsType(IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED)
                .updatedBy("updater")
                .build();
        current.setCreatedAt(LocalDateTime.now().minusDays(1));
        current.setUpdatedAt(LocalDateTime.now().minusHours(1));

        QuestionGroup group = QuestionGroup.builder()
                .groupId(UUID.randomUUID())
                .readingPassage(original)
                .sectionOrder(1)
                .isCurrent(true)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .build();
        Question q = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .build();
        q.setChoices(new java.util.ArrayList<>());
        group.setQuestions(new java.util.ArrayList<>(java.util.List.of(q)));
        original.setQuestionGroups(new java.util.ArrayList<>(java.util.List.of(group)));

        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(original));
        when(readingPassageRepository.findCurrentVersionById(pid)).thenReturn(java.util.Optional.of(current));
        when(dragItemRepository.findCurrentVersionsByGroupId(group.getGroupId()))
                .thenReturn(java.util.List.of());

        // Ensure token is retrieved from cache to avoid calling Keycloak token endpoint
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");
        // Even if code tries to fetch a token, return a non-null response to avoid NPE
        KeyCloakTokenResponse tokenResp = mock(KeyCloakTokenResponse.class);
        when(tokenResp.accessToken()).thenReturn("tok");
        when(tokenResp.expiresIn()).thenReturn(3600);
        when(keyCloakTokenClient.requestToken(any(), anyString())).thenReturn(tokenResp);
        // Force JSON error when fetching user profile from cache path
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class)))
                .thenThrow(new JsonProcessingException("boom"){});

        assertThrows(com.fptu.sep490.commonlibrary.exceptions.InternalServerErrorException.class, () -> service.getPassageById(pid));
    }

    @Test
    void deletePassage_success_marksDeletedAndPublishes() {
        UUID pid = UUID.randomUUID();
        ReadingPassage existing = ReadingPassage.builder()
                .passageId(pid)
                .title("DelTitle")
                .build();

        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(existing));
        when(readingPassageRepository.save(any(ReadingPassage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deletePassage(pid);

        assertTrue(existing.getIsDeleted());
        verify(readingPassageRepository).save(existing);
        verify(kafkaTemplate).send(any(), any());
    }

    @Test
    void deletePassage_notFound_throws() {
        UUID pid = UUID.randomUUID();
        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.empty());
        assertThrows(AppException.class, () -> service.deletePassage(pid));
        verify(readingPassageRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void getActivePassages_marksUpDataWhenCookiePresent_andSuccess() throws Exception {
        // Setup a passage
        UUID pid = UUID.randomUUID();
        ReadingPassage rp = ReadingPassage.builder()
                .passageId(pid)
                .ieltsType(IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED)
                .title("T")
                .createdBy("c")
                .updatedBy("u")
                .build();
        rp.setCreatedAt(LocalDateTime.now().minusDays(1));
        rp.setUpdatedAt(LocalDateTime.now());

        PageImpl<ReadingPassage> page = new PageImpl<>(java.util.List.of(rp));
        when(readingPassageRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(readingPassageRepository.findCurrentVersionsByIds(java.util.List.of(pid)))
                .thenReturn(java.util.List.of(rp));

        // Cookie present
        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("Authorization", "at");
        when(req.getCookies()).thenReturn(new Cookie[]{cookie});

        // Profiles for toPassageGetResponse
        UserProfileResponse profile = new UserProfileResponse("id","u","e","F","L");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");

        // Markup client OK with body
        java.util.Map<java.util.UUID, Integer> map = new java.util.HashMap<>();
        map.put(pid, 7);
        MarkedUpResponse marked = new MarkedUpResponse(map);
        BaseResponse<MarkedUpResponse> body = BaseResponse.<MarkedUpResponse>builder().data(marked).build();
        when(markupClient.getMarkedUpData(anyString(), anyString())).thenReturn(ResponseEntity.ok(body));

        var result = service.getActivePassages(0, 10, java.util.List.of(), java.util.List.of(), null, null, null, null, null, req);
        assertEquals(1, result.getContent().size());
        var p = result.getContent().get(0);
        assertEquals(pid.toString(), p.passageId());
        assertEquals(true, p.isMarkedUp());
        assertEquals(7, p.markupTypes());
    }

    @Test
    void getActivePassages_noCookie_orNoBody_returnsBaseList() throws Exception {
        // Setup a passage
        UUID pid = UUID.randomUUID();
        ReadingPassage rp = ReadingPassage.builder()
                .passageId(pid)
                .ieltsType(IeltsType.ACADEMIC)
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .passageStatus(com.fptu.sep490.readingservice.model.enumeration.Status.PUBLISHED)
                .title("T")
                .createdBy("c")
                .updatedBy("u")
                .build();
        rp.setCreatedAt(LocalDateTime.now().minusDays(1));
        rp.setUpdatedAt(LocalDateTime.now());

        PageImpl<ReadingPassage> page = new PageImpl<>(java.util.List.of(rp));
        when(readingPassageRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(readingPassageRepository.findCurrentVersionsByIds(java.util.List.of(pid)))
                .thenReturn(java.util.List.of(rp));

        // Profiles for toPassageGetResponse
        UserProfileResponse profile = new UserProfileResponse("id","u","e","F","L");
        when(redisService.getValue(anyString(), eq(UserProfileResponse.class))).thenReturn(profile);
        when(redisService.getValue(anyString(), eq(String.class))).thenReturn("cached-token");

        // Case 1: No cookie
        HttpServletRequest reqNoCookie = mock(HttpServletRequest.class);
        var res1 = service.getActivePassages(0, 10, java.util.List.of(), java.util.List.of(), null, null, null, null, null, reqNoCookie);
        assertEquals(1, res1.getContent().size());
        assertNull(res1.getContent().get(0).isMarkedUp());

        // Case 2: Cookie present but null body
        HttpServletRequest reqCookie = mock(HttpServletRequest.class);
        Cookie c = new Cookie("Authorization", "at");
        when(reqCookie.getCookies()).thenReturn(new Cookie[]{c});
        when(markupClient.getMarkedUpData(anyString(), anyString())).thenReturn(ResponseEntity.ok(null));

        var res2 = service.getActivePassages(0, 10, java.util.List.of(), java.util.List.of(), null, null, null, null, null, reqCookie);
        assertEquals(1, res2.getContent().size());
        assertNull(res2.getContent().get(0).isMarkedUp());
    }

    @Test
    void fromReadingPassage_success_buildsAttemptStructure() {
        UUID pid = UUID.randomUUID();
        ReadingPassage passage = ReadingPassage.builder()
                .passageId(pid)
                .title("PTitle")
                .content("PContent")
                .instruction("PInstr")
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_3)
                .build();

        // Group
        QuestionGroup group = QuestionGroup.builder()
                .groupId(UUID.randomUUID())
                .readingPassage(passage)
                .sectionOrder(2)
                .sectionLabel("Sec2")
                .instruction("GInstr")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .build();

        // Question with parent == null (original)
        Question q = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(group)
                .questionOrder(5)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .isOriginal(true)
                .isCurrent(true)
                .build();
        Choice ch = Choice.builder()
                .choiceId(UUID.randomUUID())
                .label("A")
                .content("C")
                .choiceOrder(1)
                .isCurrent(true)
                .build();

        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(passage));
        when(questionGroupRepository.findAllByReadingPassageByPassageId(pid)).thenReturn(java.util.List.of(group));
        when(questionRepository.findCurrentVersionByGroup(group.getGroupId())).thenReturn(java.util.List.of(q));
        when(choiceRepository.getVersionChoiceByQuestionId(q.getQuestionId())).thenReturn(java.util.List.of(ch));

        var resp = service.fromReadingPassage(pid.toString());
        assertEquals(pid, resp.passageId());
        assertEquals("PTitle", resp.title());
        assertEquals("PContent", resp.content());
        assertEquals(passage.getPartNumber().ordinal(), resp.partNumber());
        assertEquals(1, resp.questionGroups().size());
        var g = resp.questionGroups().get(0);
        assertEquals(2, g.sectionOrder());
        assertEquals(1, g.questions().size());
        var qResp = g.questions().get(0);
        assertEquals(q.getQuestionId(), qResp.questionId());
        assertEquals(1, qResp.choices().size());
        assertEquals(ch.getChoiceId(), qResp.choices().get(0).choiceId());
    }

    @Test
    void fromReadingPassage_groupNoChoices_throwsChoiceNotFound() {
        UUID pid = UUID.randomUUID();
        ReadingPassage passage = ReadingPassage.builder().passageId(pid).build();
        QuestionGroup group = QuestionGroup.builder().groupId(UUID.randomUUID()).readingPassage(passage).questionType(QuestionType.MULTIPLE_CHOICE).build();
        Question q = Question.builder().questionId(UUID.randomUUID()).questionGroup(group).questionType(QuestionType.MULTIPLE_CHOICE).build();

        when(readingPassageRepository.findById(pid)).thenReturn(java.util.Optional.of(passage));
        when(questionGroupRepository.findAllByReadingPassageByPassageId(pid)).thenReturn(java.util.List.of(group));
        when(questionRepository.findCurrentVersionByGroup(group.getGroupId())).thenReturn(java.util.List.of(q));
        when(choiceRepository.getVersionChoiceByQuestionId(q.getQuestionId())).thenReturn(java.util.List.of());

        assertThrows(AppException.class, () -> service.fromReadingPassage(pid.toString()));
    }

    @Test
    void fromExamAttemptHistory_success_mapsAll() {
        // Build passages list
        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .title("P1")
                .instruction("I1")
                .content("C1")
                .partNumber(com.fptu.sep490.readingservice.model.enumeration.PartNumber.PART_1)
                .build();

        // Groups ordered by section order
        QuestionGroup g1 = QuestionGroup.builder()
                .groupId(UUID.randomUUID())
                .readingPassage(p1)
                .sectionOrder(1)
                .sectionLabel("G1")
                .instruction("GI1")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .build();

        // MC question
        Question qm = Question.builder()
                .questionId(UUID.randomUUID())
                .questionGroup(g1)
                .questionOrder(1)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .build();
        Choice cm = Choice.builder()
                .choiceId(UUID.randomUUID())
                .label("A")
                .content("CA")
                .choiceOrder(1)
                .isCorrect(true)
                .build();

        // Exam history
        ExamAttemptHistory history = ExamAttemptHistory.builder()
                .passageId(java.util.List.of(p1.getPassageId()))
                .questionGroupIds(java.util.List.of(g1.getGroupId()))
                .questionIds(java.util.List.of(qm.getQuestionId()))
                .questionMapChoices(java.util.Map.of(qm.getQuestionId(), java.util.List.of(cm.getChoiceId())))
                .groupMapItems(java.util.Map.of())
                .build();

        when(readingPassageRepository.findAllByIdSortedByPartNumber(history.getPassageId()))
                .thenReturn(java.util.List.of(p1));
        when(questionGroupRepository.findAllByIdOrderBySectionOrder(history.getQuestionGroupIds()))
                .thenReturn(java.util.List.of(g1));
        when(questionRepository.findAllByIdOrderByQuestionOrder(history.getQuestionIds()))
                .thenReturn(java.util.List.of(qm));
        when(choiceRepository.findById(cm.getChoiceId())).thenReturn(java.util.Optional.of(cm));

        var list = service.fromExamAttemptHistory(history);
        assertEquals(1, list.size());
        ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse resp = list.get(0);
        assertEquals(p1.getPassageId(), resp.passageId());
        assertEquals(p1.getPartNumber().ordinal(), resp.partNumber());
        assertEquals(1, resp.questionGroups().size());
        var groupResp = resp.questionGroups().get(0);
        assertEquals(g1.getGroupId(), groupResp.questionGroupId());
        assertEquals(1, groupResp.questions().size());
        var qResp = groupResp.questions().get(0);
        assertEquals(qm.getQuestionId(), qResp.questionId());
        assertEquals(1, qResp.choices().size());
        assertEquals(cm.getChoiceId(), qResp.choices().get(0).choiceId());
    }

    @Test
    void getTaskTitle_returnsMappedList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ReadingPassage p1 = ReadingPassage.builder().passageId(id1).title("T1").build();
        ReadingPassage p2 = ReadingPassage.builder().passageId(id2).title("T2").build();

        when(readingPassageRepository.findAllById(java.util.List.of(id1, id2)))
                .thenReturn(java.util.List.of(p1, p2));

        var result = service.getTaskTitle(java.util.List.of(id1, id2));
        assertEquals(2, result.size());
        assertEquals(id1, result.get(0).taskId());
        assertEquals("T1", result.get(0).title());
        assertEquals(id2, result.get(1).taskId());
        assertEquals("T2", result.get(1).title());
    }
}

