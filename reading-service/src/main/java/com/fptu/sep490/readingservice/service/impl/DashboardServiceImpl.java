package com.fptu.sep490.readingservice.service.impl;

import com.fptu.sep490.readingservice.repository.AttemptRepository;
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

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    ReadingPassageRepository readingPassageRepository;
    ReadingExamRepository readingExamRepository;
    AttemptRepository attemptRepository;

    @Override
    @Transactional
    public DataStats getDataStats() {
        int numberOfTasks = readingExamRepository.getNumberOfExams();
        int numberOfExams = readingPassageRepository.getNumberOfPassages();
        int numberOfAttempts = attemptRepository.getNumberOfAttempts();
        return DataStats.builder()
                .numberOfTasks(numberOfTasks)
                .numberOfExams(numberOfExams)
                .build();
    }
}
