package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.Choice;
import com.fptu.sep490.readingservice.model.ExamAttempt;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.readingservice.model.json.UserAnswer;
import com.fptu.sep490.readingservice.repository.ChoiceRepository;
import com.fptu.sep490.readingservice.repository.ExamAttemptRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.response.SubmittedAttemptResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ExamAttemptServiceImpl implements ExamAttemptService {
    QuestionRepository questionRepository;
    ExamAttemptRepository examAttemptRepository;
    ObjectMapper objectMapper;
    ChoiceRepository choiceRepository;
    @Override
    public SubmittedAttemptResponse submittedExam(String attemptId, ExamAttemptAnswersRequest answers, HttpServletRequest request) throws JsonProcessingException {

        ExamAttempt examAttempt = examAttemptRepository.findById(UUID.fromString(attemptId)).orElseThrow(
                () -> new AppException(
                        Constants.ErrorCodeMessage.EXAM_ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                )
        );
        examAttempt.setDuration(answers.duration());
        List<UUID> questionIds = answers.answers().stream()
                .map(ExamAttemptAnswersRequest.ExamAnswerRequest::questionId)
                .toList();
        List<Question> questions = questionRepository.findQuestionsByIds(questionIds);

        // Save version of exam attempt
        ExamAttemptHistory examAttemptHistory = ExamAttemptHistory.builder()
                .passageId(answers.passageId())
                .questionGroupIds(answers.questionGroupIds())
                .questionIds(questionIds)
                .build();
        examAttempt.setHistory(objectMapper.writeValueAsString(examAttemptHistory));

        // Convert user answers for mapping questions and answers
        Map<UUID, List<String>> userAnswers = answers.answers().stream()
                .collect(Collectors.toMap(
                        ExamAttemptAnswersRequest.ExamAnswerRequest::questionId,
                        ExamAttemptAnswersRequest.ExamAnswerRequest::selectedAnswers
                ));

        int points = 0;
        List<SubmittedAttemptResponse.ResultSet> resultSets = new ArrayList<>();
        for(Question question : questions) {
            List<String> userSelectedAnswers = userAnswers.get(question.getQuestionId());
            if(userSelectedAnswers == null) {
                continue;
            }

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                SubmittedAttemptResponse.ResultSet result = checkMultipleChoiceQuestion(question, userSelectedAnswers);
                points += result.isCorrect() ? question.getPoint() : 0;
                resultSets.add(result);
            }
            if (question.getQuestionType() == QuestionType.FILL_IN_THE_BLANKS) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(userSelectedAnswers)
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getCorrectAnswer()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if(question.getCorrectAnswer().equalsIgnoreCase(userSelectedAnswers.getFirst())) {
                    result.setCorrect(true);
                    points += question.getPoint();
                }
                resultSets.add(result);
            }

            if (question.getQuestionType() == QuestionType.MATCHING) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(userSelectedAnswers)
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getCorrectAnswer()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if(question.getCorrectAnswer().equalsIgnoreCase(userSelectedAnswers.getFirst())) {
                    result.setCorrect(true);
                    points += question.getPoint();
                }
                resultSets.add(result);
            }

            if( question.getQuestionType() == QuestionType.DRAG_AND_DROP) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
                        .userAnswer(userSelectedAnswers)
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getDragItem().getContent()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if(question.getDragItem().getDragItemId().equals(UUID.fromString(userSelectedAnswers.getFirst()))) {
                    result.setCorrect(true);
                }
                resultSets.add(result);
            }


        }
        examAttempt.setTotalPoint(points);

        examAttempt = examAttemptRepository.save(examAttempt);
        return SubmittedAttemptResponse.builder()
                .duration(examAttempt.getDuration().longValue())
                .resultSets(resultSets)
                .build();

    }

    private SubmittedAttemptResponse.ResultSet checkMultipleChoiceQuestion(Question question, List<String> userSelectedAnswers) {
        // Convert user selected answers to UUIDs
        List<UUID> answerChoice = userSelectedAnswers.stream()
                .map(UUID::fromString)
                .toList();
        List<Choice> correctAnswers = new ArrayList<>();
        List<String> userAnswers= choiceRepository.getChoicesByIds(answerChoice);
        SubmittedAttemptResponse.ResultSet resultSet = SubmittedAttemptResponse.ResultSet.builder()
                .questionIndex(question.getQuestionOrder())
                .userAnswer(userAnswers)

                .explanation(question.getExplanation())
                .build();
        if(question.getIsOriginal()) {
            List<Choice> originalChoice = choiceRepository.getOriginalChoiceByOriginalQuestion(question.getQuestionId());
            correctAnswers = choiceRepository.getCurrentCorrectChoice(originalChoice);
        } else {
            List<Choice> originalChoice = choiceRepository.getOriginalChoiceByOriginalQuestion(question.getParent().getQuestionId());
            correctAnswers = choiceRepository.getCurrentCorrectChoice(originalChoice);
        }

        List<UUID> userChoice = answerChoice;
        List<String> correctLabel = new ArrayList<>();
        for(Choice correctAnswer : correctAnswers) {

            if( userChoice.contains(correctAnswer.getChoiceId())) {
                correctLabel.add(correctAnswer.getLabel());
            }
        }
        resultSet.setCorrectAnswer(correctLabel);
        int numberOfCorrect = 0;
        for(String userAnswer : userAnswers) {
            if(correctLabel.contains(userAnswer)) {
                numberOfCorrect++;
            }
        }
        boolean isCorrect = numberOfCorrect == correctAnswers.size();
        resultSet.setCorrect(isCorrect);
        return resultSet;
    }
}
