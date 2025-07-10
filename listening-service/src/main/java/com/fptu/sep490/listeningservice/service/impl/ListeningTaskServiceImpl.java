package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.enumeration.IeltsType;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.model.json.QuestionVersion;
import com.fptu.sep490.listeningservice.model.specification.ListeningTaskSpecification;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.service.FileService;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ListeningTaskServiceImpl implements ListeningTaskService {
    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;

    FileService fileService;
    Helper helper;
    RedisService redisService;
    KeyCloakUserClient keyCloakUserClient;
    KeyCloakTokenClient keyCloakTokenClient;

    @Value("${topic.upload-audio}")
    @NonFinal
    String uploadAudioTopic;


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
    @Transactional
    public ListeningTaskResponse createListeningTask(ListeningTaskCreationRequest request,
                                                     HttpServletRequest httpServletRequest) throws IOException {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        MultipartFile audio = request.audioFile();
        if (audio == null || audio.isEmpty() || !audio.getContentType().startsWith("audio/")) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // 2) validate size (e.g. max 10MB)
        long maxBytes = 10 * 1024 * 1024;
        if (audio.getSize() > maxBytes) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.PAYLOAD_TOO_LARGE.value()
            );
        }



        ListeningTask listeningTask = ListeningTask.builder()
                .ieltsType(safeEnumFromOrdinal(IeltsType.values(), request.ieltsType()))
                .partNumber(safeEnumFromOrdinal(PartNumber.values(), request.partNumber()))
                .instruction(request.instruction())
                .title(request.title())
                .status(safeEnumFromOrdinal(Status.values(), request.status()))
                .transcription(request.isAutomaticTranscription() ? null : request.transcription())
                .isOriginal(true)
                .isCurrent(true)
                .parent(null)
                .createdBy(userId)
                .updatedBy(userId)
                .isDeleted(false)
                .version(1)
                .build();

        ListeningTask saved = listeningTaskRepository.save(listeningTask);

        fileService.uploadAsync("listening-tasks", audio, saved.getTaskId(), UUID.fromString(userId));

        return ListeningTaskResponse.builder()
                .taskId(saved.getTaskId())
                .audioFileId(saved.getAudioFileId())
                .ieltsType(saved.getIeltsType().ordinal())
                .partNumber(saved.getPartNumber().ordinal())
                .title(saved.getTitle())
                .instruction(saved.getInstruction())
                .transcription(saved.getTranscription())
                .build();
    }

    @Override
    @Transactional
    public ListeningTaskResponse updateTask(UUID taskId, Integer status, Integer ieltsType, Integer partNumber,
                                            String instruction, String title, MultipartFile audioFile,
                                            String transcription, HttpServletRequest httpServletRequest) throws IOException {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        ListeningTask task = listeningTaskRepository.findById(taskId).orElseThrow(
                () -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                )
        );

        task.setIsCurrent(false);

        ListeningTask newVersion = ListeningTask.builder().build();

        if (!Objects.isNull(status)) {
            newVersion.setStatus(safeEnumFromOrdinal(Status.values(), status));
        } else {
            newVersion.setStatus(task.getStatus());
        }

        if (!Objects.isNull(ieltsType)) {
            newVersion.setIeltsType(safeEnumFromOrdinal(IeltsType.values(), ieltsType));
        } else {
            newVersion.setIeltsType(task.getIeltsType());
        }

        if (!Objects.isNull(partNumber)) {
            newVersion.setPartNumber(safeEnumFromOrdinal(PartNumber.values(), partNumber));
        } else {
            newVersion.setPartNumber(task.getPartNumber());
        }

        if (!Objects.isNull(instruction)) {
            newVersion.setInstruction(instruction);
        } else {
            newVersion.setInstruction(task.getInstruction());
        }

        if (!Objects.isNull(title)) {
            newVersion.setTitle(title);
        } else {
            newVersion.setTitle(task.getTitle());
        }

        if (!Objects.isNull(transcription)) {
            newVersion.setTranscription(transcription);
        } else {
            newVersion.setTranscription(task.getTranscription());
        }



        newVersion.setIsCurrent(true);
        newVersion.setCreatedBy(userId);
        newVersion.setUpdatedBy(userId);
        newVersion.setIsDeleted(false);
        newVersion.setVersion(listeningTaskRepository.findLastestVersion(task.getTaskId()).getVersion() + 1);
        newVersion.setParent(task);
        newVersion.setIsOriginal(false);

        if(!Objects.isNull(audioFile)) {
            fileService.uploadAsync("listening-tasks", audioFile, newVersion.getTaskId(), UUID.fromString(userId));
        } else {
            newVersion.setAudioFileId(task.getAudioFileId());
        }

        listeningTaskRepository.save(newVersion);
        listeningTaskRepository.save(task);

        return ListeningTaskResponse.builder()
                .taskId(task.getTaskId())
                .ieltsType(newVersion.getIeltsType().ordinal())
                .partNumber(newVersion.getPartNumber().ordinal())
                .instruction(newVersion.getInstruction())
                .title(newVersion.getTitle())
                .audioFileId(newVersion.getAudioFileId())
                .transcription(newVersion.getTranscription())
                .build();
    }

    @Override
    @Transactional
    public void deleteTask(UUID taskId) {
        List<ListeningTask> tasks = new ArrayList<>();
        ListeningTask origin = listeningTaskRepository.findById(taskId).orElseThrow(
                () -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                )
        );
        tasks.add(origin);

        List<ListeningTask> childrenTasks = listeningTaskRepository.findAllByParentId(origin.getTaskId());
        tasks.addAll(childrenTasks);

        tasks.forEach(task -> {
            task.setIsDeleted(true);
        });

        listeningTaskRepository.saveAll(tasks);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListeningTaskGetResponse> getActivatedTask(int page, int size, List<Integer> ieltsType,
                                                           List<Integer> partNumber, String questionCategory,
                                                           String sortBy, String sortDirection, String title,
                                                           String createdBy) {
        Pageable pageable = PageRequest.of(page, size);
        var spec = ListeningTaskSpecification.byCondition(
                ieltsType, List.of(1), partNumber, questionCategory,
                sortBy, sortDirection, title, createdBy
        );
        Page<ListeningTask> pageResult = listeningTaskRepository.findAll(spec, pageable);
        List<ListeningTask> tasks = pageResult.getContent();

        List<UUID> taskIds = tasks.stream().map(ListeningTask::getTaskId).toList();

        Map<UUID, ListeningTask> lastestVersion = listeningTaskRepository.findCurrentVersionsByIds(taskIds)
                .stream().collect(Collectors.toMap(ListeningTask::getTaskId, Function.identity()));

        for(ListeningTask task : tasks) {
            ListeningTask lastVersion = lastestVersion.get(task.getTaskId());
            if (!Objects.isNull(lastVersion)) {
                task.setTitle(lastVersion.getTitle());
                task.setIeltsType(lastVersion.getIeltsType());
                task.setPartNumber(lastVersion.getPartNumber());
            }
        }
        List<ListeningTaskGetResponse> responses = tasks.stream()
                .map(this :: toListeningTaskGetResponse)
                .toList();
        return new PageImpl<>(responses, pageable, pageResult.getTotalElements());
    }

    @Override
    public Page<ListeningTaskGetResponse> getListeningTask(int page, int size, List<Integer> statuses, List<Integer> ieltsType,
                                                           List<Integer> partNumber, String questionCategory,
                                                           String sortBy, String sortDirection, String title,
                                                           String createdBy) {
        Pageable pageable = PageRequest.of(page, size);
        var spec = ListeningTaskSpecification.byCondition(
                ieltsType, statuses, partNumber, questionCategory,
                sortBy, sortDirection, title, createdBy
        );
        Page<ListeningTask> pageResult = listeningTaskRepository.findAll(spec, pageable);
        List<ListeningTask> tasks = pageResult.getContent();

        List<UUID> taskIds = tasks.stream().map(ListeningTask::getTaskId).toList();

        Map<UUID, ListeningTask> lastestVersion = listeningTaskRepository.findCurrentVersionsByIds(taskIds)
                .stream().collect(Collectors.toMap(ListeningTask::getTaskId, Function.identity()));

        for(ListeningTask task : tasks) {
            ListeningTask lastVersion = lastestVersion.get(task.getTaskId());
            if (!Objects.isNull(lastVersion)) {
                task.setTitle(lastVersion.getTitle());
                task.setIeltsType(lastVersion.getIeltsType());
                task.setPartNumber(lastVersion.getPartNumber());
            }
        }
        List<ListeningTaskGetResponse> responses = tasks.stream()
                .map(this :: toListeningTaskGetResponse)
                .toList();
        return new PageImpl<>(responses, pageable, pageResult.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ListeningTaskGetAllResponse getTaskById(UUID taskId) {
        ListeningTask originalTask = listeningTaskRepository.findById(taskId)
                .orElseThrow(
                        () -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        ListeningTask currentVersion = listeningTaskRepository.findLastestVersion(taskId);
        List<QuestionGroup> originalVersionGroupQuestion = questionGroupRepository
                .findOriginalVersionByTaskId(originalTask.getTaskId());

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

        List<ListeningTaskGetAllResponse.QuestionGroupResponse> groups = new ArrayList<>();
        currentGroupMapCurrentDragItem.forEach((key, value) -> {
            List<ListeningTaskGetAllResponse.QuestionGroupResponse.DragItemResponse> dragItemResponses =
                    value.stream()
                            .map(item -> ListeningTaskGetAllResponse.QuestionGroupResponse.DragItemResponse
                                    .builder()
                                    .dragItemId(item.getParent() == null ? item.getDragItemId() : item.getParent().getDragItemId() )
                                    .content(item.getContent())
                                    .build())
                            .toList();
            List<Question> questions = currentGroupIdQuestions.get(key.getGroupId());
            Map<UUID, List<Choice>> questionMapChoices = currentGroupIdMapQuestionIdMapCurrentChoice.get(key.getGroupId());

            List<ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse> questionListResponse = new ArrayList<>();

            questions.forEach(question -> {
                List<Choice> choices = questionMapChoices.get(question.getQuestionId());

                ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse questionResponse =
                        ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.builder()
                                .questionId(question.getParent() == null ? question.getQuestionId() : question.getParent().getQuestionId())
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
                                .dragItemId(question.getDragItem() != null ? question.getDragItem().getDragItemId() : null)
                                .choices(choices.stream()
                                        .map(c -> ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse
                                                .ChoiceResponse.builder()
                                                .choiceId(c.getParent() == null ? c.getChoiceId() : c.getParent().getChoiceId())
                                                .label(c.getLabel())
                                                .choiceOrder(c.getChoiceOrder())
                                                .content(c.getContent())
                                                .isCorrect(c.isCorrect())
                                                .build())
                                        .sorted(Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.ChoiceResponse::choiceOrder))
                                        .toList())
                                .build();
                questionListResponse.add(questionResponse);
            });
            ListeningTaskGetAllResponse.QuestionGroupResponse group = ListeningTaskGetAllResponse.QuestionGroupResponse.builder()
                    .groupId(key.getParent() == null ? key.getGroupId() : key.getParent().getGroupId())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .instruction(key.getInstruction())
                    .dragItems(dragItemResponses)
                    .questions(questionListResponse.stream()
                            .sorted(Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse::questionOrder)).toList())
                    .build();
            groups.add(group);
        });



        return ListeningTaskGetAllResponse.builder()
                .taskId(taskId)
                .title(currentVersion.getTitle())
                .instruction(currentVersion.getInstruction())
                .ieltsType(currentVersion.getIeltsType().ordinal())
                .partNumber(currentVersion.getPartNumber().ordinal())
                .audioFileId(currentVersion.getAudioFileId())
                .transcription(currentVersion.getTranscription())
                .status(currentVersion.getStatus().ordinal())
                .questionGroups(groups.stream().sorted(Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse::sectionOrder)).toList())
                .build();
    }

    private ListeningTaskGetResponse toListeningTaskGetResponse(ListeningTask listeningTask) {
        UserProfileResponse createdByProfile;
        UserProfileResponse updatedByProfile;
        try {
            createdByProfile = getUserProfileById(listeningTask.getCreatedBy());
            updatedByProfile = getUserProfileById(listeningTask.getUpdatedBy());
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

        return ListeningTaskGetResponse.builder()
                .taskId(listeningTask.getTaskId())
                .ieltsType(listeningTask.getIeltsType().ordinal())
                .partNumber(listeningTask.getPartNumber().ordinal())
                .status(listeningTask.getStatus().ordinal())
                .title(listeningTask.getTitle())
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .createdAt(listeningTask.getCreatedAt().toString())
                .updatedAt(listeningTask.getUpdatedAt().toString())
                .build();
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

    private UserProfileResponse getFromCache(String userId) throws JsonProcessingException {
        String cacheKey = Constants.RedisKey.USER_PROFILE + userId;
        UserProfileResponse cachedProfile = redisService.getValue(cacheKey, UserProfileResponse.class);
        return cachedProfile;
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
}
