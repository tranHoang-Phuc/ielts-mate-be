package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Choice;
import com.fptu.sep490.listeningservice.model.DragItem;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.DragItemRepository;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.service.QuestionService;
import com.fptu.sep490.listeningservice.viewmodel.request.InformationUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.OrderUpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.UpdatedQuestionRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionCreationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UpdatedQuestionResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserInformationResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    QuestionRepository questionRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    Helper helper;

    @Override
    @Transactional
    public List<QuestionCreationResponse> createQuestions(
            List<QuestionCreationRequest> questionCreationRequest, HttpServletRequest request) throws JsonProcessingException {
        List<QuestionCreationResponse> questionCreationResponseList = new ArrayList<>();
        if (questionCreationRequest == null || questionCreationRequest.isEmpty()) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_LIST_EMPTY,
                    Constants.ErrorCode.QUESTION_LIST_EMPTY, HttpStatus.BAD_REQUEST.value());
        }

        String userId = helper.getUserIdFromToken(request);


        for(QuestionCreationRequest question : questionCreationRequest){
            int correctAnswersCount = 0;
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

            Question savedQuestion = Question.builder()
                    .questionOrder(question.questionOrder())
                    .categories(Set.copyOf(categories))
                    .explanation(question.explanation())
                    .questionGroup(questionGroup)
                    .isOriginal(true)
                    .isCurrent(true)
                    .version(1)
                    .isDeleted(false)
                    .point(question.point())
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();

            UserInformationResponse userInformationResponse = helper.getUserInformationResponse(userId);
            if(question.questionType() == QuestionType.MULTIPLE_CHOICE.ordinal()) {
                if (question.choices() == null || question.choices().isEmpty()) {
                    throw new AppException(Constants.ErrorCodeMessage.CHOICES_LIST_EMPTY,
                            Constants.ErrorCode.CHOICES_LIST_EMPTY, HttpStatus.BAD_REQUEST.value());
                }
                if (question.numberOfCorrectAnswers() < 1) {
                    throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                            Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
                }

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
                savedQuestion.setNumberOfCorrectAnswers(question.numberOfCorrectAnswers());
                savedQuestion.setInstructionForChoice(question.instructionForChoice());
                savedQuestion.setQuestionType(QuestionType.MULTIPLE_CHOICE);


                Question saved = questionRepository.saveAndFlush(savedQuestion);

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
                                .createdBy(userInformationResponse)
                                .updatedBy(userInformationResponse)
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

                savedQuestion.setQuestionType(QuestionType.FILL_IN_THE_BLANKS);
                savedQuestion.setCorrectAnswer(question.correctAnswer());
                savedQuestion.setBlankIndex(question.blankIndex());
                Question saved = questionRepository.saveAndFlush(savedQuestion);

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
                                .createdBy(userInformationResponse)
                                .updatedBy(userInformationResponse)
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
                savedQuestion.setQuestionType(QuestionType.MATCHING);
                savedQuestion.setInstructionForMatching(question.instructionForMatching());
                savedQuestion.setCorrectAnswerForMatching(question.correctAnswerForMatching());
                Question saved = questionRepository.saveAndFlush(savedQuestion);

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
                                .createdBy(userInformationResponse)
                                .updatedBy(userInformationResponse)
                                .build());
            }
            else {
                var dragItem = dragItemRepository.findDragItemByDragItemId(UUID.fromString(question.dragItemId()))
                        .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.INVALID_REQUEST,
                                Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value()));

                savedQuestion.setQuestionType(QuestionType.DRAG_AND_DROP);
                savedQuestion.setZoneIndex(question.zoneIndex());
                savedQuestion.setNumberOfCorrectAnswers(question.numberOfCorrectAnswers());
                savedQuestion.setDragItem(dragItem);

                dragItem.setQuestion(savedQuestion);

                Question saved = questionRepository.saveAndFlush(savedQuestion);

                dragItemRepository.save(dragItem);
                saved.setCreatedBy(userId);
                saved.setUpdatedBy(userId);
                UserProfileResponse userProfile = helper.getUserProfileById(userId);
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
                                .createdBy(helper.getUserInformationResponse(userProfile.id()))
                                .updatedBy(helper.getUserInformationResponse(userProfile.id()))
                                .createdAt(saved.getCreatedAt().toString())
                                .updatedAt(saved.getUpdatedAt().toString())
                                .build()
                );

            }

        }
        return questionCreationResponseList;
    }

    @Override
    @Transactional
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
            question.setUpdatedBy(helper.getUserIdFromToken(request));
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
            question.setUpdatedBy(helper.getUserIdFromToken(request));
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
            question.setUpdatedBy(helper.getUserIdFromToken(request));
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
            question.setUpdatedBy(helper.getUserIdFromToken(request));
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

    @Transactional
    @Override
    public UpdatedQuestionResponse updateOrder(
            String questionId,
            String groupId,
            OrderUpdatedQuestionRequest questionCreationRequest,
            HttpServletRequest request
    ) throws JsonProcessingException {

        String userId = helper.getUserIdFromToken(request);
        UserProfileResponse userInformation = helper.getUserProfileById(userId);

        QuestionGroup questionGroup = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // Get all questions in the group (original versions)
        List<Question> allQuestions = questionRepository
                .findAllByQuestionGroupOrderByQuestionOrderAsc(questionGroup);

        if (allQuestions.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_LIST_EMPTY,
                    Constants.ErrorCode.QUESTION_LIST_EMPTY,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        // Get current versions of all questions
        List<Question> currentVersionQuestions = questionRepository.findAllCurrentVersion(
                allQuestions.stream().map(Question::getQuestionId).toList()
        );

        if (currentVersionQuestions.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_LIST_EMPTY,
                    Constants.ErrorCode.QUESTION_LIST_EMPTY,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        // Sort current versions by their current order
        currentVersionQuestions.sort(Comparator.comparingInt(Question::getQuestionOrder));

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

        // Find target question in current versions
        Question targetQuestion = null;
        for (Question q : currentVersionQuestions) {
            if (q.getQuestionId().equals(targetQuestionUuid) ||
                    (q.getParent() != null && q.getParent().getQuestionId().equals(targetQuestionUuid))) {
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

        // Debug logging
        log.info("Requested new order: {}, Total questions: {}, Target question current order: {}",
                newOrder, currentVersionQuestions.size(), targetQuestion.getQuestionOrder());

        // Reorder questions: assign newOrder to target and shift others accordingly
        List<Question> toSave = new ArrayList<>();
        Map<UUID, Integer> orderUpdateMap = new HashMap<>(); // baseId -> newOrder

        int oldOrder = targetQuestion.getQuestionOrder();

        log.info("Reordering: Moving question from order {} to order {}", oldOrder, newOrder);

        if (oldOrder != newOrder) {
            // Update all questions that need to be shifted
            for (Question q : currentVersionQuestions) {
                int currentOrder = q.getQuestionOrder();
                int newQuestionOrder = currentOrder;

                if (q.getQuestionId().equals(targetQuestion.getQuestionId())) {
                    // This is the target question - assign the new order
                    newQuestionOrder = newOrder;
                    log.info("Target question {} gets new order {}", q.getQuestionId(), newOrder);
                } else {
                    // Shift other questions based on the movement
                    if (newOrder > oldOrder) {
                        // Moving target down: shift questions between oldOrder+1 and newOrder up by 1
                        if (currentOrder > oldOrder && currentOrder <= newOrder) {
                            newQuestionOrder = currentOrder - 1;
                            log.info("Shifting question {} up: {} -> {}", q.getQuestionId(), currentOrder, newQuestionOrder);
                        }
                    } else {
                        // Moving target up: shift questions between newOrder and oldOrder-1 down by 1
                        if (currentOrder >= newOrder && currentOrder < oldOrder) {
                            newQuestionOrder = currentOrder + 1;
                            log.info("Shifting question {} down: {} -> {}", q.getQuestionId(), currentOrder, newQuestionOrder);
                        }
                    }
                }

                // Update if order changed
                if (currentOrder != newQuestionOrder) {
                    q.setQuestionOrder(newQuestionOrder);
                    q.setUpdatedBy(userInformation.id());
                    toSave.add(q);

                    // Get base ID for updating all versions
                    UUID baseId = q.getParent() != null ? q.getParent().getQuestionId() : q.getQuestionId();
                    orderUpdateMap.put(baseId, newQuestionOrder);
                }
            }
        }

        // Save current versions first
        if (!toSave.isEmpty()) {
            questionRepository.saveAll(toSave);
        }

        // Update order for all versions of each question
        for (Map.Entry<UUID, Integer> entry : orderUpdateMap.entrySet()) {
            questionRepository.updateOrderForAllVersions(entry.getKey(), entry.getValue(), userInformation.id());
        }

        // Reload the target question with categories eagerly fetched to avoid lazy loading exception
        Question refreshedTarget = questionRepository.findByIdWithCategories(targetQuestion.getQuestionId())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // Extract categories names (now safely loaded)
        List<String> categoriesNames = refreshedTarget.getCategories().stream()
                .map(QuestionCategory::name)
                .toList();

        UserInformationResponse createdUser = helper.getUserInformationResponse(targetQuestion.getCreatedBy().toString());
        UserInformationResponse updatedUser = helper.getUserInformationResponse(userInformation.id());

        return UpdatedQuestionResponse.builder()
                .questionId(targetQuestion.getQuestionId().toString())
                .questionOrder(targetQuestion.getQuestionOrder())
                .point(targetQuestion.getPoint())
                .questionType(targetQuestion.getQuestionType().ordinal())
                .questionCategories(categoriesNames)
                .explanation(targetQuestion.getExplanation())
                .questionGroupId(targetQuestion.getQuestionGroup().getGroupId().toString())
                .numberOfCorrectAnswers(targetQuestion.getNumberOfCorrectAnswers())
                .instructionForChoice(targetQuestion.getInstructionForChoice())
                .createdBy(createdUser)
                .updatedBy(updatedUser)
                .createdAt(targetQuestion.getCreatedAt().toString())
                .updatedAt(targetQuestion.getUpdatedAt().toString())
                .build();
    }
    @Transactional
    @Override
    public UpdatedQuestionResponse updateInformation(String questionId, String groupId,
                                                     InformationUpdatedQuestionRequest informationRequest,
                                                     HttpServletRequest request) throws JsonProcessingException {
        int lastVersion = 0;
        String userId = helper.getUserIdFromToken(request);
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
                    .numberOfCorrectAnswers(informationRequest.numberOfCorrectAnswers() != null ? informationRequest.numberOfCorrectAnswers() : 0)
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
                    .categories(question.getCategories() != null ?
                            Set.copyOf(question.getCategories()) :
                            Set.of())
                    .numberOfCorrectAnswers(informationRequest.numberOfCorrectAnswers() != null ? informationRequest.numberOfCorrectAnswers() : 0)
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


        UserInformationResponse createdUser = helper.getUserInformationResponse(question.getCreatedBy().toString());
        UserInformationResponse updatedUser = helper.getUserInformationResponse(question.getUpdatedBy());
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

                .createdBy(createdUser)
                .updatedBy(updatedUser)
                .createdAt(question.getCreatedAt().toString())
                .updatedAt(question.getUpdatedAt().toString())
                .build();
    }

    @Transactional
    @Override
    public void deleteQuestion(String questionId, String groupId, HttpServletRequest request) {
        String userId = helper.getUserIdFromToken(request);
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

//        question.setQuestionGroup(null);
        question.setIsDeleted(true);
        question.setIsCurrent(false);
        questionRepository.save(question);
        questionGroup.setUpdatedBy(userId);
        questionGroupRepository.save(questionGroup);
    }

}
