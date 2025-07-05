package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.embedded.AnswerAttemptId;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.model.json.AttemptVersion;
import com.fptu.sep490.listeningservice.model.json.QuestionVersion;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.service.AttemptService;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

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

            List<AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse> questionListResponse = new ArrayList<>();

            questions.forEach(question -> {
                List<Choice> choices = questionMapChoices.get(question.getQuestionId());
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
                                choices.stream().map(c ->
                                                AttemptResponse.QuestionGroupAttemptResponse.QuestionAttemptResponse.ChoiceAttemptResponse.builder()
                                                        .choiceId(c.getChoiceId())
                                                        .label(c.getLabel())
                                                        .content(c.getContent())
                                                        .choiceOrder(question.getQuestionOrder())
                                                        .build()
                                        ).sorted(Comparator.comparing(AttemptResponse.QuestionGroupAttemptResponse.
                                        QuestionAttemptResponse.ChoiceAttemptResponse::choiceOrder)).toList()
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

}
