package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.Choice;
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
import com.fptu.sep490.readingservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedQuestionRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
            List<QuestionCreationRequest> questionCreationRequest, HttpServletRequest request) throws JsonProcessingException {
        List<QuestionCreationResponse> questionCreationResponseList = new ArrayList<>();
        if (questionCreationRequest == null || questionCreationRequest.isEmpty()) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_LIST_EMPTY,
                    Constants.ErrorCode.QUESTION_LIST_EMPTY, HttpStatus.BAD_REQUEST.value());
        }

        int correctAnswersCount = 0;
        for(QuestionCreationRequest question : questionCreationRequest){
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
                        .isOriginal(true)
                        .isCurrent(true)
                        .version(1)
                        .isDeleted(false)
                        .point(question.point())
                        .instructionForChoice(question.instructionForChoice())
                        .build();
                List<Choice> choices = new ArrayList<>();
                for(QuestionCreationRequest.ChoiceRequest choice: question.choices()){
                    if(choice.isCorrect()) {
                        correctAnswersCount++;
                    }
                    Choice savedChoice = Choice.builder()
                            .label(choice.label())
                            .content(choice.content())
                            .choiceOrder(choice.choiceOrder())
                            .isCorrect(choice.isCorrect())
                            .isOriginal(true)
                            .isCurrent(true)
                            .version(1)
                            .isDeleted(false)
                            .question(savedQuestion)
                            .build();
                    choices.add(savedChoice);

                }
                savedQuestion.setChoices(choices);
                if(question.numberOfCorrectAnswers() != correctAnswersCount) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                            Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
                }
                String userId = getUserIdFromToken(request);
                savedQuestion.setCreatedBy(userId);
                savedQuestion.setUpdatedBy(userId);

                Question saved = questionRepository.save(savedQuestion);
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
                        .isOriginal(true)
                        .isCurrent(true)
                        .version(1)
                        .isDeleted(false)
                        .explanation(question.explanation())
                        .questionGroup(questionGroup)
                        .build();
                String userId = getUserIdFromToken(request);
                savedQuestion.setCreatedBy(userId);
                savedQuestion.setUpdatedBy(userId);
                Question saved = questionRepository.save(savedQuestion);
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
                        .isOriginal(true)
                        .isCurrent(true)
                        .isDeleted(false)
                        .version(1)
                        .explanation(question.explanation())
                        .questionGroup(questionGroup)
                        .build();
                String userId = getUserIdFromToken(request);
                savedQuestion.setCreatedBy(userId);
                savedQuestion.setUpdatedBy(userId);
                Question saved = questionRepository.save(savedQuestion);
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
                var dragItem = dragItemRepository.findDragItemByDragItemId(UUID.fromString(question.dragItemId()))
                        .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                                Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value()));


                Question savedQuestion = Question.builder()
                        .questionType(QuestionType.DRAG_AND_DROP)
                        .point(question.point())
                        .questionOrder(question.questionOrder())
                        .zoneIndex(question.zoneIndex())
                        .dragItem(dragItem)
                        .categories(Set.copyOf(categories))
                        .explanation(question.explanation())
                        .numberOfCorrectAnswers(question.numberOfCorrectAnswers())
                        .isOriginal(true)
                        .isCurrent(true)
                        .isDeleted(false)
                        .version(1)
                        .zoneIndex(question.zoneIndex())
                        .questionGroup(questionGroup)
                        .build();
                String userId = getUserIdFromToken(request);
                savedQuestion.setCreatedBy(userId);
                savedQuestion.setUpdatedBy(userId);
                Question saved = questionRepository.save(savedQuestion);
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
                                .zoneIndex(saved.getZoneIndex())
                                .dragItems(List.of(
                                      QuestionCreationResponse.DragItemResponse.builder()
                                              .dragItemId(dragItem.getDragItemId().toString())
                                              .content(dragItem.getContent())
                                              .build()
                                ))
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

        }
        return questionCreationResponseList;
    }

    @Override
    public UpdatedQuestionResponse updateQuestion(String questionId, UpdatedQuestionRequest questionCreationRequest, HttpServletRequest request) {
        if( questionCreationRequest == null) {
            throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
        }
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        if (questionCreationRequest.questionType() < 0 || questionCreationRequest.questionType() >= QuestionType.values().length) {
            throw new AppException(Constants.ErrorCodeMessage.INVALID_QUESTION_TYPE,
                    Constants.ErrorCode.INVALID_QUESTION_TYPE, HttpStatus.BAD_REQUEST.value());
        }


        List<QuestionCategory> categories = questionCreationRequest.questionCategories().stream()
                .map(QuestionCategory::valueOf)
                .toList();

        List<Question> previousVersions = questionRepository.findAllVersionByQuestionId(question);

        int currentVersion =0;
        for(Question previousVersion : previousVersions) {
            previousVersion.setIsCurrent(false);
            if(previousVersion.getVersion() > currentVersion) {
                currentVersion = previousVersion.getVersion();
            }
        }

        if (questionCreationRequest.questionType() == QuestionType.MULTIPLE_CHOICE.ordinal()) {

            // Create new question version
            Question updatedQuestion = Question.builder()
                    .questionId(question.getQuestionId())
                    .questionType(QuestionType.MULTIPLE_CHOICE)
                    .questionOrder(questionCreationRequest.questionOrder())
                    .categories(Set.copyOf(categories))
                    .explanation(questionCreationRequest.explanation())
                    .numberOfCorrectAnswers(questionCreationRequest.numberOfCorrectAnswers())
                    .questionGroup(question.getQuestionGroup())
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .parent(question)
                    .version(currentVersion + 1)
                    .point(questionCreationRequest.point())
                    .instructionForChoice(questionCreationRequest.instructionForChoice())
                    .build();

            question.setIsCurrent(false);
            question.setUpdatedBy(getUserIdFromToken(request));
            question.setUpdatedAt(LocalDateTime.now());

            questionRepository.save(question);
            questionRepository.save(updatedQuestion);

            return UpdatedQuestionResponse.builder()
                    .questionId(question.getQuestionId().toString())
                    .questionOrder(updatedQuestion.getQuestionOrder())
                    .point(updatedQuestion.getPoint())
                    .questionType(updatedQuestion.getQuestionType().ordinal())
                    .questionCategories(updatedQuestion.getCategories().stream()
                            .map(QuestionCategory::name)
                            .toList())
                    .explanation(updatedQuestion.getExplanation())
                    .questionGroupId(question.getQuestionGroup().getGroupId().toString())
                    .numberOfCorrectAnswers(updatedQuestion.getNumberOfCorrectAnswers())
                    .instructionForChoice(updatedQuestion.getInstructionForChoice())
                    .choices(updatedQuestion.getChoices().stream()
                            .map(c -> new UpdatedQuestionResponse.ChoiceResponse(
                                    c.getChoiceId().toString(),
                                    c.getLabel(),
                                    c.getChoiceOrder(),
                                    c.getContent(),
                                    c.isCorrect()))
                            .toList())
                    .build();
        } else if(questionCreationRequest.questionType() == QuestionType.FILL_IN_THE_BLANKS.ordinal()) {
            Question updatedQuestion = Question.builder()
                    .questionId(question.getQuestionId())
                    .questionType(QuestionType.FILL_IN_THE_BLANKS)
                    .point(questionCreationRequest.point())
                    .questionOrder(questionCreationRequest.questionOrder())
                    .blankIndex(questionCreationRequest.blankIndex())
                    .correctAnswer(questionCreationRequest.correctAnswer())
                    .categories(Set.copyOf(categories))
                    .explanation(questionCreationRequest.explanation())
                    .questionGroup(question.getQuestionGroup())
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .parent(question)
                    .version(currentVersion + 1)
                    .build();

        question.setIsCurrent(false);
        question.setUpdatedBy(getUserIdFromToken(request));
        question.setUpdatedAt(LocalDateTime.now());
        questionRepository.save(question);
        questionRepository.save(updatedQuestion);

        return UpdatedQuestionResponse.builder()
                .questionId(question.getQuestionId().toString())
                .questionOrder(updatedQuestion.getQuestionOrder())
                .point(updatedQuestion.getPoint())
                .questionType(updatedQuestion.getQuestionType().ordinal())
                .questionCategories(updatedQuestion.getCategories().stream()
                        .map(QuestionCategory::name)
                        .toList())
                .explanation(updatedQuestion.getExplanation())
                .questionGroupId(question.getQuestionGroup().getGroupId().toString())
                .numberOfCorrectAnswers(updatedQuestion.getNumberOfCorrectAnswers())
                .instructionForChoice(updatedQuestion.getInstructionForChoice())
                .blankIndex(updatedQuestion.getBlankIndex())
                .correctAnswer(updatedQuestion.getCorrectAnswer())
                .build();
        } else if(questionCreationRequest.questionType() == QuestionType.MATCHING.ordinal()) {
            Question updatedQuestion = Question.builder()
                    .questionId(question.getQuestionId())
                    .questionType(QuestionType.MATCHING)
                    .point(questionCreationRequest.point())
                    .questionOrder(questionCreationRequest.questionOrder())
                    .instructionForMatching(questionCreationRequest.instructionForMatching())
                    .correctAnswerForMatching(questionCreationRequest.correctAnswerForMatching())
                    .categories(Set.copyOf(categories))
                    .explanation(questionCreationRequest.explanation())
                    .questionGroup(question.getQuestionGroup())
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .parent(question)
                    .version(currentVersion + 1)
                    .build();

            question.setIsCurrent(false);
            question.setUpdatedBy(getUserIdFromToken(request));
            question.setUpdatedAt(LocalDateTime.now());
            questionRepository.save(question);
            questionRepository.save(updatedQuestion);

            return UpdatedQuestionResponse.builder()
                    .questionId(question.getQuestionId().toString())
                    .questionOrder(updatedQuestion.getQuestionOrder())
                    .point(updatedQuestion.getPoint())
                    .questionType(updatedQuestion.getQuestionType().ordinal())
                    .questionCategories(updatedQuestion.getCategories().stream()
                            .map(QuestionCategory::name)
                            .toList())
                    .explanation(updatedQuestion.getExplanation())
                    .questionGroupId(question.getQuestionGroup().getGroupId().toString())
                    .numberOfCorrectAnswers(updatedQuestion.getNumberOfCorrectAnswers())
                    .instructionForChoice(updatedQuestion.getInstructionForChoice())
                    .instructionForMatching(updatedQuestion.getInstructionForMatching())
                    .correctAnswerForMatching(updatedQuestion.getCorrectAnswerForMatching())
                    .build();
        } else   {
            var dragItem = dragItemRepository.findDragItemByDragItemId(UUID.fromString(questionCreationRequest.dragItemId()))
                    .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                            Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value()));
            Question updatedQuestion = Question.builder()
                    .questionId(question.getQuestionId())
                    .questionType(QuestionType.DRAG_AND_DROP)
                    .point(questionCreationRequest.point())
                    .questionOrder(questionCreationRequest.questionOrder())
                    .zoneIndex(questionCreationRequest.zoneIndex())
                    .dragItem(dragItem)
                    .categories(Set.copyOf(categories))
                    .explanation(questionCreationRequest.explanation())
                    .numberOfCorrectAnswers(questionCreationRequest.numberOfCorrectAnswers())
                    .zoneIndex(questionCreationRequest.zoneIndex())
                    .questionGroup(question.getQuestionGroup())
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .parent(question)
                    .version(currentVersion + 1)
                    .build();

            question.setIsCurrent(false);
            question.setUpdatedBy(getUserIdFromToken(request));
            question.setUpdatedAt(LocalDateTime.now());
            questionRepository.save(question);
            questionRepository.save(updatedQuestion);

            return UpdatedQuestionResponse.builder()
                    .questionId(question.getQuestionId().toString())
                    .questionOrder(updatedQuestion.getQuestionOrder())
                    .point(updatedQuestion.getPoint())
                    .questionType(updatedQuestion.getQuestionType().ordinal())
                    .questionCategories(updatedQuestion.getCategories().stream()
                            .map(QuestionCategory::name)
                            .toList())
                    .explanation(updatedQuestion.getExplanation())
                    .questionGroupId(question.getQuestionGroup().getGroupId().toString())
                    .numberOfCorrectAnswers(updatedQuestion.getNumberOfCorrectAnswers())
                    .instructionForChoice(updatedQuestion.getInstructionForChoice())
                    .zoneIndex(updatedQuestion.getZoneIndex())
                    .dragItems(List.of(
                            UpdatedQuestionResponse.DragItemResponse.builder()
                                    .dragItemId(dragItem.getDragItemId().toString())
                                    .content(dragItem.getContent())
                                    .build()
                            ))
                    .build();
        }

    }

    @Override
    @Transactional
    public UpdatedQuestionResponse updateOrder(
            String questionId,
            String groupId,
            OrderUpdatedQuestionRequest questionCreationRequest,
            HttpServletRequest request
    ) throws JsonProcessingException {
        String userId = getUserIdFromToken(request);
        UserProfileResponse userInformation = getUserProfileById(userId);
        QuestionGroup questionGroup = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        List<Question> questions = questionRepository
                .findAllByQuestionGroupOrderByQuestionOrderAsc(questionGroup);
        if (questions.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_LIST_EMPTY,
                    Constants.ErrorCode.QUESTION_LIST_EMPTY,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        UUID targetQuestionUuid;
        try {
            targetQuestionUuid = UUID.fromString(questionId);
        } catch (IllegalArgumentException ex) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Question targetQuestion = null;
        for (Question q : questions) {
            if (q.getQuestionId().equals(targetQuestionUuid)) {
                targetQuestion = q;
                break;
            }
        }
        if (targetQuestion == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        int newOrder = questionCreationRequest.order();
        if (newOrder < 1) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        questions.remove(targetQuestion);
        int insertIndex = newOrder - 1;
        if (insertIndex > questions.size()) {
            insertIndex = questions.size();
        }

        questions.add(insertIndex, targetQuestion);
        List<Question> toSave = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int recalculatedOrder = i + 1;
            if (q.getQuestionOrder() != recalculatedOrder) {
                q.setQuestionOrder(recalculatedOrder);
                q.setUpdatedBy(userInformation.id());
                toSave.add(q);
            }
        }


        if (!toSave.isEmpty()) {
            questionRepository.saveAll(toSave);
        }
        UserProfileResponse createdUser = getUserProfileById(toSave.getFirst().getCreatedBy().toString());
        UserProfileResponse updatedUser = getUserProfileById(userInformation.id());
        return UpdatedQuestionResponse.builder()
                .questionId(targetQuestion.getQuestionId().toString())
                .questionOrder(targetQuestion.getQuestionOrder())
                .point(targetQuestion.getPoint())
                .questionType(targetQuestion.getQuestionType().ordinal())
                .questionCategories(targetQuestion.getCategories().stream()
                        .map(QuestionCategory::name)
                        .toList())
                .explanation(targetQuestion.getExplanation())
                .questionGroupId(targetQuestion.getQuestionGroup().getGroupId().toString())
                .numberOfCorrectAnswers(targetQuestion.getNumberOfCorrectAnswers())
                .instructionForChoice(targetQuestion.getInstructionForChoice())
                .createdBy(UserInformationResponse.builder()
                        .userId(createdUser.id())
                        .firstName(createdUser.firstName())
                        .lastName(createdUser.lastName())
                        .email(createdUser.email())
                        .build())
                .updatedBy(UserInformationResponse.builder()
                        .userId(updatedUser.id())
                        .firstName(updatedUser.firstName())
                        .lastName(updatedUser.lastName())
                        .email(updatedUser.email())
                        .build())
                .createdAt(targetQuestion.getCreatedAt().toString())
                .updatedAt(targetQuestion.getUpdatedAt().toString())
                .build();
    }

    @Override
    public UpdatedQuestionResponse updateInformation(String questionId, String groupId,
                                                     InformationUpdatedQuestionRequest informationRequest,
                                                     HttpServletRequest request) throws JsonProcessingException {
        int lastVersion = 0;
        String userId = getUserIdFromToken(request);
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

            List<Question> getPreviousVersions = questionRepository.findAllPreviousVersion(question.getQuestionId());

        if (!getPreviousVersions.isEmpty()) {
            for(Question previousVersion : getPreviousVersions) {
                previousVersion.setIsCurrent(false);
                if( previousVersion.getVersion() > lastVersion) {
                    lastVersion = previousVersion.getVersion();
                }
            }

            questionRepository.saveAll(getPreviousVersions);
        }

        if (informationRequest == null) {
            throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
        }
        Question newVersion;

        if(question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            newVersion = Question.builder()
                    .questionOrder(question.getQuestionOrder())
                    .point(informationRequest.point())
                    .questionType(QuestionType.MULTIPLE_CHOICE)
                    .explanation(informationRequest.explanation())
                    .categories(informationRequest.questionCategories() == null ? Set.of() : informationRequest.questionCategories().stream()
                            .map(QuestionCategory::valueOf)
                            .collect(Collectors.toSet()))
                    .instructionForChoice(informationRequest.instructionForChoice())
                    .numberOfCorrectAnswers(informationRequest.numberOfCorrectAnswers())
                    .parent(question)
                    .questionGroup(question.getQuestionGroup())
                    .version(lastVersion + 1)
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            question.setUpdatedAt(LocalDateTime.now());
            question.setIsCurrent(false);
            question.setUpdatedBy(userId);
            questionRepository.save(question);
            questionRepository.save(newVersion);

        } else if(question.getQuestionType() == QuestionType.FILL_IN_THE_BLANKS) {
            newVersion = Question.builder()
                    .questionOrder(question.getQuestionOrder())
                    .point(informationRequest.point())
                    .questionType(QuestionType.FILL_IN_THE_BLANKS)
                    .categories(informationRequest.questionCategories() == null ? Set.of() : informationRequest.questionCategories().stream()
                            .map(QuestionCategory::valueOf)
                            .collect(Collectors.toSet()))
                    .explanation(informationRequest.explanation())
                    .blankIndex(informationRequest.blankIndex())
                    .correctAnswer(informationRequest.correctAnswer())
                    .parent(question)
                    .questionGroup(question.getQuestionGroup())
                    .version(lastVersion + 1)
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            question.setUpdatedAt(LocalDateTime.now());
            question.setIsCurrent(false);
            question.setUpdatedBy(userId);
            questionRepository.save(question);
            questionRepository.save(newVersion);
        } else if(question.getQuestionType() == QuestionType.MATCHING) {
            newVersion = Question.builder()
                    .questionOrder(question.getQuestionOrder())
                    .point(informationRequest.point())
                    .questionType(QuestionType.MATCHING)
                    .categories(informationRequest.questionCategories() == null ? Set.of() : informationRequest.questionCategories().stream()
                            .map(QuestionCategory::valueOf)
                            .collect(Collectors.toSet()))
                    .explanation(informationRequest.explanation())
                    .instructionForMatching(informationRequest.instructionForMatching())
                    .correctAnswerForMatching(informationRequest.correctAnswerForMatching())
                    .parent(question)
                    .questionGroup(question.getQuestionGroup())
                    .version(lastVersion + 1)
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            question.setUpdatedAt(LocalDateTime.now());
            question.setIsCurrent(false);
            question.setUpdatedBy(userId);
            questionRepository.save(question);
            questionRepository.save(newVersion);
        } else {
            var dragItem = dragItemRepository.findDragItemByDragItemId(UUID.fromString(informationRequest.dragItemId()))
                    .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                            Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value()));
            newVersion = Question.builder()
                    .questionOrder(question.getQuestionOrder())
                    .point(informationRequest.point())
                    .questionType(QuestionType.DRAG_AND_DROP)
                    .explanation(informationRequest.explanation())
                    .categories(informationRequest.questionCategories() == null ? Set.of() : informationRequest.questionCategories().stream()
                            .map(QuestionCategory::valueOf)
                            .collect(Collectors.toSet()))
                    .questionGroup(question.getQuestionGroup())
                    .zoneIndex(informationRequest.zoneIndex())
                    .dragItem(dragItem)
                    .categories(question.getCategories())
                    .numberOfCorrectAnswers(informationRequest.numberOfCorrectAnswers())
                    .zoneIndex(informationRequest.zoneIndex())
                    .parent(question)
                    .version(lastVersion + 1)
                    .isOriginal(false)
                    .isCurrent(true)
                    .isDeleted(false)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            question.setUpdatedAt(LocalDateTime.now());
            question.setIsCurrent(false);
            question.setUpdatedBy(userId);
            questionRepository.save(question);
            questionRepository.save(newVersion);
        }


        UserProfileResponse createdUser = getUserProfileById(question.getCreatedBy().toString());
        UserProfileResponse updatedUser = getUserProfileById(question.getUpdatedBy());
        return UpdatedQuestionResponse.builder()
                .questionId(question.getQuestionId().toString())
                .questionOrder(newVersion.getQuestionOrder())
                .point(newVersion.getPoint())
                .questionType(newVersion.getQuestionType().ordinal())
                .questionCategories(newVersion.getCategories() == null ? List.of() : newVersion.getCategories().stream()
                        .map(QuestionCategory::name)
                        .toList())
                .explanation(newVersion.getExplanation())
                .questionGroupId(newVersion.getQuestionGroup().getGroupId().toString())
                .numberOfCorrectAnswers(newVersion.getNumberOfCorrectAnswers())
                .instructionForChoice(newVersion.getInstructionForChoice())
                .blankIndex(newVersion.getBlankIndex() == null ? null : newVersion.getBlankIndex())
                .correctAnswer(newVersion.getCorrectAnswer() == null ? null : newVersion.getCorrectAnswer())
                .instructionForMatching(newVersion.getInstructionForMatching() == null ? null : newVersion.getInstructionForMatching())
                .correctAnswerForMatching(newVersion.getCorrectAnswerForMatching() == null ? null : newVersion.getCorrectAnswerForMatching())
                .zoneIndex(newVersion.getZoneIndex() == null ? null : newVersion.getZoneIndex())
                .dragItems(newVersion.getDragItem() == null ? List.of() : List.of(
                        UpdatedQuestionResponse.DragItemResponse.builder()
                                .dragItemId(newVersion.getDragItem().getDragItemId().toString())
                                .content(newVersion.getDragItem().getContent())
                                .build()
                ))

                .createdBy(UserInformationResponse.builder()
                        .userId(createdUser.id())
                        .firstName(createdUser.firstName())
                        .lastName(createdUser.lastName())
                        .email(createdUser.email())
                        .build())
                .updatedBy(UserInformationResponse.builder()
                        .userId(updatedUser.id())
                        .firstName(updatedUser.firstName())
                        .lastName(updatedUser.lastName())
                        .email(updatedUser.email())
                        .build())
                .createdAt(question.getCreatedAt().toString())
                .updatedAt(question.getUpdatedAt().toString())
                .build();
    }

    @Override
    @Transactional
    public void deleteQuestion(String questionId, String groupId, HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        QuestionGroup questionGroup = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        if (!question.getQuestionGroup().equals(questionGroup)) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_BELONG_TO_GROUP,
                    Constants.ErrorCode.QUESTION_NOT_BELONG_TO_GROUP, HttpStatus.BAD_REQUEST.value());
        }
        questionGroup.getQuestions().remove(question);
        question.setQuestionGroup(null);
        question.setIsDeleted(false);
        question.setIsCurrent(false);
        questionRepository.save(question);
        questionGroup.setUpdatedBy(userId);
        questionGroupRepository.save(questionGroup);
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
