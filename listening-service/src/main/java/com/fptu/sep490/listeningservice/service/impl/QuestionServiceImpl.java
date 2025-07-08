package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Choice;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.DragItemRepository;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.service.QuestionService;
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

        int correctAnswersCount = 0;
        String userId = helper.getUserIdFromToken(request);


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


                Question saved = questionRepository.save(savedQuestion);

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
                Question saved = questionRepository.save(savedQuestion);

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
                Question saved = questionRepository.save(savedQuestion);

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

                Question saved = questionRepository.save(savedQuestion);

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

}
