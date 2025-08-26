package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.*;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.exceptions.InternalServerErrorException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.event.TopicMasterRequest;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import com.fptu.sep490.readingservice.model.enumeration.PartNumber;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.readingservice.model.json.QuestionVersion;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.repository.client.MarkupClient;
import com.fptu.sep490.readingservice.repository.specification.PassageSpecifications;
import com.fptu.sep490.readingservice.service.PassageService;
import com.fptu.sep490.readingservice.viewmodel.request.PassageCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedPassageRequest;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PassageServiceImpl implements PassageService {

    QuestionGroupRepository questionGroupRepository;
    ReadingPassageRepository readingPassageRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    DragItemRepository dragItemRepository;
    RedisService redisService;
    MarkupClient markupClient;
    KafkaTemplate<String, Object> kafkaTemplate;
    private final Helper helper;


    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;

    @Value("${kafka.topic.topic-master}")
    @NonFinal
    String topicMaster;


    @Override
    public PassageCreationResponse createPassage(PassageCreationRequest passageCreationRequest,
                                                 HttpServletRequest request) throws JsonProcessingException {
        String userId = getUserIdFromToken(request);

        if(!safeEnumFromOrdinal(Status.values(), passageCreationRequest.passageStatus()).equals(Status.DRAFT)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CAN_ONLY_CREATE_DRAFT,
                    Constants.ErrorCode.CAN_ONLY_CREATE_DRAFT,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        IeltsType ieltsType = safeEnumFromOrdinal(IeltsType.values(), passageCreationRequest.ieltsType());
        PartNumber partNumber = safeEnumFromOrdinal(PartNumber.values(), passageCreationRequest.partNumber());
        Status passageStatus = safeEnumFromOrdinal(Status.values(), passageCreationRequest.passageStatus());

        ReadingPassage readingPassage = ReadingPassage.builder()
                .title(passageCreationRequest.title())
                .ieltsType(ieltsType)
                .partNumber(partNumber)
                .passageStatus(Status.DRAFT)
                .instruction(passageCreationRequest.instruction())
                .content(passageCreationRequest.content())
                .contentWithHighlightKeyword(passageCreationRequest.contentWithHighlightKeywords())
                .passageStatus(passageStatus)
                .isCurrent(true)
                .version(1)
                .isOriginal(true)
                .isDeleted(false)
                .createdBy(userId)
                .updatedBy(userId)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();
        ReadingPassage saved = readingPassageRepository.save(readingPassage);
        UserProfileResponse createdUserProfileResponse = getUserProfileById(userId);
        UserProfileResponse updatedUserProfileResponse = getUserProfileById(saved.getUpdatedBy());
        TopicMasterRequest topicMaterRequest = TopicMasterRequest.builder()
                .type(TopicType.READING_TYPE)
                .operation(Operation.CREATE)
                .taskId(saved.getPassageId())
                .title(saved.getTitle())
                .build();
        kafkaTemplate.send(topicMaster, topicMaterRequest);
        return PassageCreationResponse.builder()
                .passageId(saved.getPassageId().toString())
                .ieltsType(saved.getIeltsType().ordinal())
                .partNumber(saved.getPartNumber().ordinal())
                .passageStatus(saved.getPassageStatus().ordinal())
                .content(saved.getContent())
                .contentWithHighlightKeyword(saved.getContentWithHighlightKeyword())
                .title(saved.getTitle())
                .createdBy(UserInformationResponse.builder()
                        .userId(createdUserProfileResponse.id())
                        .lastName(createdUserProfileResponse.lastName())
                        .firstName(createdUserProfileResponse.firstName())
                        .email(createdUserProfileResponse.email())
                        .build())
                .updatedBy(UserInformationResponse.builder()
                        .userId(updatedUserProfileResponse.id())
                        .lastName(updatedUserProfileResponse.lastName())
                        .firstName(updatedUserProfileResponse.firstName())
                        .email(updatedUserProfileResponse.email())
                        .build())
                .createdAt(saved.getCreatedAt().toString())
                .updatedAt(saved.getUpdatedAt().toString())
                .build();
    }

    @Override
    @Transactional
    public Page<PassageGetResponse> getPassages(
            int page,
            int size,
            List<Integer> ieltsType,
            List<Integer> status,
            List<Integer> partNumber,
            String questionCategory,
            String sortBy,
            String sortDirection,
            String title,
            String createdBy
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var spec = PassageSpecifications.byConditions(ieltsType, status, partNumber, questionCategory, sortBy, sortDirection, title, createdBy);
        Page<ReadingPassage> pageResult = readingPassageRepository.findAll(spec, pageable);
        List<ReadingPassage> passages = pageResult.getContent();

        List<UUID> passageIds = passages.stream()
                .map(ReadingPassage::getPassageId)
                .toList();

        Map<UUID, ReadingPassage> latestVersions = readingPassageRepository
                .findCurrentVersionsByIds(passageIds)
                .stream()
                .collect(Collectors.toMap(rp -> rp.getParent() == null ? rp.getPassageId() :
                        rp.getParent().getPassageId(), Function.identity()));

        for (ReadingPassage passage : passages) {
            ReadingPassage lastVersion = latestVersions.get(passage.getPassageId());
            if (lastVersion != null) {
                passage.setTitle(lastVersion.getTitle());
                passage.setIeltsType(lastVersion.getIeltsType());
                passage.setPartNumber(lastVersion.getPartNumber());
                passage.setPassageStatus(lastVersion.getPassageStatus());
            }
        }

        List<PassageGetResponse> responseList = passages.stream()
                .map(this::toPassageGetResponse)
                .toList();

        return new PageImpl<>(responseList, pageable, pageResult.getTotalElements());
    }

    @Override
    @Transactional
    public PassageDetailResponse updatePassage(UUID passageId, UpdatedPassageRequest request,
                                               HttpServletRequest httpServletRequest) {
        String userId = getUserIdFromToken(httpServletRequest);
        ReadingPassage entity  = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        PassageDetailResponse detailResponse = getPassageById(passageId);
        AtomicInteger totalPoint = new AtomicInteger(0);

        var questionGroups = detailResponse.questionGroups();

        questionGroups.forEach(questionGroup -> {
            var questions = questionGroup.questions();
            for (var question : questions) {
                totalPoint.addAndGet(question.point());
            }
        });

        int sum = totalPoint.get();

        if(entity.getPartNumber().equals(PartNumber.PART_1) && sum != TotalPoint.PART_1_QUESTION_READING
                &&( safeEnumFromOrdinal(Status.values(), request.passageStatus()).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), request.passageStatus()).equals(Status.PUBLISHED))) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_1_POINT_INVALID,
                    Constants.ErrorCode.PART_1_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if(entity.getPartNumber().equals(PartNumber.PART_2) && sum != TotalPoint.PART_2_QUESTION_READING
                && (safeEnumFromOrdinal(Status.values(), request.passageStatus()).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), request.passageStatus()).equals(Status.PUBLISHED))) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_2_POINT_INVALID,
                    Constants.ErrorCode.PART_2_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if(entity.getPartNumber().equals(PartNumber.PART_3) && sum != TotalPoint.PART_3_QUESTION_READING
                && (safeEnumFromOrdinal(Status.values(), request.passageStatus()).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), request.passageStatus()).equals(Status.PUBLISHED))) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_3_POINT_INVALID,
                    Constants.ErrorCode.PART_3_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        List<ReadingPassage> allVersions = readingPassageRepository.findAllVersion(entity.getPassageId());
        int currentVersion = 0;
        for (ReadingPassage version : allVersions) {
            version.setIsCurrent(false);
            if(version.getVersion() > currentVersion) {
                currentVersion = version.getVersion();
            }
        }
        readingPassageRepository.saveAll(allVersions);

        ReadingPassage currentVersionPassage = readingPassageRepository.findCurrentVersionById(passageId).get();

        if(currentVersionPassage.getPassageStatus() == Status.TEST) {
            if(request.passageStatus() != Status.TEST.ordinal()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.CANT_UPDATE_TEST_TO_ANOTHER,
                        Constants.ErrorCode.CANT_UPDATE_TEST_TO_ANOTHER,
                        HttpStatus.CONFLICT.value()
                );
            }
        }

        if (request.title()== null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if (request.ieltsType() != null) {
            int ordinal = request.ieltsType();
            if (ordinal < 0 || ordinal >= IeltsType.values().length) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
        }

        if (request.partNumber() != null) {
            int ordinal = request.partNumber();
            if (ordinal < 0 || ordinal >= PartNumber.values().length) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
        }

        if (request.content() == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if (request.contentWithHighlightKeywords() == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if (request.instruction() == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );        }

        if (request.passageStatus() != null) {
            int ordinal = request.passageStatus();
            if (ordinal < 0 || ordinal >= Status.values().length) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
        }
        entity.setUpdatedBy(userId);

        ReadingPassage updatedVersion = ReadingPassage.builder()
                .instruction(request.instruction())
                .content(request.content())
                .contentWithHighlightKeyword(request.contentWithHighlightKeywords())
                .ieltsType(request.ieltsType() == null ? entity.getIeltsType() : safeEnumFromOrdinal(IeltsType.values(), request.ieltsType()))
                .partNumber(request.partNumber() == null ? entity.getPartNumber() : safeEnumFromOrdinal(PartNumber.values(), request.partNumber()))
                .passageStatus(request.passageStatus() == null ? entity.getPassageStatus() : safeEnumFromOrdinal(Status.values(), request.passageStatus()))
                .title(request.title() == null ? entity.getTitle() : request.title())
                .createdBy(userId)
                .updatedBy(userId)
                .isCurrent(true)
                .version(currentVersion + 1)
                .isDeleted(false)
                .isOriginal(false)
                .parent(entity)
                .updatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();


        entity.setIsCurrent(false);
        entity.setUpdatedBy(userId);
        entity.setPassageStatus(request.passageStatus() == null ? entity.getPassageStatus() : safeEnumFromOrdinal(Status.values(), request.passageStatus()));
        ReadingPassage updated = readingPassageRepository.save(entity);
        ReadingPassage saved = readingPassageRepository.save(updatedVersion);
        UserProfileResponse createdProfile;
        UserProfileResponse updatedProfile;
        try {
            createdProfile = getUserProfileById(updated.getCreatedBy());
            updatedProfile = getUserProfileById(saved.getUpdatedBy());
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        UserInformationResponse createdByResp = UserInformationResponse.builder()
                .userId(createdProfile.id())
                .firstName(createdProfile.firstName())
                .lastName(createdProfile.lastName())
                .email(createdProfile.email())
                .build();

        UserInformationResponse updatedByResp = UserInformationResponse.builder()
                .userId(updatedProfile.id())
                .firstName(updatedProfile.firstName())
                .lastName(updatedProfile.lastName())
                .email(updatedProfile.email())
                .build();

        TopicMasterRequest topicMaterRequest = TopicMasterRequest.builder()
                .type(TopicType.READING_TYPE)
                .operation(Operation.UPDATE)
                .taskId(saved.getPassageId())
                .title(saved.getTitle())
                .build();
        kafkaTemplate.send(topicMaster, topicMaterRequest);

        return PassageDetailResponse.builder()
                .passageId(updated.getPassageId().toString())
                .title(saved.getTitle())
                .ieltsType(saved.getIeltsType().ordinal())
                .partNumber(saved.getPartNumber().ordinal())
                .content(saved.getContent())
                .contentWithHighlightKeywords(saved.getContentWithHighlightKeyword())
                .instruction(saved.getInstruction())
                .passageStatus(saved.getPassageStatus().ordinal())
                .createdBy(createdByResp)
                .updatedBy(updatedByResp)
                .createdAt(updated.getCreatedAt().toString())
                .updatedAt(updated.getUpdatedAt().toString())
                .build();
    }

    @Override
    @Transactional
    public PassageDetailResponse getPassageById(UUID passageId) {
        var readingPassage = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        var passage = readingPassageRepository.findCurrentVersionById(passageId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        List<QuestionGroup> originalVersionGroupQuestion = questionGroupRepository
                .findOriginalVersionByTaskId(readingPassage.getPassageId());

        Map<QuestionGroup, List<DragItem>> currentGroupMapCurrentDragItem = new HashMap<>();
        Map<UUID,List<Question>>  currentGroupIdQuestions = new HashMap<>();
        Map<UUID, Map<UUID, List<Choice>>> currentGroupIdMapQuestionIdMapCurrentChoice = new HashMap<>();
        originalVersionGroupQuestion.forEach(g -> {
            // Get All Current DragItem
            List<DragItem> currentVersionDragItem = dragItemRepository.findCurrentVersionByGroupId(g.getGroupId());
            QuestionGroup latestVersion = questionGroupRepository.findLatestVersionByOriginalId(g.getGroupId());
            currentGroupMapCurrentDragItem.put(latestVersion, currentVersionDragItem);

            // Get All Original Question
            List<UUID> originalQuestionId = questionRepository.findOriginalVersionByGroupId(g.getGroupId());
            List<Question> currentQuestion = questionRepository.findAllCurrentVersion(originalQuestionId);
            currentGroupIdQuestions.put(g.getGroupId(), currentQuestion);

            Map<UUID, List<Choice>> questionIdMapCurrentChoice = new HashMap<>();
            List<QuestionVersion> questionVersionList = new ArrayList<>();
            currentQuestion.forEach(q -> {
                if(q.getIsOriginal()) {
                    List<Choice> currentChoice = choiceRepository.findCurrentVersionByQuestionId(q.getQuestionId());
                    questionIdMapCurrentChoice.put(q.getQuestionId(), currentChoice);
                    QuestionVersion questionVersion = QuestionVersion.builder()
                            .questionId(q.getQuestionId())
                            .choiceMapping(currentChoice.stream().map(Choice::getChoiceId).toList())
                            .build();
                    questionVersionList.add(questionVersion);
                } else {
                    List<Choice> currentChoice = choiceRepository.findCurrentVersionByQuestionId(q.getParent().getQuestionId());

                    questionIdMapCurrentChoice.put(q.getParent().getQuestionId(), currentChoice);
                    QuestionVersion questionVersion = QuestionVersion.builder()
                            .questionId(q.getQuestionId())
                            .choiceMapping(currentChoice.stream().map(Choice::getChoiceId).toList())
                            .build();
                    questionVersionList.add(questionVersion);
                }
            });
            currentGroupIdMapQuestionIdMapCurrentChoice.put(g.getGroupId(), questionIdMapCurrentChoice);
        });

        List<PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse> groups = new ArrayList<>();
        currentGroupMapCurrentDragItem.forEach((key, value) -> {
            List<UpdatedQuestionResponse.DragItemResponse> dragItemResponses =
                    value.stream()
                            .map(item -> UpdatedQuestionResponse.DragItemResponse
                                    .builder()
                                    .dragItemId(item.getParent() == null ? item.getDragItemId().toString() : item.getParent().getDragItemId().toString())
                                    .content(item.getContent())
                                    .build())
                            .toList();
            List<Question> questions = currentGroupIdQuestions.get(key.getGroupId());
            Map<UUID, List<Choice>> questionMapChoices = currentGroupIdMapQuestionIdMapCurrentChoice.get(key.getGroupId());

            List<PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse> questionListResponse = new ArrayList<>();
            if(questions != null) {
                questions.forEach(question -> {
                    List<Choice> choices = questionMapChoices.get(question.getQuestionId()) == null ? questionMapChoices.get(question.getParent().getQuestionId()) : questionMapChoices.get(question.getQuestionId());

                    PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse questionResponse =
                            PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse.builder()
                                    .questionId(question.getParent() == null ? question.getQuestionId().toString() : question.getParent().getQuestionId().toString())
                                    .questionOrder(question.getQuestionOrder())
                                    .questionType(question.getQuestionType().ordinal())
                                    .point(question.getPoint())
                                    .explanation(question.getExplanation())
                                    .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                                    .instructionForChoice(question.getInstructionForChoice())
                                    .blankIndex(question.getBlankIndex())
                                    .correctAnswer(question.getCorrectAnswer())
                                    .instructionForMatching(question.getInstructionForMatching())
                                    .correctAnswerForMatching(question.getCorrectAnswerForMatching())
                                    .zoneIndex(question.getZoneIndex())
                                    .dragItemId(question.getDragItem() != null ? question.getDragItem().getDragItemId().toString() : null)
                                    .choices(choices != null ? choices.stream()
                                            .map(c -> UpdatedQuestionResponse.ChoiceResponse.builder().choiceId(c.getParent() == null ? c.getChoiceId().toString() : c.getParent().getChoiceId().toString())
                                                    .label(c.getLabel())
                                                    .choiceOrder(c.getChoiceOrder())
                                                    .content(c.getContent())
                                                    .isCorrect(c.isCorrect())
                                                    .build())
                                            .sorted(Comparator.comparing(UpdatedQuestionResponse.ChoiceResponse::choiceOrder))
                                            .toList() : null)
                                    .build();
                    questionListResponse.add(questionResponse);
                });
            } else {
                questions = new ArrayList<>();
            }
            PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse group = PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.builder()
                    .groupId(key.getParent() == null ? key.getGroupId().toString() : key.getParent().getGroupId().toString())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .questionType(key.getQuestionType().ordinal())
                    .instruction(key.getInstruction())
                    .dragItems(dragItemResponses)
                    .questions(questionListResponse.stream()
                            .sorted(Comparator.comparing(PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse::questionOrder)).toList())
                    .build();
            groups.add(group);
        });



        return PassageDetailResponse.builder()
                .passageId(passageId.toString())
                .title(passage.getTitle())
                .instruction(passage.getInstruction())
                .ieltsType(passage.getIeltsType().ordinal())
                .partNumber(passage.getPartNumber().ordinal())
                .content(passage.getContent())
                .contentWithHighlightKeywords(passage.getContentWithHighlightKeyword())
                .passageStatus(passage.getPassageStatus().ordinal())
                .questionGroups(groups.stream().sorted(Comparator.comparing(PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse::sectionOrder)).toList())
                .build();
    }




    @Override
    public void deletePassage(UUID passageId) {
        var existingPassage = readingPassageRepository.findById(passageId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        existingPassage.setIsDeleted(true);
        readingPassageRepository.save(existingPassage);
        TopicMasterRequest topicMaterRequest = TopicMasterRequest.builder()
                .type(TopicType.READING_TYPE)
                .operation(Operation.DELETE)
                .taskId(existingPassage.getPassageId())
                .title(existingPassage.getTitle())
                .build();
        kafkaTemplate.send(topicMaster, topicMaterRequest);
        log.info("Passage with ID {} has been deleted successfully", passageId);
    }

    @Override
    @Transactional
    public Page<PassageGetResponse> getActivePassages(int page,
                                                      int size,
                                                      List<Integer> ieltsType,
                                                      List<Integer> partNumber,
                                                      String questionCategory,
                                                      String sortBy,
                                                      String sortDirection,
                                                      String title,
                                                      String createdBy, HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size);
        var spec = PassageSpecifications.byConditions(
                ieltsType, List.of(1), partNumber, questionCategory,
                sortBy, sortDirection, title, createdBy
        );

        Page<ReadingPassage> pageResult = readingPassageRepository.findAll(spec, pageable);
        List<ReadingPassage> passages = pageResult.getContent();

        List<UUID> passageIds = passages.stream()
                .map(ReadingPassage::getPassageId)
                .toList();

        Map<UUID, ReadingPassage> latestVersions = readingPassageRepository
                .findCurrentVersionsByIds(passageIds)
                .stream()
                .collect(Collectors.toMap(rp -> rp.getParent() == null ? rp.getPassageId() :
                        rp.getParent().getPassageId(), Function.identity()));

        for (ReadingPassage passage : passages) {
            ReadingPassage lastVersion = latestVersions.get(passage.getPassageId());
            if (lastVersion != null) {
                passage.setTitle(lastVersion.getTitle());
                passage.setIeltsType(lastVersion.getIeltsType());
                passage.setPartNumber(lastVersion.getPartNumber());
                passage.setPassageStatus(lastVersion.getPassageStatus());
            }
        }
        List<PassageGetResponse> responseList = passages.stream()
                .map(this::toPassageGetResponse)
                .toList();
        Map<UUID, Integer> passageIdsMarkedUp;
        String accessToken = CookieUtils.getCookieValue(request, CookieConstants.ACCESS_TOKEN);
        if(accessToken != null) {
            var response = markupClient.getMarkedUpData("Bearer " + accessToken, DataMarkup.READING_TASK);
            if(response.getStatusCode() == HttpStatus.OK) {
                var body = response.getBody();
                if (body != null) {
                    passageIdsMarkedUp = body.data().markedUpIdsMapping();
                    responseList = responseList.stream()
                            .map(p -> PassageGetResponse.builder()
                                    .passageId(p.passageId())
                                    .ieltsType(p.ieltsType())
                                    .partNumber(p.partNumber())
                                    .passageStatus(p.passageStatus())
                                    .title(p.title())
                                    .createdBy(p.createdBy())
                                    .updatedBy(p.updatedBy())
                                    .createdAt(p.createdAt())
                                    .updatedAt(p.updatedAt())
                                    .isMarkedUp(passageIdsMarkedUp.get(UUID.fromString(p.passageId())) != null)
                                    .markupTypes(passageIdsMarkedUp.get(UUID.fromString(p.passageId())))
                                    .build())
                            .toList();
                    return new PageImpl<>(responseList, pageable, pageResult.getTotalElements());
                }
            }
        }


        // Call sang lấy list markup


        return new PageImpl<>(responseList, pageable, pageResult.getTotalElements());
    }

    @Override
    public CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse fromReadingPassage(String passageId) {
        ReadingPassage passage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        //Không cần find current nua vi exam da luu phien ban tai thoi diem tao cua passage r
//        ReadingPassage currentVersion = readingPassageRepository.findCurrentVersionById(passage.getPassageId())
//                .orElseThrow(() -> new AppException(
//                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
//                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
//                        HttpStatus.NOT_FOUND.value()
//                ));

//        if (currentVersion.getPassageStatus() == null || currentVersion.getPassageStatus() != Status.PUBLISHED) {
//            throw new AppException(
//                    Constants.ErrorCodeMessage.PASSAGE_NOT_ACTIVE,
//                    Constants.ErrorCode.PASSAGE_NOT_ACTIVE,
//                    HttpStatus.BAD_REQUEST.value()
//            );
//        }
//

        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByReadingPassageByPassageId(passage.getPassageId());
        if (questionGroups.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        Map<QuestionGroup, Map<Question, List<Choice>>> currentVersionChoicesByGroup = new HashMap<>();
        Map<QuestionGroup, List<DragItem>> currentVersionDragItemsByGroup = new HashMap<>();
        for (QuestionGroup group : questionGroups) {
            List<Question> currentVersionQuestions = questionRepository.findCurrentVersionByGroup(group.getGroupId());
            List<DragItem> currentVersionDragItems = dragItemRepository.findCurrentVersionsByGroupId(group.getGroupId());
            currentVersionDragItemsByGroup.put(group, currentVersionDragItems);
            Map<Question, List<Choice>> currentVersionChoicesByQuestion = new HashMap<>();
            for (Question currentVersionQuestion : currentVersionQuestions) {
                QuestionVersion questionVersion = QuestionVersion.builder()
                        .questionId(currentVersionQuestion.getQuestionId())
                        .build();
                if (currentVersionQuestion.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {

                    List<UUID> choiceVersionIds = new ArrayList<>();
                    if (currentVersionQuestion.getParent() == null) {
                        List<Choice> currentVersionChoices = choiceRepository.getVersionChoiceByQuestionId(
                                currentVersionQuestion.getQuestionId());
                        currentVersionChoices.stream()
                                .map(Choice::getChoiceId)
                                .forEach(choiceVersionIds::add);

                        if (currentVersionChoices.isEmpty()) {
                            throw new AppException(
                                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                                    Constants.ErrorCode.CHOICE_NOT_FOUND,
                                    HttpStatus.NOT_FOUND.value()
                            );
                        }
                        currentVersionChoicesByQuestion.put(currentVersionQuestion, currentVersionChoices);
                    } else {
                        List<Choice> originVersionChoices = choiceRepository.getVersionChoiceByParentQuestionId(
                                currentVersionQuestion.getParent().getQuestionId());
                        List<Choice> choices = new ArrayList<>();

                        for (Choice choice : originVersionChoices) {
                            if (!choice.getIsCurrent()) {
                                Choice current = choiceRepository.getCurrentVersionChoiceByChoiceId(choice.getChoiceId());
                                choices.add(current);

                            } else {
                                choices.add(choice);
                            }
                        }
                        choices.stream()
                                .map(Choice::getChoiceId)
                                .forEach(choiceVersionIds::add);
                        currentVersionChoicesByQuestion.put(currentVersionQuestion, choices);
                    }
                    questionVersion.setChoiceMapping(choiceVersionIds);
                }

                else {
                    List<Choice> choices = new ArrayList<>();
                    currentVersionChoicesByQuestion.put(currentVersionQuestion, choices);
                }
            }


            currentVersionChoicesByGroup.put(group, currentVersionChoicesByQuestion);
        }

        List<AttemptResponse.QuestionGroupAttemptResponse> questionGroupResponses =
                currentVersionChoicesByGroup.entrySet().stream()
                        .map(groupEntry -> {
                            QuestionGroup group = groupEntry.getKey();
                            Map<Question, List<Choice>> questionChoices = groupEntry.getValue();

                            List<AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse> questionResponses =
                                    questionChoices.entrySet().stream()
                                            .map(questionEntry -> {
                                                Question question = questionEntry.getKey();
                                                List<Choice> choices = questionEntry.getValue();

                                                List<AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse> choiceResponses =
                                                        choices.stream()
                                                                .map(choice -> new AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse(
                                                                        choice.getChoiceId(),
                                                                        choice.getLabel(),
                                                                        choice.getContent(),
                                                                        choice.getChoiceOrder()
                                                                ))
                                                                .collect(Collectors.toList());

//
                                                return AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse
                                                        .builder()
                                                        .questionId(question.getQuestionId())
                                                        .questionOrder(question.getQuestionOrder())
                                                        .questionType(question.getQuestionType().ordinal())
                                                        .instructionForChoice(question.getInstructionForChoice())
                                                        .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                                                        .instructionForMatching(question.getInstructionForMatching())
                                                        .choices(choiceResponses)
                                                        .build();
                                            })
                                            .collect(Collectors.toList());

                            return new AttemptResponse.QuestionGroupAttemptResponse(
                                    group.getGroupId(),
                                    group.getSectionOrder(),
                                    group.getSectionLabel(),
                                    group.getInstruction(),
                                    group.getSentenceWithBlanks(),
                                    questionResponses,
                                    currentVersionDragItemsByGroup.getOrDefault(group, Collections.emptyList()).stream()
                                            .filter(DragItem::getIsCurrent)
                                            .map(d -> UpdatedQuestionResponse.DragItemResponse.builder()
                                                    .dragItemId(d.getParent() == null
                                                            ? d.getDragItemId().toString()
                                                            : d.getParent().getDragItemId().toString())
                                                    .content(d.getContent())
                                                    .build())
                                            .toList()
                            );
                        })
                        .collect(Collectors.toList());

        return CreateExamAttemptResponse.ReadingExamResponse.ReadingPassageResponse.builder()
                .passageId(passage.getPassageId())
                .instruction(passage.getInstruction())
                .title(passage.getTitle())
                .content(passage.getContent())
                .partNumber(passage.getPartNumber().ordinal())
                .questionGroups(questionGroupResponses)
                .build();
    }

    @Override
    public List<ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse> fromExamAttemptHistory(ExamAttemptHistory history) {
        List<ReadingPassage> passages = readingPassageRepository.findAllByIdSortedByPartNumber(history.getPassageId());
        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByIdOrderBySectionOrder(history.getQuestionGroupIds());
        List<Question> questions = questionRepository.findAllByIdOrderByQuestionOrder(history.getQuestionIds());

        List<ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse> passageResponses = new ArrayList<>();
        for (ReadingPassage passage: passages) {
            int partNumber = passage.getPartNumber().ordinal();

            List<ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse> questionGroupsList = new ArrayList<>();
            for (QuestionGroup group : questionGroups) {

                if (group.getReadingPassage().getPartNumber().ordinal()==partNumber) {
                    List<UpdatedQuestionResponse.DragItemResponse> dragItemResponses = new ArrayList<>();
                    if (group.getQuestionType()==QuestionType.DRAG_AND_DROP) {
                        List<UUID> dragItemIds = history.getGroupMapItems().getOrDefault(group.getGroupId(), Collections.emptyList());
                        List<DragItem> dragItems = dragItemRepository.findAllById(dragItemIds);
                        for (DragItem dragItem : dragItems) {
                            UpdatedQuestionResponse.DragItemResponse dragItemResponse =
                                    UpdatedQuestionResponse.DragItemResponse.builder()
                                            .dragItemId(dragItem.getDragItemId().toString())
                                            .content(dragItem.getContent())
                                            .build();
                            dragItemResponses.add(dragItemResponse);
                        }
                    }

                    List<ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse> questionAttemptResponses = new ArrayList<>();
                    for (Question question : questions) {
                        if (question.getQuestionGroup() != null && question.getQuestionGroup().getGroupId().equals(group.getGroupId())) {

                            List<ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse> choiceAttemptResponses = new ArrayList<>();

                            if (question.getQuestionType()== QuestionType.MULTIPLE_CHOICE) {
                                for (UUID choiceId : history.getQuestionMapChoices().getOrDefault(question.getQuestionId(), Collections.emptyList())) {
                                    Choice choice = choiceRepository.findById(choiceId)
                                            .orElseThrow(() -> new AppException(
                                                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                                                    Constants.ErrorCode.CHOICE_NOT_FOUND,
                                                    HttpStatus.NOT_FOUND.value()
                                            ));
                                    ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse choiceAttemptResponse =
                                            ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse.builder()
                                                    .choiceId(choice.getChoiceId())
                                                    .label(choice.getLabel())
                                                    .content(choice.getContent())
                                                    .choiceOrder(choice.getChoiceOrder())
                                                    .isCorrect(choice.isCorrect())
                                                    .build();
                                    choiceAttemptResponses.add(choiceAttemptResponse);
                                }
                            }
                            ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse questionAttemptResponse =
                                    ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.builder()
                                            .questionId(question.getQuestionId())
                                            .questionOrder(question.getQuestionOrder())
                                            .questionType(question.getQuestionType().ordinal())
                                            .blankIndex(question.getBlankIndex())
                                            .instructionForChoice(question.getInstructionForChoice())
                                            .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                                            .instructionForMatching(question.getInstructionForMatching())
                                            .zoneIndex( question.getZoneIndex())
                                            .choices(choiceAttemptResponses)
                                            .correctAnswer(question.getQuestionType() != QuestionType.DRAG_AND_DROP ? question.getCorrectAnswer(): question.getDragItem().getDragItemId().toString())
                                            .correctAnswerForMatching(question.getCorrectAnswerForMatching())
                                            .explanation( question.getExplanation())
                                            .point( question.getPoint())
                                            .build();

                            questionAttemptResponses.add(questionAttemptResponse);
                        }
                    }

                    ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse currentGroup = ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.QuestionGroupAttemptResponse.builder()
                            .questionGroupId(group.getGroupId())
                            .sectionOrder(group.getSectionOrder())
                            .sectionLabel(group.getSectionLabel())
                            .instruction(group.getInstruction())
                            .sentenceWithBlanks(group.getSentenceWithBlanks())
                            .questions(questionAttemptResponses)
                            .dragItems(dragItemResponses)
                            .build();

                    questionGroupsList.add(currentGroup);
                }
            }
            ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse readingPassageResponse = ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse.builder()
                    .passageId(passage.getPassageId())
                    .title(passage.getTitle())
                    .instruction(passage.getInstruction())
                    .content(passage.getContent())
                    .contentWithHighlightKeyword(passage.getContentWithHighlightKeyword())
                    .partNumber(passage.getPartNumber().ordinal())
                    .questionGroups(questionGroupsList)
                    .build();

            passageResponses.add(readingPassageResponse);
        }

        return passageResponses;
    }

    @Override
    public List<TaskTitle> getTaskTitle(List<UUID> ids) {
        List<ReadingPassage> tasks = readingPassageRepository.findAllById(ids);
        return tasks.stream().map(
                t-> TaskTitle.builder()
                        .taskId(t.getPassageId())
                        .title(t.getTitle())
                        .build()
        ).toList();
    }


    private PassageGetResponse toPassageGetResponse(ReadingPassage readingPassage) {

        UserProfileResponse createdByProfile;
        UserProfileResponse updatedByProfile;
        try {
            createdByProfile = getUserProfileById(readingPassage.getCreatedBy());
            updatedByProfile = getUserProfileById(readingPassage.getUpdatedBy());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to fetch user profile", e);
        }

        var createdBy = UserInformationResponse.builder()
                .userId(createdByProfile.id())
                .lastName(createdByProfile.lastName())
                .firstName(createdByProfile.firstName())
                .email(createdByProfile.email())
                .build();

        var updatedBy = UserInformationResponse.builder()
                .userId(updatedByProfile.id())
                .lastName(updatedByProfile.lastName())
                .firstName(updatedByProfile.firstName())
                .email(updatedByProfile.email())
                .build();

        return PassageGetResponse.builder()
                .passageId(readingPassage.getPassageId().toString())
                .ieltsType(readingPassage.getIeltsType().ordinal())
                .partNumber(readingPassage.getPartNumber().ordinal())
                .passageStatus(readingPassage.getPassageStatus().ordinal())
                .title(readingPassage.getTitle())
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(readingPassage.getCreatedAt().toString())
                .updatedAt(readingPassage.getUpdatedAt().toString())
                .build();
    }



    private String getUserIdFromToken(HttpServletRequest request) {
        String token = CookieUtils.getCookieValue(request, "Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new AppException(Constants.ErrorCodeMessage.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value());
        }
    }

    private <T extends Enum<T>> T safeEnumFromOrdinal(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        return values[ordinal];
    }

    private UserProfileResponse getUserProfileById(String userId) throws JsonProcessingException {
        String clientToken = getCachedClientToken();
        UserProfileResponse cachedProfile = getFromCache(userId);
        if (cachedProfile != null) {
            return cachedProfile;
        }
        UserProfileResponse profileResponse = keyCloakUserClient.getUserById(realm, "Bearer " + clientToken, userId);

        if (profileResponse == null) {
            throw new AppException(Constants.ErrorCodeMessage.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value());
        }
        redisService.saveValue(Constants.RedisKey.USER_PROFILE + userId, profileResponse, Duration.ofDays(1));
        return profileResponse;
    }
    private UserProfileResponse getFromCache(String userId) throws JsonProcessingException {
        String cacheKey = Constants.RedisKey.USER_PROFILE + userId;
        UserProfileResponse cachedProfile = redisService.getValue(cacheKey, UserProfileResponse.class);
        return cachedProfile;
    }

    private String getCachedClientToken() throws JsonProcessingException {
        final String cacheKey = Constants.RedisKey.KEY_CLOAK_CLIENT_TOKEN;

        String cachedToken = redisService.getValue(cacheKey, String.class);
        if (cachedToken != null) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "openid");

        KeyCloakTokenResponse tokenResponse = keyCloakTokenClient.requestToken(form, realm);
        String newToken = tokenResponse.accessToken();
        var expiresIn = tokenResponse.expiresIn();
        redisService.saveValue(cacheKey, newToken, Duration.ofSeconds(expiresIn));
        return newToken;
    }
}
