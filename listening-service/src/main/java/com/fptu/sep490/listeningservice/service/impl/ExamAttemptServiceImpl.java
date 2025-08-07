package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.DateTimeUtils;
import com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.listeningservice.model.specification.ExamAttemptSpecification;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.ExamAttemptService;
import com.fptu.sep490.listeningservice.service.ListeningTaskService;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ExamAttemptServiceImpl implements ExamAttemptService {
    ListeningExamRepository listeningExamRepository;
    QuestionRepository questionRepository;
    ExamAttemptRepository examAttemptRepository;
    ObjectMapper objectMapper;
    ChoiceRepository choiceRepository;
    DragItemRepository dragItemRepository;
    Helper helper;
    ListeningTaskService listeningTaskService;
    AttemptRepository attemptRepository;
    ListeningTaskRepository listeningTaskRepository;

    @Transactional
    @Override
    public CreateExamAttemptResponse createExamAttempt(String urlSlug, HttpServletRequest request){
        ListeningExam originalExam = listeningExamRepository
                .findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(urlSlug)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.LISTENING_EXAM_NOT_FOUND,
                        Constants.ErrorCode.LISTENING_EXAM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        ListeningExam currentExam = listeningExamRepository.findCurrentChildByParentId(originalExam.getListeningExamId())
                .orElse(originalExam);

        String userId = helper.getUserIdFromToken(request);
        UserInformationResponse user = helper.getUserInformationResponse(userId);

        //create examAttempt
        ExamAttempt examAttempt = ExamAttempt.builder()
                .duration(null) // Initialize duration to null, will be updated later
                .totalPoint(null) // Initialize totalPoint to null, will be updated later
                .listeningExam(currentExam)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        //save examAttempt
        examAttempt = examAttemptRepository.saveAndFlush(examAttempt);

        CreateExamAttemptResponse.ListeningExamResponse listeningExamResponse = CreateExamAttemptResponse.ListeningExamResponse.builder()
                .listeningExamId(currentExam.getListeningExamId())
                .listeningExamName(currentExam.getExamName())
                .listeningExamDescription(currentExam.getExamDescription())
                .urlSlug(currentExam.getUrlSlug())
                .listeningTaskIdPart1(listeningTaskService.fromListeningTask(currentExam.getPart1().getTaskId().toString()))
                .listeningTaskIdPart2(listeningTaskService.fromListeningTask(currentExam.getPart2().getTaskId().toString()))
                .listeningTaskIdPart3(listeningTaskService.fromListeningTask(currentExam.getPart3().getTaskId().toString()))
                .listeningTaskIdPart4(listeningTaskService.fromListeningTask(currentExam.getPart4().getTaskId().toString()))
                .build();
        return CreateExamAttemptResponse.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .urlSlug(currentExam.getUrlSlug())
                .createdBy(user)
                .createdAt(examAttempt.getCreatedAt().toString())
                .listeningExam(listeningExamResponse)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public ExamAttemptGetDetail getExamAttemptById(String examAttemptId, HttpServletRequest request) throws JsonProcessingException {
        UUID attemptId = UUID.fromString(examAttemptId);
        ExamAttempt examAttempt = examAttemptRepository.findById(attemptId).orElseThrow(
                () -> new AppException(
                        Constants.ErrorCodeMessage.EXAM_ATTEMPT_NOT_FOUND,
                        Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                )
        );

        String userId = helper.getUserIdFromToken(request);
        if (!examAttempt.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.EXAM_ATTEMPT_NOT_FOUND,
                    Constants.ErrorCode.EXAM_ATTEMPT_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        if (examAttempt.getHistory() == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.EXAM_ATTEMPT_NOT_SUBMIT,
                    Constants.ErrorCode.EXAM_ATTEMPT_NOT_SUBMIT,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        ExamAttemptHistory history = objectMapper.readValue(examAttempt.getHistory(), ExamAttemptHistory.class);

        List<ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse> taskResponses = listeningTaskService.fromExamAttemptHistory(history);
        taskResponses = taskResponses.stream()
                .sorted(Comparator.comparing(ExamAttemptGetDetail.ListeningExamResponse.ListeningTaskResponse::partNumber))
                .toList();
        ExamAttemptGetDetail.ListeningExamResponse readingExamResponse = ExamAttemptGetDetail.ListeningExamResponse.builder()
                .listeningExamId(examAttempt.getListeningExam().getListeningExamId())
                .listeningExamName(examAttempt.getListeningExam().getExamName())
                .listeningExamDescription(examAttempt.getListeningExam().getExamDescription())
                .urlSlug(examAttempt.getListeningExam().getUrlSlug())
                .listeningTaskIdPart1(taskResponses.get(0))
                .listeningTaskIdPart2(taskResponses.get(1))
                .listeningTaskIdPart3(taskResponses.get(2))
                .listeningTaskIdPart4(taskResponses.get(3))
                .build();
        return ExamAttemptGetDetail.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .readingExam(readingExamResponse)
                .duration(examAttempt.getDuration().longValue())
                .totalQuestion(examAttempt.getTotalPoint())
                .createdBy(helper.getUserInformationResponse(examAttempt.getCreatedBy()))
                .updatedBy(helper.getUserInformationResponse(examAttempt.getUpdatedBy()))
                .createdAt(examAttempt.getCreatedAt().toString())
                .updatedAt(examAttempt.getUpdatedAt().toString())
                .answers(history.getUserAnswers())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserGetHistoryExamAttemptResponse> getListExamHistory(
            int page,
            int size,
            String listeningExamName,
            String sortBy,
            String sortDirection,
            HttpServletRequest request
    ) {

        Pageable pageable = PageRequest.of(page, size);
        var spec = ExamAttemptSpecification.byConditions(
                listeningExamName,
                sortBy,
                sortDirection,
                helper.getUserIdFromToken(request)
        );

        Page<ExamAttempt> examAttemptsResult = examAttemptRepository.findAll(spec, pageable);

        List<ExamAttempt> examAttempts = examAttemptsResult.getContent();

        List<UserGetHistoryExamAttemptResponse> list = examAttempts.stream().map(examAttempt -> {
            UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptListeningExamResponse listeningExamResponse =
                    UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptListeningExamResponse.builder()
                            .listeningExamId(examAttempt.getListeningExam().getListeningExamId())
                            .listeningExamName(examAttempt.getListeningExam().getExamName())
                            .listeningExamDescription(examAttempt.getListeningExam().getExamDescription())
                            .urlSlug(examAttempt.getListeningExam().getUrlSlug())
                            .build();

            return UserGetHistoryExamAttemptResponse.builder()
                    .examAttemptId(examAttempt.getExamAttemptId())
                    .listeningExam(listeningExamResponse)
                    .duration(examAttempt.getDuration())
                    .totalQuestion(examAttempt.getTotalPoint())
                    .createdBy(helper.getUserInformationResponse(examAttempt.getCreatedBy()))
                    .updatedBy(helper.getUserInformationResponse(examAttempt.getUpdatedBy()))
                    .createdAt(examAttempt.getCreatedAt().toString())
                    .updatedAt(examAttempt.getUpdatedAt().toString())
                    .build();
        }).toList();

        return new PageImpl<>(list, pageable, examAttemptsResult.getTotalElements());

    }

    @Override
    public SubmittedExamAttemptResponse submittedExam(String attemptId, ExamAttemptAnswersRequest answers, HttpServletRequest request) throws JsonProcessingException {

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
        Map<UUID, List<UUID>> groupMapDragItem = new HashMap<>();
        if(!CollectionUtils.isEmpty(answers.itemsIds())) {
            List<DragItem> items = dragItemRepository.findAllById(answers.itemsIds());
            List<UUID> groupIds = items.stream()
                    .map(i -> i.getQuestionGroup().getGroupId()).toList();
            Set<UUID> group = new HashSet<>(groupIds);
            group.forEach(groupId -> {
                List<UUID> ids = items.stream()
                        .filter(dragItem -> groupId.equals(dragItem.getQuestionGroup().getGroupId()))
                        .map(DragItem::getDragItemId)
                        .toList();

                groupMapDragItem.put(groupId, ids);
            });
        }



        // Convert user answers for mapping questions and answers
        Map<UUID, List<String>> userAnswers = answers.answers().stream()
                .collect(Collectors.toMap(
                        ExamAttemptAnswersRequest.ExamAnswerRequest::questionId,
                        ExamAttemptAnswersRequest.ExamAnswerRequest::selectedAnswers
                ));
        Map<UUID, List<UUID>> questionMapChoice = new HashMap<>();
        for (ExamAttemptAnswersRequest.ExamAnswerRequest answer : answers.answers()) {
            if(!CollectionUtils.isEmpty(answer.choiceIds())) {
                questionMapChoice.put(answer.questionId(), answer.choiceIds());
            }
        }
        ExamAttemptHistory examAttemptHistory = ExamAttemptHistory.builder()
                .taskId(answers.taskId())
                .questionGroupIds(answers.questionGroupIds())
                .userAnswers(userAnswers)
                .groupMapItems(groupMapDragItem)
                .questionMapChoices(questionMapChoice)
                .questionIds(questionIds)
                .build();
        examAttempt.setHistory(objectMapper.writeValueAsString(examAttemptHistory));

        int points = 0;
        List<SubmittedExamAttemptResponse.ResultSet> resultSets = new ArrayList<>();
        for(Question question : questions) {

            List<String> userSelectedAnswers = userAnswers.get(question.getQuestionId());
            if(userSelectedAnswers == null) {
                continue;
            }

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                SubmittedExamAttemptResponse.ResultSet result = checkMultipleChoiceQuestion(question, userSelectedAnswers);
                points += result.isCorrect() ? question.getPoint() : 0;
                resultSets.add(result);
            }
            if (question.getQuestionType() == QuestionType.FILL_IN_THE_BLANKS) {
                SubmittedExamAttemptResponse.ResultSet result = SubmittedExamAttemptResponse.ResultSet.builder()
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
                SubmittedExamAttemptResponse.ResultSet result = SubmittedExamAttemptResponse.ResultSet.builder()
                        .userAnswer(userSelectedAnswers)
                        .explanation(question.getExplanation())
                        .correctAnswer(List.of(question.getCorrectAnswerForMatching()))
                        .isCorrect(false)
                        .questionIndex(question.getQuestionOrder())
                        .build();
                if(question.getCorrectAnswerForMatching().equalsIgnoreCase(userSelectedAnswers.getFirst())) {
                    result.setCorrect(true);
                    points += question.getPoint();
                }
                resultSets.add(result);
            }

            if( question.getQuestionType() == QuestionType.DRAG_AND_DROP) {
                SubmittedExamAttemptResponse.ResultSet result = SubmittedExamAttemptResponse.ResultSet.builder()
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
        return SubmittedExamAttemptResponse.builder()
                .duration(examAttempt.getDuration().longValue())
                .resultSets(resultSets)
                .build();

    }

    private SubmittedExamAttemptResponse.ResultSet checkMultipleChoiceQuestion(Question question, List<String> userSelectedAnswers) {
        // Convert user selected answers to UUIDs
        List<UUID> answerChoice = userSelectedAnswers.stream()
                .map(UUID::fromString)
                .toList();
        List<Choice> correctAnswers = new ArrayList<>();
        List<String> userAnswers= choiceRepository.getChoicesByIds(answerChoice);
        SubmittedExamAttemptResponse.ResultSet resultSet = SubmittedExamAttemptResponse.ResultSet.builder()
                .questionIndex(question.getQuestionOrder())
                .userAnswer(userAnswers)
                .explanation(question.getExplanation())
                .build();
        if(question.getIsOriginal()) {
            List<Choice> originalChoice = choiceRepository.getOriginalChoiceByOriginalQuestion(question.getQuestionId());
            correctAnswers = choiceRepository.getCurrentCorrectChoice(originalChoice.stream().map(
                    Choice::getChoiceId
            ).toList());
        } else {
            List<Choice> originalChoice = choiceRepository.getOriginalChoiceByOriginalQuestion(question.getParent().getQuestionId());
            correctAnswers = choiceRepository.getCurrentCorrectChoice(originalChoice.stream().map(
                    Choice::getChoiceId
            ).toList());
        }

        List<UUID> userChoice = answerChoice;
        List<String> correctLabel = new ArrayList<>();
        for(Choice correctAnswer : correctAnswers) {

            if(userChoice.contains(correctAnswer.getChoiceId())) {
                correctLabel.add(correctAnswer.getLabel());
            }
        }
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

    @Override
    public OverviewProgress getOverViewProgress(OverviewProgressReq body, String token) {
        String userId = helper.getUserIdFromToken(token);

        List<ExamAttempt> exams = examAttemptRepository.findAllByUserId(userId);
        Integer numberOfExams = listeningExamRepository.numberOfActiveExams();

        List<Attempt> tasks = attemptRepository.findAllByUserId(userId);
        Integer numberOfTasks = listeningTaskRepository.numberOfPublishedTasks();

        OverviewProgress overviewProgress = new OverviewProgress();
        overviewProgress.setExam(exams.size());
        overviewProgress.setTask(tasks.size());
        overviewProgress.setTotalExams(numberOfExams);
        overviewProgress.setTotalTasks(numberOfTasks);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = DateTimeUtils.calculateStartDateFromTimeFrame(body.getTimeFrame());

// 3. Duyệt qua các bài thi và cập nhật lastLearningDate
        double totalScore = 0.0;
        int numberOfExamsInTimeFrame = 0;
        int numberOfTasksInTimeFrame = 0;
        for (ExamAttempt exam : exams) {
            LocalDateTime createdAt = exam.getCreatedAt();
            if ((createdAt.isAfter(startDate) || createdAt.isEqual(startDate)) && createdAt.isBefore(endDate)) {
                LocalDateTime lastDateStr = overviewProgress.getLastLearningDate();
                totalScore += exam.getTotalPoint() != null ? exam.getTotalPoint() : 0;
                numberOfExamsInTimeFrame++;
                if (lastDateStr == null) {
                    overviewProgress.setLastLearningDate(createdAt); // dùng format mặc định
                } else {
                    if (createdAt.isAfter(lastDateStr)) {
                        overviewProgress.setLastLearningDate(createdAt);
                    }
                }
            }
        }
        for (Attempt task : tasks) {
            LocalDateTime createdAt = task.getCreatedAt();
            if ((createdAt.isAfter(startDate) || createdAt.isEqual(startDate)) && createdAt.isBefore(endDate)) {
                LocalDateTime lastDateStr = overviewProgress.getLastLearningDate();
                numberOfTasksInTimeFrame++;
                if (lastDateStr == null) {
                    overviewProgress.setLastLearningDate(createdAt); // dùng format mặc định
                } else {
                    if (createdAt.isAfter(lastDateStr)) {
                        overviewProgress.setLastLearningDate(createdAt);
                    }
                }
            }
        }

        overviewProgress.setAverageBandInTimeFrame(
                numberOfExamsInTimeFrame > 0 ? totalScore / numberOfExamsInTimeFrame : null
        );

        overviewProgress.setNumberOfExamsInTimeFrame(numberOfExamsInTimeFrame);
        overviewProgress.setNumberOfTasksInTimeFrame(numberOfTasksInTimeFrame);
        return overviewProgress;
    }

    @Override
    public List<LineChartData> getBandChart(LineChartReq body, String token) {
        List<ExamAttempt> exams = examAttemptRepository.findByUserAndDateRange(helper.getUserIdFromToken(token),
                body.getStartDate() != null ? body.getStartDate().atStartOfDay() : null,
                body.getEndDate() != null ? body.getEndDate().atTime(LocalTime.MAX) : null);

        if (exams == null || exams.isEmpty()) {
            return Collections.emptyList(); // Trả về danh sách rỗng
        }

        LocalDate startDate = exams.getFirst().getCreatedAt().toLocalDate();
        // 2. Grouping + averaging
        Map<LocalDate, Double> avgByPeriod = exams.stream()
                .collect(Collectors.groupingBy(
                        exam -> DateTimeUtils.normalize(
                                exam.getCreatedAt().toLocalDate(),
                                body.getTimeFrame(),
                                startDate),
                        TreeMap::new,
                        Collectors.averagingDouble(e -> e.getTotalPoint().doubleValue())
                ));

        // 3. Chuyển thành LineChartData và sort
        return avgByPeriod.entrySet().stream()
                .map(e -> LineChartData.builder()
                        .date(e.getKey())
                        .value(e.getValue())
                        .build())
                .sorted(Comparator.comparing(LineChartData::getDate))
                .collect(Collectors.toList());
    }

}
