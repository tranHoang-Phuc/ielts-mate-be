package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.model.json.AttemptVersion;
import com.fptu.sep490.readingservice.model.json.QuestionVersion;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.repository.specification.AttemptSpecification;
import com.fptu.sep490.readingservice.service.AttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemResponse;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.xml.transform.Result;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttemptServiceImpl implements AttemptService {
    ReadingPassageRepository readingPassageRepository;
    AttemptRepository attemptRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionRepository questionRepository;
    DragItemRepository dragItemRepository;
    ChoiceRepository choiceRepository;
    AnswerAttemptRepository answerAttemptRepository;
    ObjectMapper objectMapper;
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    RedisService redisService;
    Helper helper;

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
    public AttemptResponse createAttempt(String passageId, HttpServletRequest request) throws JsonProcessingException {
        String userId = helper.getUserIdFromToken(request);
        ReadingPassage passage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        ReadingPassage currentVersion = readingPassageRepository.findCurrentVersionById(passage.getPassageId())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));


        if (currentVersion.getPassageStatus() == null || currentVersion.getPassageStatus() != Status.PUBLISHED) {
            throw new AppException(
                    Constants.ErrorCodeMessage.PASSAGE_NOT_ACTIVE,
                    Constants.ErrorCode.PASSAGE_NOT_ACTIVE,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        AttemptVersion attemptVersion = AttemptVersion.builder()
                .readingPassageId(currentVersion.getPassageId())
                .build();
        Map<UUID, List<QuestionVersion>> questionVersions = new HashMap<>();
        Map<UUID, List<UUID>> groupMapDragItem = new HashMap<>();
        List<QuestionGroup> originalVersionGroupQuestion = questionGroupRepository
                .findOriginalVersionByTaskId(passage.getPassageId());

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
                .readingPassage(passage)
                .version(objectMapper.writeValueAsString(attemptVersion))
                .build();
        attempt = attemptRepository.save(attempt);

        List<AttemptResponse.QuestionGroupAttemptResponse> groups = new ArrayList<>();
        currentGroupMapCurrentDragItem.forEach((key, value)-> {
            List<UpdatedQuestionResponse.DragItemResponse> dragItems =
                    value.stream()
                            .map(item -> UpdatedQuestionResponse.DragItemResponse
                                    .builder()
                                    .dragItemId(item.getDragItemId().toString())
                                    .content(item.getContent())
                                    .build()
                            )
                            .toList();
            List<Question> questions = currentGroupIdQuestions.get(key.getGroupId());
            Map<UUID, List<Choice>> questionMapChoices = currentGroupIdMapQuestionIdMapCurrentChoice.get(key.getGroupId());

            List<AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse> questionListResponse = new ArrayList<>();
            questions.forEach(question -> {
                List<Choice> choices = questionMapChoices.get(question.getQuestionId());
                AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse questionResponse =
                        AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.builder()
                        .questionId(question.getQuestionId())
                        .questionOrder(question.getQuestionOrder())
                        .questionType(question.getQuestionType().ordinal())
                        .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                        .blankIndex(question.getBlankIndex())
                        .instructionForChoice(question.getInstructionForChoice())
                        .instructionForMatching(question.getInstructionForMatching())
                        .zoneIndex(question.getZoneIndex())
                        .choices(
                                choices.stream().map(c ->
                                        AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse.builder()
                                                .choiceId(c.getChoiceId())
                                                .label(c.getLabel())
                                                .content(c.getContent())
                                                .choiceOrder(question.getQuestionOrder())
                                                .build()
                                ).sorted(Comparator.comparing(AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse::choiceOrder)).toList()
                        )
                        .build();
                questionListResponse.add(questionResponse);
            });

            AttemptResponse.QuestionGroupAttemptResponse questionGroupResponse = AttemptResponse.QuestionGroupAttemptResponse.builder()
                    .questionGroupId(key.getGroupId())
                    .sectionLabel(key.getSectionLabel())
                    .sectionOrder(key.getSectionOrder())
                    .instruction(key.getInstruction())
                    .dragItems(dragItems)
                    .questions(questionListResponse.stream().sorted(
                            Comparator.comparing(AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse::questionOrder)
                    ).toList())
                    .build();

            groups.add(questionGroupResponse);
        });
        return AttemptResponse.builder()
                .attemptId(attempt.getAttemptId())
                .readingPassageId(currentVersion.getPassageId())
                .ieltsType(currentVersion.getIeltsType().ordinal())
                .partNumber(currentVersion.getPartNumber().ordinal())
                .title(currentVersion.getTitle())
                .instruction(currentVersion.getInstruction())
                .content(currentVersion.getContent())
                .questionGroups(groups)
                .build();
    }

    @Override
    @Transactional
    public void saveAttempt(String attemptId,
                            HttpServletRequest request,
                            SavedAnswersRequestList answers) {
        Attempt attempt = attemptRepository.findById(UUID.fromString(attemptId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        String userId = getUserIdFromToken(request);
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

        attempt.setDuration(attempt.getDuration());

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
                        Constants.ErrorCodeMessage.ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        String userId = getUserIdFromToken(request);
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

        String rawJson = attempt.getVersion(); // Là một chuỗi chứa JSON

        JsonNode decodedNode = objectMapper.readTree(rawJson);

        AttemptVersion questionVersion = objectMapper.treeToValue(decodedNode, AttemptVersion.class);

        ReadingPassage passage = readingPassageRepository.findById(questionVersion.getReadingPassageId())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        Map<UUID, List<QuestionVersion>> groupMappingQuestion = questionVersion.getGroupMappingQuestion();
        Map<UUID, List<UUID>> groupMappingDragItem = questionVersion.getGroupMappingDragItem();

        Map<QuestionGroup, List<Question>> groupQuestions = new HashMap<>();
        Map<UUID, List<DragItem>> groupDragItems = new HashMap<>();
        Map<UUID, List<Choice>> questionChoice = new HashMap<>();
        groupMappingQuestion.forEach((key, value) ->{
            var group = questionGroupRepository.findById(key).get();
            List<Question> questions = new ArrayList<>();
            value.forEach(q -> {
                var question = questionRepository.findById(q.getQuestionId()).get();
                if(question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    List<Choice> choices = choiceRepository.findAllById(q.getChoiceMapping());
                    questionChoice.put(question.getQuestionId(), choices);
                }
                questions.add(question);
            });
            groupQuestions.put(group, questions);

            List<DragItem> dragItems = dragItemRepository.findAllById(groupMappingDragItem.get(group.getGroupId()));
            groupDragItems.put(key, dragItems);
        });

        List<ReadingPassageGetAllResponse.QuestionGroupResponse> questionGroups = new ArrayList<>();
        groupQuestions.forEach((key, value) ->{

            ReadingPassageGetAllResponse.QuestionGroupResponse groupResponse = ReadingPassageGetAllResponse
                    .QuestionGroupResponse.builder()
                    .groupId(key.getGroupId())
                    .sectionOrder(key.getSectionOrder())
                    .sectionLabel(key.getSectionLabel())
                    .instruction(key.getInstruction())
                    .questions(value.stream().map(q ->
                                    ReadingPassageGetAllResponse.QuestionGroupResponse.QuestionResponse.builder()
                                            .questionId(q.getQuestionId())
                                            .questionOrder(q.getQuestionOrder())
                                            .questionType(q.getQuestionType().ordinal())
                                            .point(q.getPoint())
                                            .numberOfCorrectAnswers(q.getNumberOfCorrectAnswers())
                                            .instructionForMatching(q.getInstructionForMatching())
                                            .zoneIndex(q.getZoneIndex())
                                            .choices(q.getQuestionType() == QuestionType.MULTIPLE_CHOICE ?
                                                    questionChoice.get(q.getQuestionId()).stream()
                                                            .map(c -> ReadingPassageGetAllResponse.QuestionGroupResponse
                                                                    .QuestionResponse.ChoiceResponse.builder()
                                                                    .choiceId(c.getChoiceId())
                                                                    .label(c.getLabel())
                                                                    .choiceOrder(c.getChoiceOrder())
                                                                    .content(c.getContent())
                                                                    .build()).sorted(Comparator.comparing(ReadingPassageGetAllResponse.QuestionGroupResponse.QuestionResponse.ChoiceResponse::choiceOrder)).toList() : null)
                                            .build()
                            )
                            .sorted(Comparator.comparing(ReadingPassageGetAllResponse.QuestionGroupResponse.QuestionResponse::questionOrder)).toList())
                    .dragItems(groupDragItems.get(key.getGroupId()).stream().map(i ->
                            ReadingPassageGetAllResponse.QuestionGroupResponse.DragItemResponse.builder()
                                    .dragItemId(i.getDragItemId())
                                    .content(i.getContent())
                                    .build()
                    ).toList())
                    .build();
            questionGroups.add(groupResponse);
        });

        ReadingPassageGetAllResponse attemptResponse = ReadingPassageGetAllResponse.builder()
                .passageId(passage.getPassageId())
                .ieltsType(passage.getIeltsType().ordinal())
                .partNumber(passage.getPartNumber().ordinal())
                .instruction(passage.getInstruction())
                .title(passage.getTitle())
                .content(passage.getContent())
                .questionGroups(questionGroups.stream().sorted(
                        Comparator.comparing(ReadingPassageGetAllResponse.QuestionGroupResponse::sectionOrder)
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
                .duration(attempt.getDuration())
                .attemptResponse(attemptResponse)
                .build();
    }

    @Override
    @Transactional
    public SubmittedAttemptResponse submitAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers) throws JsonProcessingException {
        Attempt attempt = attemptRepository.findById(UUID.fromString(attemptId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        String userId = getUserIdFromToken(request);
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
            }
            if (question.getQuestionType() == QuestionType.FILL_IN_THE_BLANKS) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(List.of(answer.dataFilled()))
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getCorrectAnswer()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if (question.getCorrectAnswer().equalsIgnoreCase(answer.dataFilled())) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
                answerAttempt.setDataFilled(answer.dataFilled());
                answerAttempt.setIsCorrect(result.isCorrect());

            }
            if (question.getQuestionType() == QuestionType.MATCHING) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(List.of(answer.dataMatched()))
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getCorrectAnswerForMatching()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if (question.getCorrectAnswerForMatching().equalsIgnoreCase(answer.dataMatched())) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
                answerAttempt.setDataMatched(answer.dataMatched());
                answerAttempt.setIsCorrect(result.isCorrect());
            }
            if (question.getQuestionType() == QuestionType.DRAG_AND_DROP) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(List.of(dragItemRepository.findById(answer.dragItemId()).get().getContent()))
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getDragItem().getContent()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if (question.getDragItem().getDragItemId().equals(answer.dragItemId())) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
                answerAttempt.setDragItemId(answer.dragItemId());
                answerAttempt.setIsCorrect(result.isCorrect());
            }
            answerAttemptRepository.save(answerAttempt);
        }

        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setStatus(Status.FINISHED);
        attempt.setDuration(answers.duration());
        attemptRepository.save(attempt);


        return SubmittedAttemptResponse.builder()
                .duration(answers.duration())
                .resultSets(resultSets.stream().sorted(Comparator.comparing(SubmittedAttemptResponse.ResultSet::getQuestionIndex)).collect(Collectors.toList()))
                .build();
    }

    @Override
    public Page<UserAttemptResponse> getAttemptByUser(int page, int size, List<Integer> ieltsTypeList, List<Integer> statusList, List<Integer> partNumberList, String sortBy, String sortDirection, String title, UUID passageId, HttpServletRequest request) {
        String userId = helper.getUserIdFromToken(request);

        Pageable pageable = PageRequest.of(page, size);
        var spec = AttemptSpecification.byConditions(
                ieltsTypeList, statusList, partNumberList, sortBy, sortDirection, title, passageId, userId
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
                        .readingPassageId(a.getReadingPassage().getPassageId())
                        .title(a.getReadingPassage().getTitle())
                        .build()
        ).toList();

        return new PageImpl<>(responses, pageable, pageResult.getTotalElements());


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

    private Map<UUID, QuestionAttempt> getCorrectAnswer(List<Question> questions) {
        Map<UUID, QuestionAttempt> correctAnswers = new HashMap<>();
        for (Question q : questions) {

            QuestionAttempt questionAttempt = QuestionAttempt.builder()
                    .questionType(q.getQuestionType().ordinal())
                    .numberOfCorrectAnswers(q.getNumberOfCorrectAnswers())
                    .build();

            switch (q.getQuestionType()) {
                case MULTIPLE_CHOICE -> {
                    List<String> correctIdStrings = choiceRepository.findCorrectChoiceByQuestion(q).stream()
                            .map(Choice::getChoiceId)
                            .map(UUID::toString)
                            .collect(Collectors.toList());
                    questionAttempt.setCorrectAnswer(correctIdStrings);
                    correctAnswers.put(q.getQuestionId(), questionAttempt);
                }

                case FILL_IN_THE_BLANKS -> {
                    questionAttempt.setCorrectAnswer(Collections.singletonList(q.getCorrectAnswer()));
                    correctAnswers.put(q.getQuestionId(), questionAttempt);
                }

                case MATCHING -> {
                    String correctAnswerForMatching = q.getCorrectAnswerForMatching();
                    questionAttempt.setCorrectAnswer(Collections.singletonList(correctAnswerForMatching));
                    correctAnswers.put(q.getQuestionId(), questionAttempt);
                }

                case DRAG_AND_DROP -> {
                    DragItem dragItems = dragItemRepository.findByQuestion(q).orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.DRAG_ITEM_NOT_FOUND,
                            Constants.ErrorCode.DRAG_ITEM_NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));

                    questionAttempt.setCorrectAnswer(Collections.singletonList(dragItems.getDragItemId().toString()));
                    correctAnswers.put(q.getQuestionId(), questionAttempt);
                }
            }

        }
        return correctAnswers;
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
