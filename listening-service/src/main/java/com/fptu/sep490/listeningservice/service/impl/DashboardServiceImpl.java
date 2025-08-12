package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStatsWrong;
import com.fptu.sep490.listeningservice.model.UserInBranch;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.DashboardService;
import com.fptu.sep490.listeningservice.viewmodel.response.DataStats;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    ListeningTaskRepository listeningTaskRepository;
    ListeningExamRepository listeningExamRepository;
    AttemptRepository attemptRepository;
    ExamAttemptRepository examAttemptRepository;
    ReportDataRepository reportDataRepository;
    Helper helper;
    @Override
    public DataStats getDataStats() {
        int numberOfTasks = listeningTaskRepository.getNumberOfTasks();
        int numberOfExams = listeningExamRepository.getNumberOfExams();
        int numberOfAttempts = attemptRepository.getNumberOfAttempts();
        int numberOfExamAttempts = examAttemptRepository.getNumberOfExamAttempts();
        List<UserInBranch> userInBranchAvg = examAttemptRepository.getNumberOfUsersInBranchAvg();
        List<UserInBranch> userInBranchHighest = examAttemptRepository.getNumberOfUsersInBranchHighest();
        List<ReportQuestionTypeStats> questionTypeStats = reportDataRepository.countCorrectByQuestionType(null, null);
        List<ReportQuestionTypeStatsWrong> questionTypeStatsWrong = reportDataRepository.countWrongByQuestionType(null, null);
        return DataStats.builder()
                .numberOfTasks(numberOfTasks)
                .numberOfExams(numberOfExams)
                .examAttempted(numberOfExamAttempts)
                .taskAttempted(numberOfAttempts)
                .userInBranchAvg(userInBranchAvg)
                .userInBranchHighest(userInBranchHighest)
                .questionTypeStats(questionTypeStats)
                .questionTypeStatsWrong(questionTypeStatsWrong)
                .build();
    }

    @Override
    public List<ReportQuestionTypeStats> getQuestionTypeStats(LocalDate fromDate, LocalDate toDate, HttpServletRequest request) {
        return reportDataRepository.countCorrectByQuestionType(fromDate, toDate);
    }

    @Override
    public List<ReportQuestionTypeStatsWrong> getQuestionTypeStatsWrong(LocalDate fromDate, LocalDate toDate, HttpServletRequest request) {
        return reportDataRepository.countWrongByQuestionType(fromDate, toDate);
    }
}
