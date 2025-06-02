package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.repository.DragItemRepository;
import com.fptu.sep490.readingservice.repository.QuestionGroupRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.service.QuestionService;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UserProfileResponse;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    QuestionRepository questionRepository;
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    RedisService redisService;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;

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
    public List<QuestionCreationResponse> createQuestions(
            List<QuestionCreationRequest> questionCreationResponses, HttpServletRequest request) throws JsonProcessingException {
        List<QuestionCreationResponse> questionCreationResponseList = new ArrayList<>();
        if (questionCreationResponses == null || questionCreationResponses.isEmpty()) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_LIST_EMPTY,
                    Constants.ErrorCode.QUESTION_LIST_EMPTY, HttpStatus.BAD_REQUEST.value());
        }

        int correctAnswersCount = 0;
        for(QuestionCreationRequest question : questionCreationResponses){
            if (question.questionType() < 0 || question.questionType() >= QuestionType.values().length) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_QUESTION_TYPE,
                        Constants.ErrorCode.INVALID_QUESTION_TYPE, HttpStatus.BAD_REQUEST.value());
            }
            List<QuestionCategory> categories = question.questionCategories().stream()
                    .map(QuestionCategory::valueOf)
                    .toList();
            QuestionGroup questionGroup = questionGroupRepository.findById(UUID.fromString(question.questionGroupId()))
                    .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                            Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
            if(question.questionType() == QuestionType.MULTIPLE_CHOICE.ordinal()) {
                if (question.choices() == null || question.choices().isEmpty()) {
                    throw new AppException(Constants.ErrorCodeMessage.CHOICES_LIST_EMPTY,
                            Constants.ErrorCode.CHOICES_LIST_EMPTY, HttpStatus.BAD_REQUEST.value());
                }
                if (question.numberOfCorrectAnswers() < 1) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                            Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
                }


                Question savedQuestion = Question.builder()
                        .questionType(QuestionType.MULTIPLE_CHOICE)
                        .questionOrder(question.questionOrder())
                        .categories(Set.copyOf(categories))
                        .explanation(question.explanation())
                        .numberOfCorrectAnswers(question.numberOfCorrectAnswers())
                        .questionGroup(questionGroup)
                        .instructionForChoice(question.instructionForChoice())
                        .build();
                for(QuestionCreationRequest.ChoiceRequest choice: question.choices()){
                    if(choice.isCorrect()) {
                        correctAnswersCount++;
                    }
                    Choice savedChoice = Choice.builder()
                            .label(choice.label())
                            .content(choice.content())
                            .choiceOrder(choice.choiceOrder())
                            .isCorrect(choice.isCorrect())
                            .build();
                    savedQuestion.getChoices().add(savedChoice);
                }
                if(question.numberOfCorrectAnswers() != correctAnswersCount) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                            Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
                }

                Question saved = questionRepository.save(savedQuestion);
                String userId = getUserIdFromToken(request);

                saved.setCreatedBy(userId);
                saved.setUpdatedBy(userId);
                UserProfileResponse userProfile = getUserProfileById(userId);
                questionCreationResponseList.add(
                        QuestionCreationResponse.builder()
                                .questionId(saved.getQuestionId().toString())
                                .questionOrder(saved.getQuestionOrder())
                                .point(saved.getPoint())
                                .questionType(saved.getQuestionType().ordinal())
                                .questionCategories(saved.getCategories().stream()
                                        .map(QuestionCategory::name)
                                        .toList())
                                .explanation(saved.getExplanation())
                                .questionGroupId(saved.getQuestionGroup().getGroupId().toString())
                                .numberOfCorrectAnswers(saved.getNumberOfCorrectAnswers())
                                .instructionForChoice(saved.getInstructionForChoice())
                                .choices(saved.getChoices().stream()
                                        .map(c -> new QuestionCreationResponse.ChoiceResponse(
                                                c.getChoiceId().toString(),
                                                c.getLabel(),
                                                c.getChoiceOrder(),
                                                c.getContent(),
                                                c.isCorrect()))
                                        .toList())
                                .createdBy(UserInformationResponse.builder()
                                        .userId(userProfile.id())
                                        .firstName(userProfile.firstName())
                                        .lastName(userProfile.lastName())
                                        .email(userProfile.email())
                                        .build())
                                .updatedBy(UserInformationResponse.builder()
                                        .userId(userProfile.id())
                                        .firstName(userProfile.firstName())
                                        .lastName(userProfile.lastName())
                                        .email(userProfile.email())
                                        .build())
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .build()
                );

            }
            else if (question.questionType() == QuestionType.FILL_IN_THE_BLANKS.ordinal()) {
                if(question.numberOfCorrectAnswers() != 0) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                            Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
                }
                if (question.blankIndex() == null || question.blankIndex() < 0) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_BLANK_INDEX,
                            Constants.ErrorCode.INVALID_BLANK_INDEX, HttpStatus.BAD_REQUEST.value());
                }
                Question savedQuestion = Question.builder()
                        .questionType(QuestionType.FILL_IN_THE_BLANKS)
                        .point(question.point())
                        .questionOrder(question.questionOrder())
                        .blankIndex(question.blankIndex())
                        .correctAnswer(question.correctAnswer())
                        .categories(Set.copyOf(categories))
                        .explanation(question.explanation())
                        .questionGroup(questionGroup)
                        .build();

                Question saved = questionRepository.save(savedQuestion);
                String userId = getUserIdFromToken(request);
                saved.setCreatedBy(userId);
                saved.setUpdatedBy(userId);
                UserProfileResponse userProfile = getUserProfileById(userId);
                questionCreationResponseList.add(
                        QuestionCreationResponse.builder()
                                .questionId(saved.getQuestionId().toString())
                                .questionOrder(saved.getQuestionOrder())
                                .point(saved.getPoint())
                                .questionType(saved.getQuestionType().ordinal())
                                .questionCategories(saved.getCategories().stream()
                                        .map(QuestionCategory::name)
                                        .toList())
                                .explanation(saved.getExplanation())
                                .questionGroupId(saved.getQuestionGroup().getGroupId().toString())
                                .numberOfCorrectAnswers(saved.getNumberOfCorrectAnswers())
                                .instructionForChoice(saved.getInstructionForChoice())
                                .blankIndex(saved.getBlankIndex())
                                .correctAnswer(saved.getCorrectAnswer())
                                .createdBy(UserInformationResponse.builder()
                                        .userId(userProfile.id())
                                        .firstName(userProfile.firstName())
                                        .lastName(userProfile.lastName())
                                        .email(userProfile.email())
                                        .build())
                                .updatedBy(UserInformationResponse.builder()
                                        .userId(userProfile.id())
                                        .firstName(userProfile.firstName())
                                        .lastName(userProfile.lastName())
                                        .email(userProfile.email())
                                        .build())
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .build()
                );
            }
            else if (question.questionType() == QuestionType.MATCHING.ordinal()) {
                if(question.numberOfCorrectAnswers() != 0) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                            Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
                }
                if (question.instructionForMatching() == null || question.instructionForMatching().isEmpty()) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                            Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
                }
                if (question.correctAnswerForMatching() == null || question.correctAnswerForMatching().isEmpty()) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                            Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
                }
                Question savedQuestion = Question.builder()
                        .questionType(QuestionType.MATCHING)
                        .point(question.point())
                        .questionOrder(question.questionOrder())
                        .instructionForMatching(question.instructionForMatching())
                        .correctAnswerForMatching(question.correctAnswerForMatching())
                        .categories(Set.copyOf(categories))
                        .explanation(question.explanation())
                        .questionGroup(questionGroup)
                        .build();

                Question saved = questionRepository.save(savedQuestion);
                String userId = getUserIdFromToken(request);
                saved.setCreatedBy(userId);
                saved.setUpdatedBy(userId);
                UserProfileResponse userProfile = getUserProfileById(userId);
                questionCreationResponseList.add(
                        QuestionCreationResponse.builder()
                                .questionId(saved.getQuestionId().toString())
                                .questionOrder(saved.getQuestionOrder())
                                .point(saved.getPoint())
                                .questionType(saved.getQuestionType().ordinal())
                                .questionCategories(saved.getCategories().stream()
                                        .map(QuestionCategory::name)
                                        .toList())
                                .explanation(saved.getExplanation())
                                .questionGroupId(saved.getQuestionGroup().getGroupId().toString())
                                .numberOfCorrectAnswers(saved.getNumberOfCorrectAnswers())
                                .instructionForChoice(saved.getInstructionForChoice())
                                .instructionForMatching(saved.getInstructionForMatching())
                                .correctAnswerForMatching(saved.getCorrectAnswerForMatching())
                                .createdBy(UserInformationResponse.builder()
                                        .userId(userProfile.id())
                                        .firstName(userProfile.firstName())
                                        .lastName(userProfile.lastName())
                                        .email(userProfile.email())
                                        .build())
                                .updatedBy(UserInformationResponse.builder()
                                        .userId(userProfile.id())
                                        .firstName(userProfile.firstName())
                                        .lastName(userProfile.lastName())
                                        .email(userProfile.email())
                                        .build())
                                .build());
            } else {
//                var items = dragItemRepository.findDragItemByDragItemId(UUID.fromString(question.dragItemId())
//                        .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
//                                Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value()));

            }

        }
        return questionCreationResponseList;
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
        return keyCloakUserClient.getUserById(realm, "Bearer " + clientToken, userId);

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
