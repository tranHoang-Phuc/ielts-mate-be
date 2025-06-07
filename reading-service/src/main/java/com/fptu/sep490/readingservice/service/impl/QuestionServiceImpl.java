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
import com.fptu.sep490.readingservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.readingservice.viewmodel.response.UpdatedQuestionResponse;
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
import org.springframework.transaction.annotation.Transactional;
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
    return null;

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
        String userId = getUserIdFromToken(request);
        UserProfileResponse userInformation = getUserProfileById(userId);
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (informationRequest == null) {
            throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
        }
        if(informationRequest.explanation() != null || !informationRequest.explanation().isEmpty()) {
            question.setExplanation(informationRequest.explanation());
        }
        if(informationRequest.point() != null && informationRequest.point() > 0) {
            question.setPoint(informationRequest.point());
        }
        if(informationRequest.questionCategories() != null && !informationRequest.questionCategories().isEmpty()) {
            List<QuestionCategory> categories = informationRequest.questionCategories().stream()
                    .map(QuestionCategory::valueOf)
                    .toList();
            question.setCategories(Set.copyOf(categories));
        }
        if(informationRequest.numberOfCorrectAnswers() != null && informationRequest.numberOfCorrectAnswers() > 0) {
            question.setNumberOfCorrectAnswers(informationRequest.numberOfCorrectAnswers());
        }

        if(informationRequest.instructionForChoice() != null && !informationRequest.instructionForChoice().isEmpty()) {
            question.setInstructionForChoice(informationRequest.instructionForChoice());
        }
        if(informationRequest.blankIndex() != null && informationRequest.blankIndex() >= 0) {
            if (question.getQuestionType() != QuestionType.FILL_IN_THE_BLANKS) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
            }
            question.setBlankIndex(informationRequest.blankIndex());
        }
        if(informationRequest.correctAnswer() != null && !informationRequest.correctAnswer().isEmpty()) {
            if (question.getQuestionType() != QuestionType.FILL_IN_THE_BLANKS) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
            }
            question.setCorrectAnswer(informationRequest.correctAnswer());
        }
        if(informationRequest.instructionForMatching() != null && !informationRequest.instructionForMatching().isEmpty()) {
            if (question.getQuestionType() != QuestionType.MATCHING) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
            }
            question.setInstructionForMatching(informationRequest.instructionForMatching());
        }
        if(informationRequest.correctAnswerForMatching() != null && !informationRequest.correctAnswerForMatching().isEmpty()) {
            if (question.getQuestionType() != QuestionType.MATCHING) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
            }
            question.setCorrectAnswerForMatching(informationRequest.correctAnswerForMatching());
        }
        if(informationRequest.zoneIndex() != null && informationRequest.zoneIndex() >= 0) {
            if (question.getQuestionType() != QuestionType.DRAG_AND_DROP) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
            }
            question.setZoneIndex(informationRequest.zoneIndex());
        }
        if(informationRequest.dragItemId() != null && !informationRequest.dragItemId().isEmpty()) {
            if (question.getQuestionType() != QuestionType.DRAG_AND_DROP) {
                throw new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value());
            }
            DragItem dragItem = dragItemRepository.findDragItemByDragItemId(UUID.fromString(informationRequest.dragItemId()))
                    .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.DRAG_ITEM_NOT_FOUND,
                            Constants.ErrorCode.DRAG_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
            question.setDragItem(dragItem);
        }

        question.setUpdatedBy(userInformation.id());
        Question savedQuestion = questionRepository.save(question);
        UserProfileResponse createdUser = getUserProfileById(savedQuestion.getCreatedBy().toString());
        UserProfileResponse updatedUser = getUserProfileById(userInformation.id());
        return UpdatedQuestionResponse.builder()
                .questionId(savedQuestion.getQuestionId().toString())
                .questionOrder(savedQuestion.getQuestionOrder())
                .point(savedQuestion.getPoint())
                .questionType(savedQuestion.getQuestionType().ordinal())
                .questionCategories(savedQuestion.getCategories().stream()
                        .map(QuestionCategory::name)
                        .toList())
                .explanation(savedQuestion.getExplanation())
                .questionGroupId(savedQuestion.getQuestionGroup().getGroupId().toString())
                .numberOfCorrectAnswers(savedQuestion.getNumberOfCorrectAnswers())
                .instructionForChoice(savedQuestion.getInstructionForChoice())
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
                .createdAt(savedQuestion.getCreatedAt().toString())
                .updatedAt(savedQuestion.getUpdatedAt().toString())
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
        questionRepository.delete(question);
        question.setUpdatedBy(userId);
        questionRepository.save(question);
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
