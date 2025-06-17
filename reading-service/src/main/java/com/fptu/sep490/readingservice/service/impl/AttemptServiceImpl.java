package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.embedded.AnswerAttemptId;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.service.AttemptService;
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
    public PassageAttemptResponse createAttempt(String passageId, HttpServletRequest request) throws JsonProcessingException {
        ReadingPassage passage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (passage.getPassageStatus() == null || passage.getPassageStatus() != Status.PUBLISHED) {
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

        Map<UUID, List<Question>> questionsByGroup = new HashMap<>();
        Map<UUID, List<Choice>> choicesByQuestion = new HashMap<>();
        Map<UUID, List<DragItem>> dragItemsByGroup = new HashMap<>();

        for (QuestionGroup group : questionGroups) {
            List<Question> questions = questionRepository.findAllByQuestionGroupOrderByQuestionOrderAsc(group);
            if (questions.isEmpty()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                );
            }
            questionsByGroup.put(group.getGroupId(), questions);

            for (Question question : questions) {
                if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    List<Choice> choices = choiceRepository.findByQuestionAndIsDeleted(question,false);
                    choicesByQuestion.put(question.getQuestionId(), choices);
                }
                dragItemRepository.findByQuestion(question).ifPresent(di -> {
                    dragItemsByGroup.computeIfAbsent(group.getGroupId(), k -> new ArrayList<>()).add(di);
                });
            }
        }

        String userId = getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
        UserProfileResponse userProfile = getUserProfileById(userId);

        Attempt attempt = Attempt.builder()
                .createdBy(userId)
                .readingPassage(passage)
                .status(Status.DRAFT)
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();
        attempt = attemptRepository.save(attempt);

        List<PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse> groupResponses =
                questionGroups.stream().map(group -> {
                    UUID groupId = group.getGroupId();

                    List<Question> questions = questionsByGroup.get(groupId);
                    List<PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse> questionResponses =
                            questions.stream().map(question -> {
                                List<UpdatedQuestionResponse.ChoiceResponse> choiceResponses =
                                        choicesByQuestion.getOrDefault(question.getQuestionId(), Collections.emptyList())
                                                .stream()
                                                .map(choice -> UpdatedQuestionResponse.ChoiceResponse.builder()
                                                        .choiceId(choice.getChoiceId().toString())
                                                        .label(choice.getLabel())
                                                        .choiceOrder(choice.getChoiceOrder())
                                                        .content(choice.getContent())
                                                        .build())
                                                .toList();

                                return PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.QuestionResponse.builder()
                                        .questionId(question.getQuestionId().toString())
                                        .questionOrder(question.getQuestionOrder())
                                        .questionType(question.getQuestionType().ordinal())
                                        .numberOfCorrectAnswers(question.getNumberOfCorrectAnswers())
                                        .instructionForChoice(question.getInstructionForChoice())
                                        .choices(choiceResponses)
                                        .blankIndex(question.getBlankIndex())
                                        .correctAnswer(question.getCorrectAnswer())
                                        .instructionForMatching(question.getInstructionForMatching())
                                        .correctAnswerForMatching(question.getCorrectAnswerForMatching())
                                        .zoneIndex(question.getZoneIndex())
                                        .build();
                            }).toList();

                    List<UpdatedQuestionResponse.DragItemResponse> dragItemResponses =
                            dragItemsByGroup.getOrDefault(groupId, Collections.emptyList())
                                    .stream()
                                    .map(di -> UpdatedQuestionResponse.DragItemResponse.builder()
                                            .dragItemId(di.getDragItemId().toString())
                                            .content(di.getContent())
                                            .build())
                                    .toList();

                    return PassageAttemptResponse.ReadingPassageResponse.QuestionGroupResponse.builder()
                            .groupId(groupId.toString())
                            .sectionLabel(group.getSectionLabel())
                            .sectionOrder(group.getSectionOrder())
                            .instruction(group.getInstruction())
                            .dragItems(dragItemResponses)
                            .questions(questionResponses)
                            .build();
                }).toList();

        PassageAttemptResponse.ReadingPassageResponse readingPassageResp =
                PassageAttemptResponse.ReadingPassageResponse.builder()
                        .passageId(passageId)
                        .instruction(passage.getInstruction())
                        .title(passage.getTitle())
                        .content(passage.getContent())
                        .partNumber(passage.getPartNumber().ordinal())
                        .questionGroups(groupResponses)
                        .build();

        return PassageAttemptResponse.builder()
                .attemptId(attempt.getAttemptId().toString())
                .createdBy(UserInformationResponse.builder()
                        .userId(userId)
                        .firstName(userProfile.firstName())
                        .lastName(userProfile.lastName())
                        .build())
                .createdAt(attempt.getCreatedAt().toString())
                .readingPassage(readingPassageResp)
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
