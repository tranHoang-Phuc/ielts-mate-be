package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;

import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.embedded.AnswerAttemptId;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.model.json.AttemptVersion;
import com.fptu.sep490.listeningservice.model.json.QuestionVersion;
import com.fptu.sep490.listeningservice.repository.*;

import com.fptu.sep490.listeningservice.model.specification.AttemptSpecification;
import com.fptu.sep490.listeningservice.service.AttemptService;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList;
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
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class AttemptServiceImpl implements AttemptService {

    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;
    AttemptRepository attemptRepository;
    AnswerAttemptRepository answerAttemptRepository;

    ObjectMapper objectMapper;
    Helper helper;
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
    @Transactional
    public AttemptResponse createAttempt(UUID listeningTaskId, HttpServletRequest request) throws JsonProcessingException {
        String userId = helper.getUserIdFromToken(request);
        ListeningTask originalTask = listeningTaskRepository.findById(listeningTaskId)
                .orElseThrow(
                        () -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        ListeningTask currentVersion = listeningTaskRepository.findLastestVersion(listeningTaskId);
        if (currentVersion.getStatus() == null || currentVersion.getStatus() != Status.PUBLISHED) {
            throw new AppException(
                    Constants.ErrorCodeMessage.LISTENING_TASK_NOT_ACTIVATED,
                    Constants.ErrorCode.LISTENING_TASK_NOT_ACTIVATED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        AttemptVersion attemptVersion = AttemptVersion.builder()
                .taskId(currentVersion.getTaskId())
                .build();
        Map<UUID, List<QuestionVersion>> questionVersions = new HashMap<>();
        Map<UUID, List<UUID>> groupMapDragItem = new HashMap<>();


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

            // mapping group with dragItem for version
            groupMapDragItem.put(g.getGroupId(), currentVersionDragItem.stream().map(DragItem::getDragItemId).toList());

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
            questionVersions.put(g.getGroupId(), questionVersionList);
            currentGroupIdMapQuestionIdMapCurrentChoice.put(g.getGroupId(), questionIdMapCurrentChoice);
        });

        attemptVersion.setGroupMappingDragItem(groupMapDragItem);
        attemptVersion.setGroupMappingQuestion(questionVersions);
        Attempt attempt = Attempt.builder()
                .createdBy(userId)
                .status(Status.DRAFT)
                .duration(0L)
                .listeningTask(originalTask)
                .version(objectMapper.writeValueAsString(attemptVersion))
                .build();
        attempt = attemptRepository.save(attempt);

        List<AttemptResponse.QuestionGroupAttemptResponse> groups = new ArrayList<>();
        currentGroupMapCurrentDragItem.forEach((key, value) ->{

            List<AttemptResponse.QuestionGroupAttemptResponse.DragItemResponse> dragItems =
                    value.stream()
                            .map(item -> AttemptResponse.QuestionGroupAttemptResponse.DragItemResponse
                                    .builder()
                                    .dragItemId(item.getDragItemId().toString())
                                    .content(item.getContent())
                                    .build()
                            )
                            .toList();

            List<Question> questions = currentGroupIdQuestions.get(key.getGroupId());
            Map<UUID, List<Choice>> questionMapChoices = currentGroupIdMapQuestionIdMapCurrentChoice.get(key.getGroupId());
            
            final List<Question> finalQuestions = questions != null ? questions : Collections.emptyList();
            final Map<UUID, List<Choice>> finalQuestionMapChoices = questionMapChoices != null ? questionMapChoices : Collections.emptyMap();

            List<AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse> questionListResponse = new ArrayList<>();

            finalQuestions.forEach(question -> {
                List<Choice> choices = finalQuestionMapChoices.get(question.getParent() != null ? question.getParent().getQuestionId() : question.getQuestionId());
                AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse questionResponse = AttemptResponse.
                        QuestionGroupAttemptResponse.QuestionAttemptResponse.builder()
                        .questionId(question.getQuestionId())
                        .questionOrder(question.getQuestionOrder())
                        .questionType(question.getQuestionType().ordinal())
                        .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                        .blankIndex(question.getBlankIndex())
                        .instructionForChoice(question.getInstructionForChoice())
                        .instructionForMatching(question.getInstructionForMatching())
                        .zoneIndex(question.getZoneIndex())
                        .choices(
                                choices != null ? choices.stream().map(c ->
                                                AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse.builder()
                                                        .choiceId(c.getChoiceId())
                                                        .label(c.getLabel())
                                                        .content(c.getContent())
                                                        .choiceOrder(question.getQuestionOrder())
                                                        .build()
                                        ).sorted(Comparator.comparing(AttemptResponse.QuestionGroupAttemptResponse.
                                        QuestionAttemptResponse.ChoiceAttemptResponse::choiceOrder)).toList()
                                        : Collections.emptyList()
                        )
                        .build();
                questionListResponse.add(questionResponse);
            });

            AttemptResponse.QuestionGroupAttemptResponse group = AttemptResponse.QuestionGroupAttemptResponse.builder()
                    .questionGroupId(key.getGroupId())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .instruction(key.getInstruction())
                    .dragItems(dragItems)
                    .questions(questionListResponse.stream().sorted(Comparator.comparing(AttemptResponse.
                            QuestionGroupAttemptResponse.QuestionAttemptResponse::questionOrder)).toList())
                    .build();

            groups.add(group);
        });

        return AttemptResponse.builder()
                .taskId(currentVersion.getTaskId())
                .attemptId(attempt.getAttemptId())
                .title(currentVersion.getTitle())
                .instruction(currentVersion.getInstruction())
                .ieltsType(currentVersion.getIeltsType().ordinal())
                .partNumber(currentVersion.getPartNumber().ordinal())
                .audioFileId(currentVersion.getAudioFileId())
                .questionGroups(groups.stream().sorted(Comparator.comparing(AttemptResponse
                        .QuestionGroupAttemptResponse::sectionOrder)).toList())
                .build();


    }

    @Override
    public void saveAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers) {
        Attempt attempt = attemptRepository.findById(UUID.fromString(attemptId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        String userId = helper.getUserIdFromToken(request);
        if (!attempt.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        if (attempt.getStatus() != Status.DRAFT) {
            throw new AppException(
                    Constants.ErrorCodeMessage.ATTEMPT_NOT_DRAFT,
                    Constants.ErrorCode.ATTEMPT_NOT_DRAFT,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        attempt.setDuration(answers.duration());

        for (SavedAnswersRequest ans : answers.answers()) {
            Question question = questionRepository.findById(ans.questionId())
                    .orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                            Constants.ErrorCode.QUESTION_NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));

            AnswerAttemptId key = AnswerAttemptId.builder()
                    .attemptId(UUID.fromString(attemptId))
                    .questionId(question.getQuestionId())
                    .build();

            AnswerAttempt attemptAnswer = Optional.ofNullable(answerAttemptRepository.findAnswerAttemptById(key))
                    .orElseGet(() -> AnswerAttempt.builder()
                            .id(key)
                            .attempt(attempt)
                            .question(question)
                            .build()
                    );

            switch (question.getQuestionType()) {
                case MULTIPLE_CHOICE:
                    attemptAnswer.setChoices(ans.choices());
                    break;
                case FILL_IN_THE_BLANKS:
                    attemptAnswer.setDataFilled(ans.dataFilled());
                    break;
                case MATCHING:
                    attemptAnswer.setDataMatched(ans.dataMatched());
                    break;
                case DRAG_AND_DROP:
                    attemptAnswer.setDragItemId(ans.dragItemId());
                    break;
                default:
                    throw new AppException(
                            Constants.ErrorCodeMessage.INVALID_QUESTION_TYPE,
                            Constants.ErrorCode.INVALID_QUESTION_TYPE,
                            HttpStatus.BAD_REQUEST.value()
                    );
            }

            answerAttemptRepository.save(attemptAnswer);
            attemptRepository.save(attempt);
        }
    }

    @Override
    public UserDataAttempt loadAttempt(String attemptId, HttpServletRequest request) throws JsonProcessingException {
        Attempt attempt = attemptRepository.findById(UUID.fromString(attemptId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        String userId = helper.getUserIdFromToken(request);
        if (!attempt.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        if (attempt.getStatus() != Status.DRAFT) {
            throw new AppException(
                    Constants.ErrorCodeMessage.ATTEMPT_NOT_DRAFT,
                    Constants.ErrorCode.ATTEMPT_NOT_DRAFT,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // Check if attempt is currently active in another session
        checkAttemptSessionActivity(UUID.fromString(attemptId), userId);

        String rawJson = attempt.getVersion(); // Là một chuỗi chứa JSON

        JsonNode decodedNode = objectMapper.readTree(rawJson);

        AttemptVersion questionVersion = objectMapper.treeToValue(decodedNode, AttemptVersion.class);

        ListeningTask listeningTask = listeningTaskRepository.findById(questionVersion.getTaskId())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        Map<UUID, List<QuestionVersion>> groupMappingQuestion = questionVersion.getGroupMappingQuestion();
        Map<UUID, List<UUID>> groupMappingDragItem = questionVersion.getGroupMappingDragItem();

        Map<QuestionGroup, List<Question>> groupQuestions = new HashMap<>();
        Map<UUID, List<DragItem>> groupDragItems = new HashMap<>();
        Map<UUID, List<Choice>> questionChoice = new HashMap<>();
        groupMappingQuestion.forEach((key, value) ->{
            var groupOptional = questionGroupRepository.findById(key);
            if (groupOptional.isEmpty()) {
                return;
            }
            var group = groupOptional.get();
            List<Question> questions = new ArrayList<>();
            value.forEach(q -> {
                var questionOptional = questionRepository.findById(q.getQuestionId());
                if (questionOptional.isEmpty()) {
                    return;
                }
                var question = questionOptional.get();
                if(question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    List<Choice> choices = choiceRepository.findAllById(q.getChoiceMapping());
                    questionChoice.put(question.getQuestionId(), choices);
                }
                questions.add(question);
            });
            groupQuestions.put(group, questions);

            List<UUID> dragItemIds = groupMappingDragItem.get(group.getGroupId());
            if (dragItemIds != null) {
                List<DragItem> dragItems = dragItemRepository.findAllById(dragItemIds);
                groupDragItems.put(key, dragItems);
            } else {
                groupDragItems.put(key, Collections.emptyList());
            }
        });

        List<ListeningTaskGetAllResponse.QuestionGroupResponse> questionGroups = new ArrayList<>();
        groupQuestions.forEach((key, value) ->{

            ListeningTaskGetAllResponse.QuestionGroupResponse groupResponse = ListeningTaskGetAllResponse
                    .QuestionGroupResponse.builder()
                    .groupId(key.getGroupId())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .instruction(key.getInstruction())
                    .questions(value.stream().map(q ->
                                    ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.builder()
                                            .questionId(q.getQuestionId())
                                            .questionOrder(q.getQuestionOrder())
                                            .questionType(q.getQuestionType().ordinal())
                                            .point(q.getPoint())
                                            .numberOfCorrectAnswers(q.getNumberOfCorrectAnswers())
                                            .instructionForMatching(q.getInstructionForMatching())
                                            .zoneIndex(q.getZoneIndex())
                                            .choices(q.getQuestionType() == QuestionType.MULTIPLE_CHOICE && 
                                                     questionChoice.get(q.getQuestionId()) != null ?
                                                    questionChoice.get(q.getQuestionId()).stream()
                                                            .map(c -> ListeningTaskGetAllResponse.QuestionGroupResponse
                                                                    .QuestionResponse.ChoiceResponse.builder()
                                                                    .choiceId(c.getChoiceId())
                                                                    .label(c.getLabel())
                                                                    .choiceOrder(c.getChoiceOrder())
                                                                    .content(c.getContent())
                                                                    .build()).sorted(Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.ChoiceResponse::choiceOrder)).toList() : null)
                                            .build()
                            )
                            .sorted(Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse::questionOrder)).toList())
                    .dragItems(groupDragItems.get(key.getGroupId()) != null ? 
                              groupDragItems.get(key.getGroupId()).stream().map(i ->
                                    ListeningTaskGetAllResponse.QuestionGroupResponse.DragItemResponse.builder()
                                            .dragItemId(i.getDragItemId())
                                            .content(i.getContent())
                                            .build()
                            ).toList() : Collections.emptyList())
                    .build();
            questionGroups.add(groupResponse);
        });

        ListeningTaskGetAllResponse attemptResponse = ListeningTaskGetAllResponse.builder()
                .taskId(listeningTask.getTaskId())
                .ieltsType(listeningTask.getIeltsType().ordinal())
                .partNumber(listeningTask.getPartNumber().ordinal())
                .instruction(listeningTask.getInstruction())
                .title(listeningTask.getTitle())
                .audioFileId(listeningTask.getAudioFileId())
                .questionGroups(questionGroups.stream().sorted(
                        Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse::sectionOrder)
                ).toList())
                .build();

        List<AnswerAttempt> answerAttempts = answerAttemptRepository.findByAttempt(attempt);
        List<UserDataAttempt.AnswerChoice> answerChoices = new ArrayList<>();
        for (AnswerAttempt answerAttempt : answerAttempts) {
            UserDataAttempt.AnswerChoice answerChoice = UserDataAttempt.AnswerChoice.builder()
                    .questionId(answerAttempt.getQuestion() != null ? answerAttempt.getQuestion().getQuestionId() : null)
                    .dragItemId(answerAttempt.getDragItemId() != null ? answerAttempt.getDragItemId() : null)
                    .filledTextAnswer(answerAttempt.getDataFilled() != null ? answerAttempt.getDataFilled() : null)
                    .matchedTextAnswer(answerAttempt.getDataMatched() != null ? answerAttempt.getDataMatched() : null)
                    .choiceIds(answerAttempt.getChoices() != null ? answerAttempt.getChoices() : Collections.emptyList())
                    .build();
            answerChoices.add(answerChoice);


        }
        return UserDataAttempt.builder()
                .attemptId(attempt.getAttemptId())
                .answers(answerChoices)
                .totalPoints(attempt.getTotalPoints())
                .duration(attempt.getDuration())
                .attemptResponse(attemptResponse)
                .build();
    }

    @Override
    @Transactional
    public SubmittedAttemptResponse submitAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers) throws JsonProcessingException {
        Attempt attempt = attemptRepository.findById(UUID.fromString(attemptId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        String userId = helper.getUserIdFromToken(request);
        if (!attempt.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        if (attempt.getStatus().equals(Status.FINISHED)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.ATTEMPT_ALREADY_SUBMITTED,
                    Constants.ErrorCode.ATTEMPT_ALREADY_SUBMITTED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        String rawJson = attempt.getVersion(); // Là một chuỗi chứa JSON

        // Bước 1: chuyển từ chuỗi JSON lồng sang object JSON (giải mã lần đầu)
        JsonNode decodedNode = objectMapper.readTree(rawJson);

        // Bước 2: map node sang AttemptVersion
        AttemptVersion questionVersion = objectMapper.treeToValue(decodedNode, AttemptVersion.class);

        // Lấy list question của bài đọc
        List<Question> questions = new ArrayList<>();
        questionVersion.getGroupMappingQuestion().forEach((groupId, questionVersions) -> {
            List<Question> currentQuestions = questionRepository.findQuestionsByIds(
                    questionVersions.stream()
                            .map(QuestionVersion::getQuestionId)
                            .collect(Collectors.toList()));
            questions.addAll(currentQuestions);
        });

        // Tạo hashMap answers để compare
        Map<UUID, SavedAnswersRequest> savedAnswers = new HashMap<>();
        for (SavedAnswersRequest savedAnswer : answers.answers()) {
            savedAnswers.put(savedAnswer.questionId(), savedAnswer);
        }
        List<SubmittedAttemptResponse.ResultSet> resultSets = new ArrayList<>();

        int totalPoints = 0;
        for (Question question : questions) {
            SavedAnswersRequest answer = savedAnswers.get(question.getQuestionId());
            if (Objects.isNull(answer)) {
                continue;
            }
            AnswerAttemptId answerAttemptId = AnswerAttemptId.builder()
                    .questionId(question.getQuestionId())
                    .attemptId(attempt.getAttemptId())
                    .build();

            AnswerAttempt answerAttempt = answerAttemptRepository.findAnswerAttemptByAttemptId(answerAttemptId)
                    .orElse(AnswerAttempt.builder()
                            .attempt(attempt)
                            .id(answerAttemptId)
                            .question(question)
                            .build());

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                SubmittedAttemptResponse.ResultSet result = scoreMultipleChoiceQuestion(answer, question);
                resultSets.add(result);
                answerAttempt.setChoices(answer.choices());
                answerAttempt.setIsCorrect(result.isCorrect());
                if(result.isCorrect()) {
                    totalPoints += question.getPoint();
                }
            }
            if (question.getQuestionType() == QuestionType.FILL_IN_THE_BLANKS) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(answer.dataFilled() != null ? List.of(answer.dataFilled()) : Collections.emptyList())
                        .explanation(question.getExplanation())
                        .correctAnswer(question.getCorrectAnswer() != null ? List.of(question.getCorrectAnswer()) : Collections.emptyList())
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if (question.getCorrectAnswer() != null && answer.dataFilled() != null && 
                    question.getCorrectAnswer().equalsIgnoreCase(answer.dataFilled())) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
                answerAttempt.setDataFilled(answer.dataFilled());
                answerAttempt.setIsCorrect(result.isCorrect());
                if(result.isCorrect()) {
                    totalPoints += question.getPoint();
                }

            }
            if (question.getQuestionType() == QuestionType.MATCHING) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(answer.dataMatched() != null ? List.of(answer.dataMatched()) : Collections.emptyList())
                        .explanation(question.getExplanation())
                        .correctAnswer(question.getCorrectAnswer() != null ? List.of(question.getCorrectAnswer()) : Collections.emptyList())
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if (question.getCorrectAnswer() != null && answer.dataMatched() != null && 
                    question.getCorrectAnswer().equalsIgnoreCase(answer.dataMatched())) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
                answerAttempt.setDataMatched(answer.dataMatched());
                answerAttempt.setIsCorrect(result.isCorrect());
                if(result.isCorrect()) {
                    totalPoints += question.getPoint();
                }
            }
            if (question.getQuestionType() == QuestionType.DRAG_AND_DROP) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(answer.dragItemId() != null ? List.of(answer.dragItemId().toString()) : Collections.emptyList())
                        .explanation(question.getExplanation())
                        .correctAnswer(question.getDragItem() != null && question.getDragItem().getContent() != null ? 
                                      List.of(question.getDragItem().getContent()) : Collections.emptyList())
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if (question.getDragItem() != null && question.getDragItem().getDragItemId() != null && 
                    answer.dragItemId() != null && question.getDragItem().getDragItemId().equals(answer.dragItemId())) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
                answerAttempt.setDragItemId(answer.dragItemId());
                answerAttempt.setIsCorrect(result.isCorrect());
                if(result.isCorrect()) {
                    totalPoints += question.getPoint();
                }
            }
            answerAttemptRepository.save(answerAttempt);
        }

        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setStatus(Status.FINISHED);
        attempt.setTotalPoints(totalPoints);
        attempt.setDuration(answers.duration());
        attemptRepository.save(attempt);


        return SubmittedAttemptResponse.builder()
                .duration(answers.duration())
                .totalPoints(totalPoints)
                .resultSets(resultSets.stream().sorted(Comparator.comparing(SubmittedAttemptResponse.ResultSet::getQuestionIndex)).collect(Collectors.toList()))
                .build();
    }

    @Override
    public Page<UserAttemptResponse> getAttemptByUser(int page, int size,
                                                      List<Integer> ieltsTypeList,
                                                      List<Integer> statusList,
                                                      List<Integer> partNumberList,
                                                      String sortBy,
                                                      String sortDirection,
                                                      String title,
                                                      UUID listeningTaskId,
                                                      HttpServletRequest request) {
        String userId = helper.getUserIdFromToken(request);

        Pageable pageable = PageRequest.of(page, size);
        var spec = AttemptSpecification.byConditions(
                ieltsTypeList, statusList, partNumberList, sortBy, sortDirection, title, listeningTaskId, userId
        );

        Page<Attempt> pageResult = attemptRepository.findAll(spec, pageable);
        List<Attempt> attempts = pageResult.getContent();

        List<UserAttemptResponse> responses = attempts.stream().map(
                a -> UserAttemptResponse.builder()
                        .attemptId(a.getAttemptId())
                        .duration(a.getDuration())
                        .totalPoints(a.getTotalPoints())
                        .status(a.getStatus().ordinal())
                        .startAt(a.getCreatedAt())
                        .finishedAt(a.getFinishedAt())
                        .listeningTaskId(a.getListeningTask().getTaskId())
                        .title(a.getListeningTask().getTitle())
                        .build()
        ).toList();

        return new PageImpl<>(responses, pageable, pageResult.getTotalElements());
    }

    @Override
    public UserDataAttempt viewResult(UUID attemptId, HttpServletRequest request) throws JsonProcessingException {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        String userId = helper.getUserIdFromToken(request);
        if (!attempt.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        if (attempt.getStatus() != Status.FINISHED) {
            throw new AppException(
                    Constants.ErrorCodeMessage.ATTEMPT_NOT_FINISHED,
                    Constants.ErrorCode.ATTEMPT_NOT_FINISHED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        String rawJson = attempt.getVersion(); // Là một chuỗi chứa JSON

        JsonNode decodedNode = objectMapper.readTree(rawJson);

        AttemptVersion questionVersion = objectMapper.treeToValue(decodedNode, AttemptVersion.class);

        ListeningTask listeningTask = listeningTaskRepository.findById(questionVersion.getTaskId())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        Map<UUID, List<QuestionVersion>> groupMappingQuestion = questionVersion.getGroupMappingQuestion();
        Map<UUID, List<UUID>> groupMappingDragItem = questionVersion.getGroupMappingDragItem();

        Map<QuestionGroup, List<Question>> groupQuestions = new HashMap<>();
        Map<UUID, List<DragItem>> groupDragItems = new HashMap<>();
        Map<UUID, List<Choice>> questionChoice = new HashMap<>();
        groupMappingQuestion.forEach((key, value) ->{
            var groupOptional = questionGroupRepository.findById(key);
            if (groupOptional.isEmpty()) {
                return;
            }
            var group = groupOptional.get();
            List<Question> questions = new ArrayList<>();
            value.forEach(q -> {
                var questionOptional = questionRepository.findById(q.getQuestionId());
                if (questionOptional.isEmpty()) {
                    return;
                }
                var question = questionOptional.get();
                if(question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    List<Choice> choices = choiceRepository.findAllById(q.getChoiceMapping());
                    questionChoice.put(question.getQuestionId(), choices);
                }
                questions.add(question);
            });
            groupQuestions.put(group, questions);

            List<UUID> dragItemIds = groupMappingDragItem.get(group.getGroupId());
            if (dragItemIds != null) {
                List<DragItem> dragItems = dragItemRepository.findAllById(dragItemIds);
                groupDragItems.put(key, dragItems);
            } else {
                groupDragItems.put(key, Collections.emptyList());
            }
        });

        List<ListeningTaskGetAllResponse.QuestionGroupResponse> questionGroups = new ArrayList<>();
        groupQuestions.forEach((key, value) ->{

            ListeningTaskGetAllResponse.QuestionGroupResponse groupResponse = ListeningTaskGetAllResponse
                    .QuestionGroupResponse.builder()
                    .groupId(key.getGroupId())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .instruction(key.getInstruction())
                    .questions(value.stream().map(q -> {
                                String startTime = null;
                                String endTime = null;

                                try {
                                    if (q.getExplanation() != null) {
                                        JsonNode node = objectMapper.readTree(q.getExplanation());
                                        startTime = node.has("start_time") ? node.get("start_time").asText() : null;
                                        endTime = node.has("end_time") ? node.get("end_time").asText() : null;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                return ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.builder()
                                        .questionId(q.getQuestionId())
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .questionOrder(q.getQuestionOrder())
                                        .questionType(q.getQuestionType().ordinal())
                                        .point(q.getPoint())
                                        .numberOfCorrectAnswers(q.getNumberOfCorrectAnswers())
                                        .instructionForMatching(q.getInstructionForMatching())
                                        .zoneIndex(q.getZoneIndex())
                                        .correctAnswer(q.getCorrectAnswer() == null ? null : q.getCorrectAnswer())
                                        .correctAnswerForMatching(q.getCorrectAnswerForMatching() == null ? null : q.getCorrectAnswerForMatching())
                                        .dragItemId(q.getDragItem() == null ? null : q.getDragItem().getDragItemId())
                                        .choices(q.getQuestionType() == QuestionType.MULTIPLE_CHOICE ?
                                                questionChoice.get(q.getQuestionId()).stream()
                                                        .map(c -> ListeningTaskGetAllResponse.QuestionGroupResponse
                                                                .QuestionResponse.ChoiceResponse.builder()
                                                                .choiceId(c.getChoiceId())
                                                                .label(c.getLabel())
                                                                .choiceOrder(c.getChoiceOrder())
                                                                .content(c.getContent())
                                                                .isCorrect(c.isCorrect())
                                                                .build())
                                                        .sorted(Comparator.comparing(
                                                                ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse.ChoiceResponse::choiceOrder))
                                                        .toList()
                                                : null)
                                        .build();
                            })
                            .sorted(Comparator.comparing(
                                    ListeningTaskGetAllResponse.QuestionGroupResponse.QuestionResponse::questionOrder))
                            .toList())
                    .dragItems(groupDragItems.get(key.getGroupId()) != null ? 
                              groupDragItems.get(key.getGroupId()).stream().map(i ->
                            ListeningTaskGetAllResponse.QuestionGroupResponse.DragItemResponse.builder()
                                    .dragItemId(i.getDragItemId())
                                    .content(i.getContent())
                                    .build()
                    ).toList() : Collections.emptyList())
                    .build();

            questionGroups.add(groupResponse);
        });

        ListeningTaskGetAllResponse attemptResponse = ListeningTaskGetAllResponse.builder()
                .taskId(listeningTask.getTaskId())
                .ieltsType(listeningTask.getIeltsType().ordinal())
                .partNumber(listeningTask.getPartNumber().ordinal())
                .instruction(listeningTask.getInstruction())
                .title(listeningTask.getTitle())
                .audioFileId(listeningTask.getAudioFileId())
                .questionGroups(questionGroups.stream().sorted(
                        Comparator.comparing(ListeningTaskGetAllResponse.QuestionGroupResponse::sectionOrder)
                ).toList())
                .build();

        List<AnswerAttempt> answerAttempts = answerAttemptRepository.findByAttempt(attempt);
        List<UserDataAttempt.AnswerChoice> answerChoices = new ArrayList<>();
        for (AnswerAttempt answerAttempt : answerAttempts) {
            UserDataAttempt.AnswerChoice answerChoice = UserDataAttempt.AnswerChoice.builder()
                    .questionId(answerAttempt.getQuestion() != null ? answerAttempt.getQuestion().getQuestionId() : null)
                    .dragItemId(answerAttempt.getDragItemId() != null ? answerAttempt.getDragItemId() : null)
                    .filledTextAnswer(answerAttempt.getDataFilled() != null ? answerAttempt.getDataFilled() : null)
                    .matchedTextAnswer(answerAttempt.getDataMatched() != null ? answerAttempt.getDataMatched() : null)
                    .choiceIds(answerAttempt.getChoices() != null ? answerAttempt.getChoices() : Collections.emptyList())
                    .build();
            answerChoices.add(answerChoice);
        }

        return UserDataAttempt.builder()
                .attemptId(attempt.getAttemptId())
                .answers(answerChoices)
                .totalPoints(attempt.getTotalPoints())
                .duration(attempt.getDuration())
                .attemptResponse(attemptResponse)
                .build();
    }


    private SubmittedAttemptResponse.ResultSet scoreMultipleChoiceQuestion(SavedAnswersRequest answer, Question question) {
        List<Choice> correctAnswers;
        List<String> userAnswers = choiceRepository.getChoicesByIds(answer.choices());

        if (question.getIsOriginal()) {
            List<Choice> originalChoice = choiceRepository.getOriginalChoiceByOriginalQuestion(question.getQuestionId());
            correctAnswers = choiceRepository.getCurrentCorrectChoice(originalChoice.stream().map(
                    Choice::getChoiceId
            ).toList());
        } else {
            List<Choice> originalChoice = choiceRepository.getOriginalChoiceByOriginalQuestion(question.getParent().getQuestionId());
            correctAnswers = choiceRepository.getCurrentCorrectChoice(originalChoice.stream().map(
                    Choice::getChoiceId
            ).toList());
        }
        SubmittedAttemptResponse.ResultSet resultSet = SubmittedAttemptResponse.ResultSet.builder()
                .questionIndex(question.getQuestionOrder())
                .userAnswer(userAnswers)
                .correctAnswer(correctAnswers.stream().map(
                        Choice::getLabel
                ).toList())
                .explanation(question.getExplanation())
                .build();
        List<UUID> userChoice = answer.choices();
        List<String> correctLabel = new ArrayList<>();
        for (Choice correctAnswer : correctAnswers) {

            if (userChoice.contains(correctAnswer.getChoiceId())) {
                correctLabel.add(correctAnswer.getLabel());
            }
        }
        boolean isCorrect = false;
        int numberOfCorrect = 0;
        for (String userAnswer : userAnswers) {
            if (correctLabel.contains(userAnswer)) {
                numberOfCorrect++;
            }
        }
        isCorrect = numberOfCorrect == correctAnswers.size();
        resultSet.setCorrect(isCorrect);
        return resultSet;
    }

    private void checkAttemptSessionActivity(UUID attemptId, String userId) throws JsonProcessingException {
        String attemptSessionKey = "attempt_session:" + attemptId.toString();
        String sessionActivityKey = "session_activity:" + attemptId.toString();
        
        // Check if attempt session exists in Redis
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = redisService.getValue(attemptSessionKey, Map.class);
        if (sessionData != null) {
            String sessionUserId = (String) sessionData.get("userId");
            String sessionId = (String) sessionData.get("sessionId");
            
            // If it's the same user, check if session is still active
            if (userId.equals(sessionUserId)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> activityData = redisService.getValue(sessionActivityKey, Map.class);
                if (activityData != null) {
                    Long lastActivity = (Long) activityData.get("lastActivity");
                    if (lastActivity != null) {
                        long currentTime = System.currentTimeMillis();
                        long timeDiff = currentTime - lastActivity;
                        
                        // If last activity was within 5 minutes, consider it active
                        if (timeDiff < 300000) { // 5 minutes in milliseconds
                            throw new AppException(
                                    Constants.ErrorCodeMessage.ATTEMPT_SESSION_ACTIVE,
                                    Constants.ErrorCode.ATTEMPT_SESSION_ACTIVE,
                                    HttpStatus.CONFLICT.value()
                            );
                        }
                    }
                }
            } else {
                // Different user is using this attempt
                throw new AppException(
                        Constants.ErrorCodeMessage.ATTEMPT_IN_USE,
                        Constants.ErrorCode.ATTEMPT_IN_USE,
                        HttpStatus.CONFLICT.value()
                );
            }
        }
    }
}
