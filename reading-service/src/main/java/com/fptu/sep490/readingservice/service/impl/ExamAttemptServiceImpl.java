package com.fptu.sep490.readingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.DateTimeUtils;
import com.fptu.sep490.commonlibrary.viewmodel.request.LineChartReq;
import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.LineChartData;
import com.fptu.sep490.commonlibrary.viewmodel.response.feign.OverviewProgress;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.model.json.ExamAttemptHistory;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.repository.ChoiceRepository;
import com.fptu.sep490.readingservice.repository.ExamAttemptRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.repository.ReadingExamRepository;
import com.fptu.sep490.readingservice.repository.specification.ExamAttemptSpecifications;
import com.fptu.sep490.readingservice.service.ExamAttemptService;
import com.fptu.sep490.readingservice.service.PassageService;
import com.fptu.sep490.readingservice.viewmodel.request.ExamAttemptAnswersRequest;
import com.fptu.sep490.readingservice.viewmodel.response.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ExamAttemptServiceImpl implements ExamAttemptService {
    QuestionRepository questionRepository;
    ExamAttemptRepository examAttemptRepository;
    ObjectMapper objectMapper;
    ChoiceRepository choiceRepository;
    DragItemRepository  dragItemRepository;
    Helper helper;
    PassageService passageService;
    ReadingExamRepository readingExamRepository;
    AttemptRepository attemptRepository;
    ReadingPassageRepository readingPassageRepository;
    ReportDataRepository reportDataRepository;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    @NonFinal
    String issuerUri;

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

        List<ReportData> reportData = new ArrayList<>();

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
                .passageId(answers.passageId())
                .questionGroupIds(answers.questionGroupIds())
                .userAnswers(userAnswers)
                .groupMapItems(groupMapDragItem)
                .questionMapChoices(questionMapChoice)
                .questionIds(questionIds)
                .build();
        examAttempt.setHistory(objectMapper.writeValueAsString(examAttemptHistory));

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
                reportData.add(ReportData.builder()
                                .questionType(question.getQuestionType())
                                .questionId(question.getQuestionId())
                                .isCorrect(points > 0)
                        .build());
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
                reportData.add(ReportData.builder()
                        .questionType(question.getQuestionType())
                        .questionId(question.getQuestionId())
                        .isCorrect(points > 0)
                        .build());
            }

            if (question.getQuestionType() == QuestionType.MATCHING) {
                SubmittedAttemptResponse.ResultSet result = SubmittedAttemptResponse.ResultSet.builder()
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
                reportData.add(ReportData.builder()
                        .questionType(question.getQuestionType())
                        .questionId(question.getQuestionId())
                        .isCorrect(points > 0)
                        .build());
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
                reportData.add(ReportData.builder()
                        .questionType(question.getQuestionType())
                        .questionId(question.getQuestionId())
                        .isCorrect(points > 0)
                        .build());
            }


        }
        examAttempt.setTotalPoint(points);

        examAttempt = examAttemptRepository.save(examAttempt);
        reportDataRepository.saveAll(reportData);
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

    @Transactional
    @Override
    public CreateExamAttemptResponse createExamAttempt(String urlSlug, HttpServletRequest request) throws JsonProcessingException {
        //find reading exam by urlSlug and isOriginal=true and isDeleted = false, else throw error
        // 1. Tìm original exam
        ReadingExam originalExam = readingExamRepository
                .findByUrlSlugAndIsOriginalTrueAndIsDeletedFalse(urlSlug)
                .orElseThrow(() -> new AppException(
                                Constants.ErrorCodeMessage.READING_EXAM_NOT_FOUND,
                                Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        //get reading exam has parent.readingExamId = readingExamId and isCurrent=true
        // 2. Lấy bản current, nếu ko có current bắn lỗi
        ReadingExam currentExam = readingExamRepository.findCurrentChildByParentId(originalExam.getReadingExamId())
                .orElse(originalExam);

        String userId = helper.getUserIdFromToken(request);
        UserInformationResponse user = helper.getUserInformationResponse(userId);

        //create examAttempt
        ExamAttempt examAttempt = ExamAttempt.builder()
                .duration(null) // Initialize duration to null, will be updated later
                .totalPoint(null) // Initialize totalPoint to null, will be updated later
                .readingExam(currentExam)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        //save examAttempt
        examAttempt = examAttemptRepository.saveAndFlush(examAttempt);

        //create CreateExamAttemptResponse

        CreateExamAttemptResponse.ReadingExamResponse readingExamResponse = CreateExamAttemptResponse.ReadingExamResponse.builder()
                .readingExamId(currentExam.getReadingExamId())
                .readingExamName(currentExam.getExamName())
                .readingExamDescription(currentExam.getExamDescription())
                .urlSlug(currentExam.getUrlSlug())
                .readingPassageIdPart1(passageService.fromReadingPassage(currentExam.getPart1().getPassageId().toString()))
                .readingPassageIdPart2(passageService.fromReadingPassage(currentExam.getPart2().getPassageId().toString()))
                .readingPassageIdPart3(passageService.fromReadingPassage(currentExam.getPart3().getPassageId().toString()))
                .build();

        return CreateExamAttemptResponse.builder()
                .examAttemptId(examAttempt.getExamAttemptId())
                .urlSlug(currentExam.getUrlSlug())
                .createdBy(user)
                .createdAt(examAttempt.getCreatedAt().toString())
                .readingExam(readingExamResponse)
                .build();

    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserGetHistoryExamAttemptResponse> getListExamHistory(
            int page,
            int size,
            String readingExamName,
            String sortBy,
            String sortDirection,
            HttpServletRequest request
    ) {

        Pageable pageable = PageRequest.of(page, size);
        var spec = ExamAttemptSpecifications.byConditions(
                readingExamName,
                sortBy,
                sortDirection,
                helper.getUserIdFromToken(request)
        );

        Page<ExamAttempt> examAttemptsResult = examAttemptRepository.findAll(spec, pageable);

        List<ExamAttempt> examAttempts = examAttemptsResult.getContent();

        List<UserGetHistoryExamAttemptResponse> list = examAttempts.stream().map(examAttempt -> {
            UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptReadingExamResponse readingExamResponse =
                    UserGetHistoryExamAttemptResponse.UserGetHistoryExamAttemptReadingExamResponse.builder()
                            .readingExamId(examAttempt.getReadingExam().getReadingExamId())
                            .readingExamName(examAttempt.getReadingExam().getExamName())
                            .readingExamDescription(examAttempt.getReadingExam().getExamDescription())
                            .urlSlug(examAttempt.getReadingExam().getUrlSlug())
                            .build();

            return UserGetHistoryExamAttemptResponse.builder()
                    .examAttemptId(examAttempt.getExamAttemptId())
                    .readingExam(readingExamResponse)
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

        List<ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse> passageResponses = passageService.fromExamAttemptHistory(history);
        passageResponses = passageResponses.stream()
                .sorted(Comparator.comparing(ExamAttemptGetDetail.ReadingExamResponse.ReadingPassageResponse::partNumber))
                .toList();
        ExamAttemptGetDetail.ReadingExamResponse readingExamResponse = ExamAttemptGetDetail.ReadingExamResponse.builder()
                .readingExamId(examAttempt.getReadingExam().getReadingExamId())
                .readingExamName(examAttempt.getReadingExam().getExamName())
                .readingExamDescription(examAttempt.getReadingExam().getExamDescription())
                .urlSlug(examAttempt.getReadingExam().getUrlSlug())
                .readingPassageIdPart1(passageResponses.get(0))
                .readingPassageIdPart2(passageResponses.get(1))
                .readingPassageIdPart3(passageResponses.get(2))
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

    @Override
    public OverviewProgress getOverViewProgress(OverviewProgressReq body, String token) {
        String userId = helper.getUserIdFromToken(token);

        List<ExamAttempt> exams = examAttemptRepository.findAllByUserId(userId);
        Integer numberOfExams = readingExamRepository.numberOfActiveExams();

        List<Attempt> tasks = attemptRepository.findAllByUserId(userId);
        Integer numberOfTasks = readingPassageRepository.numberOfPublishedPassages();

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
