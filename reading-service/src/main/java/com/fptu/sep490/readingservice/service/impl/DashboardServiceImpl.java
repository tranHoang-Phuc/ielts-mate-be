package com.fptu.sep490.readingservice.service.impl;

import com.fptu.sep490.readingservice.viewmodel.response.UserInBranch;
import com.fptu.sep490.readingservice.repository.AttemptRepository;
import com.fptu.sep490.readingservice.repository.ExamAttemptRepository;
import com.fptu.sep490.readingservice.repository.ReadingExamRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.service.DashboardService;
import com.fptu.sep490.readingservice.viewmodel.response.DataStats;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    ReadingPassageRepository readingPassageRepository;
    ReadingExamRepository readingExamRepository;
    AttemptRepository attemptRepository;
    ExamAttemptRepository examAttemptRepository;

    @Override
    @Transactional
    public DataStats getDataStats() {
        int numberOfExams = readingExamRepository.getNumberOfExams();
        int numberOfTasks = readingPassageRepository.getNumberOfPassages();
        int numberOfAttempts = attemptRepository.getNumberOfAttempts();
        int numberOfExamAttempts = examAttemptRepository.getNumberOfExamAttempts();
        List<UserInBranch> userInBranchAvg = readingExamRepository.getNumberOfUsersInBranchAvg();
        List<UserInBranch> userInBranchHighest = readingExamRepository.getNumberOfUsersInBranchHighest();
        return DataStats.builder()
                .numberOfTasks(numberOfTasks)
                .numberOfExams(numberOfExams)
                .taskAttempted(numberOfAttempts)
                .examAttempted(numberOfExamAttempts)
                .userInBranchAvg(userInBranchAvg)
                .userInBranchHighest(userInBranchHighest)
                .build();
    }
}
