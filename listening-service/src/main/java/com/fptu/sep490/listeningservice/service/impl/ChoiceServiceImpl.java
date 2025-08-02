package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.Choice;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.repository.ChoiceRepository;
import com.fptu.sep490.listeningservice.repository.QuestionRepository;
import com.fptu.sep490.listeningservice.service.ChoiceService;
import com.fptu.sep490.listeningservice.viewmodel.request.ChoiceRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ChoiceResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ChoiceServiceImpl implements ChoiceService {
    Helper helper;
    ChoiceRepository choiceRepository;
    QuestionRepository questionRepository;

    @Override
    public ChoiceResponse createChoice(String questionId, ChoiceRequest choiceRequest, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (question.getIsDeleted()){
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        int numberOfCorrectAnswers = 0;
        List<Choice> choices =  choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true);
        if(!choices.isEmpty()) {
            for (Choice choice : choices) {
                if(choice.getChoiceOrder() == choiceRequest.choiceOrder()) {
                    throw new AppException(
                            Constants.ErrorCodeMessage.CHOICE_ORDER_EXISTS,
                            Constants.ErrorCode.CHOICE_ORDER_EXISTS,
                            HttpStatus.BAD_REQUEST.value()
                    );
                }
                if (choice.isCorrect()) {
                    numberOfCorrectAnswers++;
                }
            }
        }
        if (choiceRequest.isCorrect()) {
            if (numberOfCorrectAnswers + 1 > question.getNumberOfCorrectAnswers()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                        Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
        }
        question.setUpdatedBy(userId);
        Choice newChoice = Choice.builder()
                .content(choiceRequest.content())
                .choiceOrder(choiceRequest.choiceOrder())
                .isCorrect(choiceRequest.isCorrect())
                .label(choiceRequest.label())
                .question(question)
                .isDeleted(false)
                .isOriginal(true)
                .isCurrent(true)
                .version(1)
                .build();
        Choice saved = choiceRepository.save(newChoice);
        return ChoiceResponse.builder()
                .choiceId(saved.getChoiceId().toString())
                .content(saved.getContent())
                .choiceOrder(saved.getChoiceOrder())
                .isCorrect(saved.isCorrect())
                .label(saved.getLabel())
                .questionId(saved.getQuestion().getQuestionId().toString())
                .build();

    }

    @Override
    public ChoiceResponse updateChoice(String questionId, String choiceId, ChoiceRequest choiceRequest, HttpServletRequest request) throws Exception {
        Choice existingChoice = choiceRepository.findById(UUID.fromString(choiceId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (question.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if( !existingChoice.getQuestion().getQuestionId().equals(question.getQuestionId())) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        Choice currentChoice = findCurrentOrChildCurrentChoice(existingChoice);


        int numberOfCorrectAnswers = 0;
        List<Choice> choices =  choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true);
        if(!choices.isEmpty()) {
            for (Choice choice : choices) {
                if(choice.getChoiceOrder() == choiceRequest.choiceOrder() && !choiceRequest.choiceOrder().equals(currentChoice.getChoiceOrder())) {
                    throw new AppException(
                            Constants.ErrorCodeMessage.CHOICE_ORDER_EXISTS,
                            Constants.ErrorCode.CHOICE_ORDER_EXISTS,
                            HttpStatus.BAD_REQUEST.value()
                    );
                }
                if (choice.isCorrect()) {
                    numberOfCorrectAnswers++;
                }
            }
        }
        if (choiceRequest.isCorrect()) {
            if (numberOfCorrectAnswers + 1 > question.getNumberOfCorrectAnswers()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                        Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
        }

        currentChoice.setIsCurrent(false);

        Choice newChoice = new Choice();
        newChoice.setLabel(choiceRequest.label());
        newChoice.setContent(choiceRequest.content());
        newChoice.setChoiceOrder(choiceRequest.choiceOrder());
        newChoice.setCorrect(choiceRequest.isCorrect());
        newChoice.setQuestion(question);
        newChoice.setIsOriginal(false);
        newChoice.setIsCurrent(true);
        newChoice.setIsDeleted(false);
        newChoice.setVersion(currentChoice.getVersion() + 1);
        newChoice.setParent(existingChoice);
        choiceRepository.save(existingChoice);
        choiceRepository.save(newChoice);
        return ChoiceResponse.builder()
                .choiceId(newChoice.getChoiceId().toString())
                .content(newChoice.getContent())
                .choiceOrder(newChoice.getChoiceOrder())
                .isCorrect(newChoice.isCorrect())
                .label(newChoice.getLabel())
                .questionId(newChoice.getQuestion().getQuestionId().toString())
                .build();


    }

    @Override
    public ChoiceResponse getChoiceById(String questionId, String choiceId, HttpServletRequest request) {
        Choice choice = choiceRepository.findById(UUID.fromString(choiceId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (question.getIsDeleted() || choice.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if (!choice.getQuestion().getQuestionId().equals(question.getQuestionId())) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        Choice currentChoice = findCurrentOrChildCurrentChoice(choice);
        return ChoiceResponse.builder()
                .choiceId(currentChoice.getChoiceId().toString())
                .content(currentChoice.getContent())
                .choiceOrder(currentChoice.getChoiceOrder())
                .isCorrect(currentChoice.isCorrect())
                .label(currentChoice.getLabel())
                .questionId(currentChoice.getQuestion().getQuestionId().toString())
                .build();

    }

    @Override
    public List<ChoiceResponse> getAllChoicesOfQuestion(String questionId, HttpServletRequest request) {
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (question.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        List<Choice> choices = choiceRepository.findByQuestionAndIsDeletedAndIsCurrentOrderByChoiceOrderAsc(question, false, true);
        return choices.stream()
                .map(choice -> ChoiceResponse.builder()
                        .choiceId(choice.getChoiceId().toString())
                        .content(choice.getContent())
                        .choiceOrder(choice.getChoiceOrder())
                        .isCorrect(choice.isCorrect())
                        .label(choice.getLabel())
                        .questionId(choice.getQuestion().getQuestionId().toString())
                        .build())
                .toList();




    }

    @Override
    public void deleteChoice(String questionId, String choiceId, HttpServletRequest request) {
        Choice choice = choiceRepository.findById(UUID.fromString(choiceId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (question.getIsDeleted() || choice.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if (!choice.getQuestion().getQuestionId().equals(question.getQuestionId())) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        choice.setIsDeleted(true);
        if(!choice.getChildren().isEmpty()){
           for (Choice child : choice.getChildren()) {
                child.setIsDeleted(true);
                choiceRepository.save(child);
            }
        }
        choiceRepository.save(choice);
    }

    @Override
    public void switchChoicesOrder(String questionId, String choiceId1, String choiceId2, HttpServletRequest request) {
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (question.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Choice choice1 = choiceRepository.findById(UUID.fromString(choiceId1))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        Choice choice2 = choiceRepository.findById(UUID.fromString(choiceId2))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (choice1.getIsDeleted() || choice2.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    Constants.ErrorCode.CHOICE_NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if (!choice1.getQuestion().getQuestionId().equals(question.getQuestionId()) ||
                !choice2.getQuestion().getQuestionId().equals(question.getQuestionId())) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                    Constants.ErrorCode.CHOICE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        Choice currentChoice1 = findCurrentOrChildCurrentChoice(choice1);
        Choice currentChoice2 = findCurrentOrChildCurrentChoice(choice2);
        int tempOrder = currentChoice1.getChoiceOrder();
        currentChoice1.setChoiceOrder(currentChoice2.getChoiceOrder());
        currentChoice2.setChoiceOrder(tempOrder);
        choiceRepository.save(currentChoice1);
        choiceRepository.save(currentChoice2);



    }

    public Choice findCurrentOrChildCurrentChoice(Choice choice) {
        if (choice.getIsCurrent() && !choice.getIsDeleted()) {
            return choice;
        }
        for (Choice child : choice.getChildren()) {
            if (child.getIsCurrent() && !child.getIsDeleted()) {
                return child;
            }
        }
        return choice;
        }
}
