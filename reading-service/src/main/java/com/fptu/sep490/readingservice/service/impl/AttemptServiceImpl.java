package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.model.json.AttemptVersion;
import com.fptu.sep490.readingservice.model.json.QuestionVersion;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.service.AttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.request.SavedAnswersRequestList;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByReadingPassageByPassageId(passage.getPassageId());
        if (questionGroups.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        Map<QuestionGroup, Map<Question, List<Choice>>> currentVersionChoicesByGroup = new HashMap<>();
        for (QuestionGroup group : questionGroups) {
            List<Question> currentVersionQuestions = questionRepository.findCurrentVersionByGroup(group.getGroupId());
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

        AttemptVersion version = AttemptVersion.builder()
                .readingPassageId(passage.getPassageId())
                .build();
        Map<UUID, List<QuestionVersion>> questionVersions = new HashMap<>();



        currentVersionChoicesByGroup.forEach((group, questionChoices) -> {
            List<QuestionVersion> versions = new ArrayList<>();
            for (Map.Entry<Question, List<Choice>> entry : questionChoices.entrySet()) {
                Question question = entry.getKey();
                List<Choice> choices = entry.getValue();

                QuestionVersion questionVersion = QuestionVersion.builder()
                        .questionId(question.getQuestionId())
                        .choiceMapping(choices.stream().map(Choice::getChoiceId).collect(Collectors.toList()))
                        .build();
                versions.add(questionVersion);
                questionVersions.put(group.getGroupId(), versions);
            }
        });
        version.setGroupMappingQuestion(questionVersions);

        Attempt attempt = Attempt.builder()
                .readingPassage(passage)
                .createdBy(getUserIdFromToken(request))
                .status(Status.DRAFT)
                .version(objectMapper.writeValueAsString(version))
                .duration(0L)
                .build();
        attempt = attemptRepository.save(attempt);
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
                                    questionResponses
                            );
                        })
                        .collect(Collectors.toList());

        return new AttemptResponse(
                attempt.getAttemptId(),
                passage.getPassageId(),
                passage.getIeltsType().ordinal(),
                passage.getPartNumber().ordinal(),
                passage.getInstruction(),
                passage.getContent(),
                questionGroupResponses
        );
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
        }
    }

    @Override
    public UserDataAttempt loadAttempt(String attemptId, HttpServletRequest request) {
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
                .build();
    }

    @Override
    public SubmittedAttemptResponse submitAttempt(String attemptId, HttpServletRequest request, SavedAnswersRequestList answers) {


        Attempt attempt = attemptRepository.findById(UUID.fromString(attemptId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        List<Question> questions = questionRepository.findAllByReadingPassage(attempt.getReadingPassage());

        Map<UUID, QuestionAttempt> correctAnswers = getCorrectAnswer(questions);
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
        attempt.setStatus(Status.FINISHED);
        attempt.setDuration(answers.duration());

        attempt.setFinishedAt(LocalDateTime.now());
        return null;
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
