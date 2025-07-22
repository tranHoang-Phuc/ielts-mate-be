package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.exceptions.InternalServerErrorException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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



    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;


    @Override
    public PassageCreationResponse createPassage(PassageCreationRequest passageCreationRequest,
                                                 HttpServletRequest request) throws JsonProcessingException {
        String userId = getUserIdFromToken(request);
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
                .build();
        ReadingPassage saved = readingPassageRepository.save(readingPassage);
        UserProfileResponse createdUserProfileResponse = getUserProfileById(userId);
        UserProfileResponse updatedUserProfileResponse = getUserProfileById(saved.getUpdatedBy());
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

        if (request.content() != null) {
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

        if (request.instruction() != null) {
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
                .build();


        entity.setIsCurrent(false);
        entity.setUpdatedAt(LocalDateTime.now());
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

        List<QuestionGroup> questionGroups = readingPassage.getQuestionGroups();

        Map<QuestionGroup, List<Question>> questionGroupMap = new HashMap<>();
        Map<UUID, List<Choice>> choiceMap = new HashMap<>();
        Map<QuestionGroup, List<DragItem>> dragItemMap = new HashMap<>();
        for (QuestionGroup group : questionGroups) {
            List<Question> filteredQuestions = group.getQuestions().stream()
                    .filter(Objects::nonNull)
                    .filter(q -> (q.getParent() == null && Boolean.TRUE.equals(q.getIsOriginal()))
                            || Boolean.TRUE.equals(q.getIsCurrent()))
                    .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                    .collect(Collectors.toList());

            List<DragItem> dragItems = dragItemRepository.findCurrentVersionsByGroupId(group.getGroupId())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(d -> (d.getParent() == null && Boolean.TRUE.equals(d.getIsOriginal()))
                            || Boolean.TRUE.equals(d.getIsCurrent()))
                    .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                    .collect(Collectors.toList());


            for (Question q : filteredQuestions) {
                List<Choice> filteredChoices = q.getChoices().stream()
                        .filter(Objects::nonNull)
                        .filter(c -> (c.getParent() == null && Boolean.TRUE.equals(c.getIsOriginal()))
                                || Boolean.TRUE.equals(c.getIsCurrent()))
                        .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                        .collect(Collectors.toList());
                choiceMap.put(q.getQuestionId(), filteredChoices);
            }
            dragItemMap.put(group, dragItems);
            questionGroupMap.put(group, filteredQuestions);
        }

        UserProfileResponse createdByProfile;
        UserProfileResponse updatedByProfile;
        try {
            createdByProfile = getUserProfileById(readingPassage.getCreatedBy());
            updatedByProfile = getUserProfileById(passage.getUpdatedBy());
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        UserInformationResponse createdBy = UserInformationResponse.builder()
                .userId(createdByProfile.id())
                .lastName(createdByProfile.lastName())
                .firstName(createdByProfile.firstName())
                .email(createdByProfile.email())
                .build();

        UserInformationResponse updatedBy = UserInformationResponse.builder()
                .userId(updatedByProfile.id())
                .lastName(updatedByProfile.lastName())
                .firstName(updatedByProfile.firstName())
                .email(updatedByProfile.email())
                .build();

        return PassageDetailResponse.builder()
                .passageId(passage.getPassageId().toString())
                .title(passage.getTitle())
                .ieltsType(passage.getIeltsType().ordinal())
                .partNumber(passage.getPartNumber().ordinal())
                .content(passage.getContent())
                .contentWithHighlightKeywords(passage.getContentWithHighlightKeyword())
                .instruction(passage.getInstruction())
                .passageStatus(passage.getPassageStatus().ordinal())
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(readingPassage.getCreatedAt().toString())
                .updatedAt(passage.getUpdatedAt().toString())
                .questionGroups(
                        questionGroups.stream()
                                .map(g -> PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.builder()
                                        .groupId(g.getGroupId().toString())
                                        .sectionLabel(g.getSectionLabel())
                                        .sectionOrder(g.getSectionOrder())
                                        .questionType(g.getQuestionType().ordinal())
                                        .instruction(g.getInstruction())
                                        .dragItems(

                                                dragItemMap.getOrDefault(g, Collections.emptyList()).stream()
                                                        .filter(DragItem::getIsCurrent)
                                                        .map(d -> UpdatedQuestionResponse.DragItemResponse.builder()
                                                                .dragItemId(d.getParent() == null
                                                                        ? d.getDragItemId().toString()
                                                                        : d.getParent().getDragItemId().toString())
                                                                .content(d.getContent())
                                                                .build())
                                                        .toList()
                                        )
                                        .questions(
                                                questionGroupMap.getOrDefault(g, Collections.emptyList()).stream()
                                                        .filter(Question::getIsCurrent)
                                                        .map(q -> PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse.builder()
                                                                .questionId(q.getParent() == null
                                                                        ? q.getQuestionId().toString()
                                                                        : q.getParent().getQuestionId().toString())
                                                                .questionOrder(q.getQuestionOrder())
                                                                .questionType(q.getQuestionType().ordinal())
                                                                .numberOfCorrectAnswers(q.getNumberOfCorrectAnswers())
                                                                .explanation(q.getExplanation())
                                                                .point(q.getPoint())
                                                                .instructionForChoice(q.getInstructionForChoice())
                                                                .choices((q.getParent() == null && Boolean.TRUE.equals(g.getIsCurrent())) ?
                                                                         q.getChoices().stream()
                                                                                .filter(c -> c.getIsCurrent() && !c.getIsDeleted())
                                                                                .map(c -> UpdatedQuestionResponse.ChoiceResponse.builder()
                                                                                        .choiceId(c.getChoiceId().toString())
                                                                                        .label(c.getLabel())
                                                                                        .choiceOrder(c.getChoiceOrder())
                                                                                        .content(c.getContent())
                                                                                        .isCorrect(c.isCorrect())
                                                                                        .build())
                                                                                .sorted(Comparator.comparing(UpdatedQuestionResponse.ChoiceResponse::choiceOrder))
                                                                                .toList()
                                                                        : q.getParent().getChoices().stream()
                                                                        .filter(c -> c.getIsCurrent() && !c.getIsDeleted())
                                                                        .map(c -> UpdatedQuestionResponse.ChoiceResponse.builder()
                                                                                .choiceId(c.getChoiceId().toString())
                                                                                .label(c.getLabel())
                                                                                .choiceOrder(c.getChoiceOrder())
                                                                                .content(c.getContent())
                                                                                .isCorrect(c.isCorrect())
                                                                                .build())
                                                                        .sorted(Comparator.comparing(UpdatedQuestionResponse.ChoiceResponse::choiceOrder))
                                                                        .toList()
                                                                )
                                                                .blankIndex(q.getBlankIndex())
                                                                .correctAnswer(q.getCorrectAnswer())
                                                                .instructionForMatching(q.getInstructionForMatching())
                                                                .correctAnswerForMatching(q.getCorrectAnswerForMatching())
                                                                .zoneIndex(q.getZoneIndex())
                                                                .dragItemId(
                                                                        q.getQuestionType() == QuestionType.DRAG_AND_DROP ?
                                                                        q.getIsOriginal()?
                                                                                (dragItemRepository.findByQuestionId(q.getQuestionId()) != null ?dragItemRepository.findByQuestionId(q.getQuestionId()).getDragItemId().toString() : null)
                                                                                :
                                                                                (dragItemRepository.findByQuestionId(q.getParent().getQuestionId()) != null?dragItemRepository.findByQuestionId(q.getParent().getQuestionId()) .getDragItemId().toString() : null)
                                                                                        : null)
                                                                .build())
                                                        .sorted(Comparator.comparing(PassageAttemptResponse.
                                                                ReadingPassageResponse.QuestionGroupResponse.
                                                                QuestionResponse::questionOrder))
                                                        .toList()
                                        )
                                        .build())
                                .sorted(Comparator.comparing(PassageAttemptResponse.ReadingPassageResponse.
                                        QuestionGroupResponse::sectionOrder))
                                .toList()
                )
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
                                                      String createdBy) {

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
                .collect(Collectors.toMap(ReadingPassage::getPassageId, Function.identity()));

        for (ReadingPassage passage : passages) {
            ReadingPassage lastVersion = latestVersions.get(passage.getPassageId());
            if (lastVersion != null) {
                passage.setTitle(lastVersion.getTitle());
                passage.setIeltsType(lastVersion.getIeltsType());
                passage.setPartNumber(lastVersion.getPartNumber());
            }
        }

        List<PassageGetResponse> responseList = passages.stream()
                .map(this::toPassageGetResponse)
                .toList();

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
                                            .correctAnswer(question.getCorrectAnswer())
                                            .correctAnswerForMatching( question.getCorrectAnswerForMatching())
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
