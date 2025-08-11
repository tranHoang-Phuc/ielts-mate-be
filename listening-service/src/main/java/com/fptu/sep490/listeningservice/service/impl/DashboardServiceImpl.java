package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.listeningservice.model.UserInBranch;
import com.fptu.sep490.listeningservice.repository.AttemptRepository;
import com.fptu.sep490.listeningservice.repository.ExamAttemptRepository;
import com.fptu.sep490.listeningservice.repository.ListeningExamRepository;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.service.DashboardService;
import com.fptu.sep490.listeningservice.viewmodel.response.DataStats;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public DataStats getDataStats() {
        int numberOfTasks = listeningTaskRepository.getNumberOfTasks();
        int numberOfExams = listeningExamRepository.getNumberOfExams();
        int numberOfAttempts = attemptRepository.getNumberOfAttempts();
        int numberOfExamAttempts = examAttemptRepository.getNumberOfExamAttempts();
        List<UserInBranch> userInBranchAvg = examAttemptRepository.getNumberOfUsersInBranchAvg();
        List<UserInBranch> userInBranchHighest = examAttemptRepository.getNumberOfUsersInBranchHighest();

        return DataStats.builder()
                .numberOfTasks(numberOfTasks)
                .numberOfExams(numberOfExams)
                .examAttempted(numberOfExamAttempts)
                .taskAttempted(numberOfAttempts)
                .userInBranchAvg(userInBranchAvg)
                .userInBranchHighest(userInBranchHighest)
                .build();
    }
}
