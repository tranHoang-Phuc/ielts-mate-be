package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.listeningservice.repository.ListeningExamRepository;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.service.DashboardService;
import com.fptu.sep490.listeningservice.viewmodel.response.DataStats;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    ListeningTaskRepository listeningTaskRepository;
    ListeningExamRepository listeningExamRepository;


    @Override
    public DataStats getDataStats() {
        int numberOfTasks = listeningTaskRepository.getNumberOfTasks();
        int numberOfExams = listeningExamRepository.getNumberOfExams();

        log.info("Number of tasks: {}, Number of exams: {}", numberOfTasks, numberOfExams);

        return DataStats.builder()
                .numberOfTasks(numberOfTasks)
                .numberOfExams(numberOfExams)
                .build();
    }
}
