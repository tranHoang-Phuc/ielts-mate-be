package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.constants.*;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.event.TopicMasterRequest;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.enumeration.IeltsType;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.listeningservice.model.json.QuestionVersion;
import com.fptu.sep490.listeningservice.model.specification.ListeningTaskSpecification;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.repository.client.MarkupClient;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    MarkupClient markupClient;
    KafkaTemplate<String, Object> kafkaTemplate;

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

    @Value("${topic.topic-master}")
    @NonFinal
    String topicMasterTopic;

    @Override
    @Transactional
    public ListeningTaskResponse createListeningTask(ListeningTaskCreationRequest request,
                                                     HttpServletRequest httpServletRequest) throws IOException {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        if(!safeEnumFromOrdinal(Status.values(), request.status()).equals(Status.DRAFT)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CAN_ONLY_CREATE_DRAFT,
                    Constants.ErrorCode.CAN_ONLY_CREATE_DRAFT,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        MultipartFile audio = request.audioFile();
        if (audio == null || audio.isEmpty() || !audio.getContentType().startsWith("audio/")) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // 2) validate size (e.g. max 10MB)
        long maxBytes = 20 * 1024 * 1024;
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
                .transcription(request.isAutomaticTranscription() ? null: request.transcription())
                .isOriginal(true)
                .isCurrent(true)
                .parent(null)
                .createdBy(userId)
                .updatedBy(userId)
                .isDeleted(false)
                .version(1)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .updatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();

        ListeningTask saved = listeningTaskRepository.save(listeningTask);

        fileService.uploadAsync("listening-tasks", audio, saved.getTaskId(), UUID.fromString(userId), request.isAutomaticTranscription());
        TopicMasterRequest topicMaterRequest = TopicMasterRequest.builder()
                .type(TopicType.LISTENING_TYPE)
                .operation(Operation.CREATE)
                .taskId(saved.getTaskId())
                .title(saved.getTitle())
                .build();
        kafkaTemplate.send(topicMasterTopic, topicMaterRequest);
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
        int totalPoint = 0;
        ListeningTaskGetAllResponse taskDetail = getTaskById(taskId);
        var questionGroups = taskDetail.questionGroups();

        for(var group : questionGroups) {
            for(var question : group.questions()) {
                totalPoint += question.point();
            }
        }

        if(task.getPartNumber().equals(PartNumber.PART_1)
                && totalPoint != TotalPoint.PART_QUESTION_LISTENING
                &&( safeEnumFromOrdinal(Status.values(),ieltsType).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), ieltsType).equals(Status.PUBLISHED))
                && status != Status.DRAFT.ordinal()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_1_POINT_INVALID,
                    Constants.ErrorCode.PART_1_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if(task.getPartNumber().equals(PartNumber.PART_2) && totalPoint != TotalPoint.PART_QUESTION_LISTENING
                &&( safeEnumFromOrdinal(Status.values(),ieltsType).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), ieltsType).equals(Status.PUBLISHED))
                && status != Status.DRAFT.ordinal()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_2_POINT_INVALID,
                    Constants.ErrorCode.PART_2_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if(task.getPartNumber().equals(PartNumber.PART_3) && totalPoint != TotalPoint.PART_QUESTION_LISTENING
                &&( safeEnumFromOrdinal(Status.values(),ieltsType).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), ieltsType).equals(Status.PUBLISHED))
                && status != Status.DRAFT.ordinal()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_3_POINT_INVALID,
                    Constants.ErrorCode.PART_3_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if(task.getPartNumber().equals(PartNumber.PART_4) && totalPoint != TotalPoint.PART_QUESTION_LISTENING
                &&( safeEnumFromOrdinal(Status.values(),ieltsType).equals(Status.TEST)
                || safeEnumFromOrdinal(Status.values(), ieltsType).equals(Status.PUBLISHED))
                && status != Status.DRAFT.ordinal()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PART_4_POINT_INVALID,
                    Constants.ErrorCode.PART_4_POINT_INVALID,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        int version = 0;
        task.setIsCurrent(false);
        List<ListeningTask> allVersion = listeningTaskRepository.findAllVersion(task.getTaskId());
        for(var v : allVersion) {
            if(v.getVersion() > version) {
                version = v.getVersion();
            }
            v.setIsCurrent(false);
        }
        ListeningTask newVersion = ListeningTask.builder().build();

        if (!Objects.isNull(status)) {
            task.setStatus(safeEnumFromOrdinal(Status.values(), status));
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
        newVersion.setVersion(version + 1);
        newVersion.setParent(task);
        newVersion.setIsOriginal(false);
        newVersion.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        newVersion.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        listeningTaskRepository.saveAll(allVersion);
        newVersion = listeningTaskRepository.save(newVersion);
        listeningTaskRepository.save(task);

        listeningTaskRepository.saveAll(allVersion);
        newVersion= listeningTaskRepository.save(newVersion);
        listeningTaskRepository.save(task);
        if(!Objects.isNull(audioFile)) {
            fileService.uploadAsync("listening-tasks", audioFile, newVersion.getTaskId(), UUID.fromString(userId), false);
        } else {
            newVersion.setAudioFileId(task.getAudioFileId());
        }
        listeningTaskRepository.saveAll(allVersion);
        listeningTaskRepository.save(newVersion);
        listeningTaskRepository.save(task);
        TopicMasterRequest topicMaterRequest = TopicMasterRequest.builder()
                .type(TopicType.LISTENING_TYPE)
                .operation(Operation.UPDATE)
                .taskId(task.getTaskId())
                .title(newVersion.getTitle())
                .build();
        kafkaTemplate.send(topicMasterTopic, topicMaterRequest);
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
        TopicMasterRequest topicMaterRequest = TopicMasterRequest.builder()
                .type(TopicType.LISTENING_TYPE)
                .operation(Operation.DELETE)
                .taskId(origin.getTaskId())
                .title(origin.getTitle())
                .build();
        kafkaTemplate.send(topicMasterTopic, topicMaterRequest);
        listeningTaskRepository.saveAll(tasks);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListeningTaskGetResponse> getActivatedTask(
            int page,
            int size,
            List<Integer> ieltsType,
            List<Integer> partNumber,
            String questionCategory,
            String sortBy,
            String sortDirection,
            String title,
            String createdBy,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(page, size);

        // status = 1 (Activated) — tùy enum của bạn, giữ nguyên như code cũ
        var spec = ListeningTaskSpecification.byCondition(
                ieltsType, List.of(1), partNumber, questionCategory,
                sortBy, sortDirection, title, createdBy
        );

        Page<ListeningTask> pageResult = listeningTaskRepository.findAll(spec, pageable);
        List<ListeningTask> tasks = pageResult.getContent();

        if (tasks.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, pageResult.getTotalElements());
        }

        // Lấy danh sách task gốc trong trang hiện tại
        List<UUID> rootTaskIdsInPage = tasks.stream()
                .map(ListeningTask::getTaskId)
                .toList();

        // Tìm các phiên bản hiện tại (latest/current) theo các root task id vừa có
        // Map theo rootId => currentVersion
        Map<UUID, ListeningTask> latestByRootId = listeningTaskRepository
                .findCurrentVersionsByIds(rootTaskIdsInPage)
                .stream()
                .collect(Collectors.toMap(
                        lt -> (lt.getParent() != null ? lt.getParent().getTaskId() : lt.getTaskId()),
                        Function.identity(),
                        (a, b) -> a // merge giữ phần tử đầu (không quan trọng nếu unique)
                ));

        List<ListeningTaskGetResponse> responsesData = new ArrayList<>(tasks.size());

        for (ListeningTask root : tasks) {
            // Lấy phiên bản mới nhất theo root; nếu không có, dùng chính root
            ListeningTask current = latestByRootId.getOrDefault(root.getTaskId(), root);

            // createdBy/createdAt lấy theo root (gốc), updatedBy/updatedAt theo current
            String createdById = (root.getCreatedBy());
            String updatedById = current.getUpdatedBy(); // có thể null

            UserInformationResponse createdByProfile = null;
            UserInformationResponse updatedByProfile = null;

            try {
                if (createdById != null) {
                    var created = getUserProfileById(createdById);
                    createdByProfile = UserInformationResponse.builder()
                            .userId(created.id())
                            .lastName(created.lastName())
                            .firstName(created.firstName())
                            .email(created.email())
                            .build();
                }
                if (updatedById != null) {
                    var updated = getUserProfileById(updatedById);
                    updatedByProfile = UserInformationResponse.builder()
                            .userId(updated.id())
                            .lastName(updated.lastName())
                            .firstName(updated.firstName())
                            .email(updated.email())
                            .build();
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            ListeningTaskGetResponse dto = ListeningTaskGetResponse.builder()
                    .taskId(root.getTaskId()) // luôn trả về id của task gốc để nhất quán
                    .title(current.getTitle())
                    .ieltsType(current.getIeltsType().ordinal())
                    .partNumber(current.getPartNumber().ordinal())
                    .status(current.getStatus().ordinal())
                    .createdAt(root.getCreatedAt() != null ? root.getCreatedAt().toString() : null)
                    .updatedAt(current.getUpdatedAt() != null ? current.getUpdatedAt().toString() : null)
                    .createdBy(createdByProfile)
                    .updatedBy(updatedByProfile)
                    .build();

            responsesData.add(dto);
        }

        // Enrich Markup nếu có token
        String accessToken = CookieUtils.getCookieValue(request, CookieConstants.ACCESS_TOKEN);
        if (accessToken != null) {
            var response = markupClient.getMarkedUpData("Bearer " + accessToken, DataMarkup.LISTENING_TASK);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                var body = response.getBody();
                Map<UUID, Integer> taskIdsMarkup = body.data().markedUpIdsMapping();

                // map lại list để chèn isMarkedUp & markupTypes
                responsesData = responsesData.stream()
                        .map(t -> ListeningTaskGetResponse.builder()
                                .taskId(t.taskId())
                                .ieltsType(t.ieltsType())
                                .partNumber(t.partNumber())
                                .status(t.status())
                                .title(t.title())
                                .createdBy(t.createdBy())
                                .createdAt(t.createdAt())
                                .updatedBy(t.updatedBy())
                                .updatedAt(t.updatedAt())
                                .isMarkedUp(taskIdsMarkup.get(t.taskId()) != null)
                                .markupTypes(taskIdsMarkup.get(t.taskId()))
                                .build())
                        .toList();

                return new PageImpl<>(responsesData, pageable, pageResult.getTotalElements());
            }
        }

        // ✅ Trả về đúng biến đã build
        return new PageImpl<>(responsesData, pageable, pageResult.getTotalElements());
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

//        for(ListeningTask task : tasks) {
//            ListeningTask lastVersion = lastestVersion.get(task.getTaskId());
//            if (!Objects.isNull(lastVersion)) {
//                task.setTitle(lastVersion.getTitle());
//                task.setIeltsType(lastVersion.getIeltsType());
//                task.setPartNumber(lastVersion.getPartNumber());
//                task.setStatus(lastVersion.getStatus());
//            }
//        }
        List<ListeningTaskGetResponse> responses = new ArrayList<>();
        lastestVersion.forEach((key, value) -> {
            if(value.getParent() != null) {
                try {
                    var createdByProfile = getUserProfileById(value.getParent().getCreatedBy());
                    var updatedByProfile = getUserProfileById(value.getUpdatedBy());
                    ListeningTaskGetResponse data = ListeningTaskGetResponse.builder()
                            .taskId(value.getParent().getTaskId())
                            .title(value.getTitle())
                            .ieltsType(value.getIeltsType().ordinal())
                            .partNumber(value.getPartNumber().ordinal())
                            .status(value.getStatus().ordinal())
                            .createdAt(value.getParent().getCreatedAt().toString())
                            .updatedAt(value.getUpdatedAt().toString())
                            .createdBy(UserInformationResponse.builder()
                                    .userId(createdByProfile.id())
                                    .lastName(createdByProfile.lastName())
                                    .firstName(createdByProfile.firstName())
                                    .email(createdByProfile.email())
                                    .build())
                            .updatedBy(UserInformationResponse.builder()
                                    .userId(updatedByProfile.id())
                                    .lastName(updatedByProfile.lastName())
                                    .firstName(updatedByProfile.firstName())
                                    .email(updatedByProfile.email())
                                    .build())
                            .build();
                    responses.add(data);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ListeningTaskGetResponse data = toListeningTaskGetResponse(value);
                responses.add(data);
            }
        });
//        List<ListeningTaskGetResponse> responses = tasks.stream()
//                .map(this :: toListeningTaskGetResponse)
//                .toList();
        List<ListeningTaskGetResponse> filtered = responses;
        return new PageImpl<>(filtered.stream().sorted(Comparator.comparing(ListeningTaskGetResponse::createdAt).reversed()).toList(), pageable, pageResult.getTotalElements());
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
                List<Choice> choices = questionMapChoices.get(question.getQuestionId()) == null ? questionMapChoices.get(question.getParent().getQuestionId()) : questionMapChoices.get(question.getQuestionId());

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
                                .choices(choices != null ? choices.stream()
                                        .map(c -> ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse
                                                .ChoiceResponse.builder().choiceId(c.getParent() == null ? c.getChoiceId() : c.getParent().getChoiceId())
                                                .label(c.getLabel())
                                                .choiceOrder(c.getChoiceOrder())
                                                .content(c.getContent())
                                                .isCorrect(c.isCorrect())
                                                .build())
                                        .sorted(Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.ChoiceResponse::choiceOrder))
                                        .toList() : null)
                                .build();
                questionListResponse.add(questionResponse);
            });
            ListeningTaskGetAllResponse.QuestionGroupResponse group = ListeningTaskGetAllResponse.QuestionGroupResponse.builder()
                    .groupId(key.getParent() == null ? key.getGroupId() : key.getParent().getGroupId())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .questionType(key.getQuestionType().ordinal())
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

    // Find parent or return self if no parent exists
    public Question findOriginalQuestion(Question question) {
        Question current = question;
        while (current != null && current.getParent() != null) {
            current = current.getParent();
        }
        return current != null && !current.getIsDeleted() ? current : null;
    }



    @Override
    public CreateExamAttemptResponse.ListeningExamResponse.ListeningTaskResponse fromListeningTask(String taskId) {
        ListeningTask task = listeningTaskRepository.findById(UUID.fromString(taskId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.LISTENING_TASK_NOT_FOUND,
                        Constants.ErrorCode.LISTENING_TASK_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

//        ListeningTask currentVersion = listeningTaskRepository.findCurrentVersionById(task.getTaskId())
//                .orElseThrow(() -> new AppException(
//                        Constants.ErrorCodeMessage.LISTENING_TASK_NOT_FOUND,
//                        Constants.ErrorCode.LISTENING_TASK_NOT_FOUND,
//                        HttpStatus.NOT_FOUND.value()
//                ));

        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByListeningTaskByTaskId(task.getTaskId());
//        if (questionGroups.isEmpty()) {
//            throw new AppException(
//                    Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
//                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
//                    HttpStatus.NOT_FOUND.value()
//            );
//        }

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
                    List<Choice> choices = new ArrayList<>();
                    if (currentVersionQuestion.getParent() == null) {
                        List<Choice> currentVersionChoices = choiceRepository.getVersionChoiceByQuestionId(
                                currentVersionQuestion.getQuestionId());
                        Map<UUID, Choice> uniqueChoices = new LinkedHashMap<>();
                        for (Choice c : currentVersionChoices) {
                            uniqueChoices.put(c.getChoiceId(), c);
                        }
                        choices.addAll(uniqueChoices.values());
                        choices.stream()
                                .map(Choice::getChoiceId)
                                .forEach(choiceVersionIds::add);
                        if (choices.isEmpty()) {
                            throw new AppException(
                                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                                    Constants.ErrorCode.CHOICE_NOT_FOUND,
                                    HttpStatus.NOT_FOUND.value()
                            );
                        }
                        currentVersionChoicesByQuestion.put(currentVersionQuestion, choices);
                    } else {
                        List<Choice> originVersionChoices = choiceRepository.getVersionChoiceByParentQuestionId(
                                currentVersionQuestion.getParent().getQuestionId());
                        Map<UUID, Choice> uniqueChoices = new LinkedHashMap<>();
                        for (Choice choice : originVersionChoices) {
                            Choice toAdd;
                            if (!choice.getIsCurrent()) {
                                toAdd = choiceRepository.getCurrentVersionChoiceByChoiceId(choice.getChoiceId());
                            } else {
                                toAdd = choice;
                            }
                            if (toAdd != null) {
                                uniqueChoices.put(toAdd.getChoiceId(), toAdd);
                            }
                        }
                        choices.addAll(uniqueChoices.values());
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
                                                                .sorted(Comparator.comparing(AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse::choiceOrder))
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

                            return AttemptResponse.QuestionGroupAttemptResponse.builder()
                                            .questionGroupId(group.getGroupId())
                                    .sectionOrder(group.getSectionOrder())
                                    .sectionLabel(group.getSectionLabel())
                                    .instruction(group.getInstruction())
                                    .questions(questionResponses)
                                    .dragItems(currentVersionDragItemsByGroup.getOrDefault(group, Collections.emptyList()).stream()
                                                    .filter(DragItem::getIsCurrent)
                                                    .map(d -> AttemptResponse.QuestionGroupAttemptResponse.DragItemResponse.builder()
                                                            .dragItemId(d.getParent() == null
                                                                    ? d.getDragItemId().toString()
                                                                    : d.getParent().getDragItemId().toString())
                                                            .content(d.getContent())
                                                            .build())
                                                    .toList())
                                    .build();
                        })
                        .collect(Collectors.toList());

        return CreateExamAttemptResponse.ListeningExamResponse.ListeningTaskResponse.builder()
                .taskId(task.getTaskId())
                .ieltsType(task.getIeltsType().ordinal())
                .partNumber(task.getPartNumber().ordinal())
                .instruction(task.getInstruction())
                .title(task.getTitle())
                .audioFileId(task.getAudioFileId())
                .questionGroups(questionGroupResponses)
                .build();
    }

    @Override
    public List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse> fromExamAttemptHistory(ExamAttemptHistory history) {
        List<ListeningTask> tasks = listeningTaskRepository.findAllByIdSortedByPartNumber(history.getTaskId());
        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByIdOrderBySectionOrder(history.getQuestionGroupIds());
        List<Question> questions = questionRepository.findAllByIdOrderByQuestionOrder(history.getQuestionIds());

        List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse> taskResponses = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (ListeningTask task : tasks) {
            int partNumber = task.getPartNumber().ordinal();

            List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.QuestionGroupAttemptResponse> questionGroupsList = new ArrayList<>();
            for (QuestionGroup group : questionGroups) {
                if (group.getListeningTask().getPartNumber().ordinal() == partNumber) {

                    // --- drag items ---
                    List<UpdatedQuestionResponse.DragItemResponse> dragItemResponses = new ArrayList<>();
                    if (group.getDragItems() != null && !group.getDragItems().isEmpty()) {
                        List<UUID> dragItemIds = history.getGroupMapItems().getOrDefault(group.getGroupId(), Collections.emptyList());
                        List<DragItem> dragItems = dragItemRepository.findAllById(dragItemIds);
                        for (DragItem dragItem : dragItems) {
                            dragItemResponses.add(
                                    UpdatedQuestionResponse.DragItemResponse.builder()
                                            .dragItemId(dragItem.getDragItemId().toString())
                                            .content(dragItem.getContent())
                                            .build()
                            );
                        }
                    }

                    // --- questions ---
                    List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse> questionAttemptResponses = new ArrayList<>();
                    for (Question question : questions) {
                        if (question.getQuestionGroup() != null && question.getQuestionGroup().getGroupId().equals(group.getGroupId())) {

                            // --- choices ---
                            List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse> choiceAttemptResponses = new ArrayList<>();
                            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                                for (UUID choiceId : history.getQuestionMapChoices().getOrDefault(question.getQuestionId(), Collections.emptyList())) {
                                    Choice choice = choiceRepository.findById(choiceId)
                                            .orElseThrow(() -> new AppException(
                                                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                                                    Constants.ErrorCode.CHOICE_NOT_FOUND,
                                                    HttpStatus.NOT_FOUND.value()
                                            ));
                                    choiceAttemptResponses.add(
                                            ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse.builder()
                                                    .choiceId(choice.getChoiceId())
                                                    .label(choice.getLabel())
                                                    .content(choice.getContent())
                                                    .choiceOrder(choice.getChoiceOrder())
                                                    .isCorrect(choice.isCorrect())
                                                    .build()
                                    );
                                }
                            }

                            // --- parse explanation only if valid JSON with start_time & end_time ---
                            String startTime = null;
                            String endTime = null;
                            String explanation = question.getExplanation();
                            try {
                                if (explanation != null && explanation.trim().startsWith("{")) {
                                    JsonNode node = mapper.readTree(explanation);
                                    if (node.has("start_time") && node.has("end_time")) {
                                        startTime = node.get("start_time").asText();
                                        endTime = node.get("end_time").asText();
                                        // don’t keep explanation if it’s just JSON
                                        explanation = null;
                                    }
                                }
                            } catch (Exception ignored) {
                                // explanation stays as-is
                            }

                            questionAttemptResponses.add(
                                    ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.builder()
                                            .questionId(question.getQuestionId())
                                            .questionOrder(question.getQuestionOrder())
                                            .questionType(question.getQuestionType().ordinal())
                                            .blankIndex(question.getBlankIndex())
                                            .instructionForChoice(question.getInstructionForChoice())
                                            .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                                            .instructionForMatching(question.getInstructionForMatching())
                                            .zoneIndex(question.getZoneIndex())
                                            .choices(choiceAttemptResponses)
                                            .correctAnswer(question.getQuestionType() != QuestionType.DRAG_AND_DROP ? question.getCorrectAnswer() : question.getDragItem().getDragItemId().toString())
                                            .correctAnswerForMatching(question.getCorrectAnswerForMatching())
                                            .explanation(explanation) // only set if not JSON
                                            .point(question.getPoint())
                                            .startTime(startTime)     // parsed if JSON
                                            .endTime(endTime)         // parsed if JSON
                                            .build()
                            );
                        }
                    }

                    questionGroupsList.add(
                            ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.QuestionGroupAttemptResponse.builder()
                                    .questionGroupId(group.getGroupId())
                                    .sectionOrder(group.getSectionOrder())
                                    .sectionLabel(group.getSectionLabel())
                                    .instruction(group.getInstruction())
                                    .questions(questionAttemptResponses)
                                    .dragItems(dragItemResponses)
                                    .build()
                    );
                }
            }

            taskResponses.add(
                    ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse.builder()
                            .taskId(task.getTaskId())
                            .title(task.getTitle())
                            .instruction(task.getInstruction())
                            .audioFileId(task.getAudioFileId())
                            .ieltsType(task.getIeltsType().ordinal())
                            .partNumber(task.getPartNumber().ordinal())
                            .questionGroups(questionGroupsList)
                            .build()
            );
        }

        return taskResponses;
    }

    @Override
    public List<TaskTitle> getTaskTitle(List<UUID> taskIds) {
        List<ListeningTask> tasks = listeningTaskRepository.findAllById(taskIds);
        return tasks.stream().map(t -> TaskTitle.builder()
                .title(t.getTitle())
                .taskId(t.getTaskId())
                .build()).toList();
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
