package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.repository.ChoiceRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.service.ChoiceService;
import com.fptu.sep490.readingservice.viewmodel.request.ChoiceCreation;
import com.fptu.sep490.readingservice.viewmodel.request.UpdatedChoiceRequest;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChoiceServiceImpl implements ChoiceService {
    ChoiceRepository choiceRepository;
    QuestionRepository questionRepository;
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


    @Override
    public List<QuestionCreationResponse.ChoiceResponse> getAllChoicesOfQuestion(String questionId) {
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        List<Choice> choices = choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(question, false);
        if (choices.isEmpty()) {
            throw new AppException(Constants.ErrorCodeMessage.CHOICES_LIST_EMPTY,
                    Constants.ErrorCode.CHOICES_LIST_EMPTY, HttpStatus.NOT_FOUND.value());
        }
        return choices.stream()
                .map(choice -> QuestionCreationResponse.ChoiceResponse.builder()
                        .choiceId(choice.getChoiceId().toString())
                        .content(choice.getContent())
                        .choiceOrder(choice.getChoiceOrder())
                        .isCorrect(choice.isCorrect())
                        .label(choice.getLabel())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public QuestionCreationResponse.ChoiceResponse createChoice(String questionId, ChoiceCreation choice,
                                                                HttpServletRequest request) throws JsonProcessingException {
        String userId = getUserIdFromToken(request);
        int numberOfCorrectAnswers = 0;
        Question question = questionRepository.findById(UUID.fromString(questionId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        Question currentVersion = questionRepository.findCurrentQuestion(question.getQuestionId());
        List<Choice> choices = choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(question, false);
        if (!choices.isEmpty()) {
            for (Choice existingChoice : choices) {
                if (existingChoice.isCorrect()) {
                    numberOfCorrectAnswers++;
                }
            }
        }
        if (choice.isCorrect()) {
            numberOfCorrectAnswers++;
        }
        if (numberOfCorrectAnswers > currentVersion.getNumberOfCorrectAnswers()) {
            throw new AppException(Constants.ErrorCodeMessage.INVALID_NUMBER_OF_CORRECT_ANSWERS,
                    Constants.ErrorCode.INVALID_NUMBER_OF_CORRECT_ANSWERS, HttpStatus.BAD_REQUEST.value());
        }
        question.setUpdatedBy(userId);
        Choice newChoice = Choice.builder()
                .content(choice.content())
                .choiceOrder(choice.choiceOrder())
                .isCorrect(choice.isCorrect())
                .label(choice.label())
                .isDeleted(false)
                .isOriginal(true)
                .isCurrent(true)
                .version(1)
                .question(question)
                .build();
        Choice saved = choiceRepository.save(newChoice);
        return QuestionCreationResponse.ChoiceResponse.builder()
                .choiceId(saved.getChoiceId().toString())
                .content(saved.getContent())
                .choiceOrder(saved.getChoiceOrder())
                .isCorrect(saved.isCorrect())
                .label(saved.getLabel())
                .build();
    }

    @Override
    @Transactional
    public QuestionCreationResponse.ChoiceResponse updateChoice(String questionId, String choiceId,
                                                                UpdatedChoiceRequest choice, HttpServletRequest request) {
        Choice existingChoice = choiceRepository.findById(UUID.fromString(choiceId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        if(!existingChoice.getQuestion().getQuestionId().toString().equals(questionId)) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_NOT_FOUND, HttpStatus.NOT_FOUND.value());
        }

        int currentVersion = 0;
        List<Choice> previousVersions = choiceRepository.findAllVersion(existingChoice.getChoiceId());
        for(Choice c : previousVersions) {
            c.setIsDeleted(false);
            if(c.getVersion() > currentVersion) {
                currentVersion = c.getVersion();
            }
        }
        choiceRepository.saveAll(previousVersions);

        int numberOfCorrectAnswers = 0;
        List<Choice> choices = choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(existingChoice.getQuestion(), false);
        for (Choice existing : choices) {
            if (existing.isCorrect()) {
                numberOfCorrectAnswers++;
            }
        }
        if (choice.isCorrect()) {
            numberOfCorrectAnswers++;
        }
        if (numberOfCorrectAnswers != existingChoice.getQuestion().getNumberOfCorrectAnswers()) {
            // update the number of correct answers in the question
            existingChoice.getQuestion().setNumberOfCorrectAnswers(numberOfCorrectAnswers);
            questionRepository.save(existingChoice.getQuestion());
        }
        if(choice.label() == null || choice.label().isEmpty()) {
           throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if(choice.content() == null || choice.content().isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if(choice.choiceOrder() == null || choice.choiceOrder() < 0) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        Choice newVersion = Choice.builder()
                .choiceId(existingChoice.getChoiceId())
                .content(choice.content())
                .choiceOrder(choice.choiceOrder())
                .isCorrect(choice.isCorrect())
                .label(choice.label())
                .isDeleted(false)
                .isOriginal(false)
                .isCurrent(true)
                .version(currentVersion + 1)
                .question(existingChoice.getQuestion())
                .build();

        choiceRepository.save(newVersion);
        return QuestionCreationResponse.ChoiceResponse.builder()
                .choiceId(existingChoice.getChoiceId().toString())
                .content(newVersion.getContent())
                .choiceOrder(newVersion.getChoiceOrder())
                .isCorrect(newVersion.isCorrect())
                .label(newVersion.getLabel())
                .build();
    }

    @Override
    @Transactional
    public void deleteChoice(String questionId, String choiceId, HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        UUID questionUuid;
        try {
            questionUuid = UUID.fromString(questionId);
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Question question = questionRepository.findById(questionUuid)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        UUID choiceUuid;
        try {
            choiceUuid = UUID.fromString(choiceId);
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Choice choice = choiceRepository.findById(choiceUuid)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.CHOICE_NOT_FOUND,
                        Constants.ErrorCode.CHOICE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (!choice.getQuestion().getQuestionId().equals(questionUuid)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.CHOICE_NOT_IN_THIS_QUESTION,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        choice.setIsDeleted(true);
        choiceRepository.save(choice);
        question.getChoices().removeIf(c -> c.getChoiceId().equals(choiceUuid));
        List<Choice> remainingChoices = choiceRepository.findByQuestionAndIsDeletedOrderByChoiceOrderAsc(question, false);
        remainingChoices.sort(Comparator.comparingInt(Choice::getChoiceOrder));
        for (int i = 0; i < remainingChoices.size(); i++) {
            remainingChoices.get(i).setChoiceOrder(i + 1);
        }
        choiceRepository.saveAll(remainingChoices);
        question.setUpdatedBy(userId);
        questionRepository.save(question);
    }

}
