package com.fptu.sep490.readingservice.service.impl;

import com.fptu.sep490.readingservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.readingservice.model.ReportQuestionTypeStatsWrong;
import com.fptu.sep490.readingservice.model.UserInBranch;
import com.fptu.sep490.readingservice.repository.*;
import com.fptu.sep490.readingservice.viewmodel.response.DataStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardServiceImplTest {

    @Mock
    ReadingPassageRepository readingPassageRepository;
    @Mock
    ReadingExamRepository readingExamRepository;
    @Mock
    AttemptRepository attemptRepository;
    @Mock
    ExamAttemptRepository examAttemptRepository;
    @Mock
    ReportDataRepository reportDataRepository;

    DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DashboardServiceImpl(
                readingPassageRepository,
                readingExamRepository,
                attemptRepository,
                examAttemptRepository,
                reportDataRepository
        );
    }

    @Test
    void getDataStats_returnsAggregatedData() {
        when(readingExamRepository.getNumberOfExams()).thenReturn(7);
        when(readingPassageRepository.getNumberOfPassages()).thenReturn(13);
        when(attemptRepository.getNumberOfAttempts()).thenReturn(21);
        when(examAttemptRepository.getNumberOfExamAttempts()).thenReturn(5);

        List<UserInBranch> avg = List.of(mock(UserInBranch.class));
        List<UserInBranch> highest = List.of(mock(UserInBranch.class));
        when(readingExamRepository.getNumberOfUsersInBranchAvg()).thenReturn(avg);
        when(readingExamRepository.getNumberOfUsersInBranchHighest()).thenReturn(highest);

        List<ReportQuestionTypeStats> stats = List.of(mock(ReportQuestionTypeStats.class));
        List<ReportQuestionTypeStatsWrong> wrong = List.of(mock(ReportQuestionTypeStatsWrong.class));
        when(reportDataRepository.countCorrectByQuestionType(null, null)).thenReturn(stats);
        when(reportDataRepository.countWrongByQuestionType(null, null)).thenReturn(wrong);

        DataStats result = service.getDataStats();

        assertEquals(13, result.numberOfTasks());
        assertEquals(7, result.numberOfExams());
        assertEquals(21, result.taskAttempted());
        assertEquals(5, result.examAttempted());
        assertEquals(avg, result.userInBranchAvg());
        assertEquals(highest, result.userInBranchHighest());
        assertEquals(stats, result.questionTypeStats());
        assertEquals(wrong, result.questionTypeStatsWrong());

        verify(readingExamRepository).getNumberOfExams();
        verify(readingPassageRepository).getNumberOfPassages();
        verify(attemptRepository).getNumberOfAttempts();
        verify(examAttemptRepository).getNumberOfExamAttempts();
        verify(readingExamRepository).getNumberOfUsersInBranchAvg();
        verify(readingExamRepository).getNumberOfUsersInBranchHighest();
        verify(reportDataRepository).countCorrectByQuestionType(null, null);
        verify(reportDataRepository).countWrongByQuestionType(null, null);
    }

    @Test
    void getQuestionTypeStats_delegatesToRepository() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();
        List<ReportQuestionTypeStats> stats = List.of(mock(ReportQuestionTypeStats.class));
        when(reportDataRepository.countCorrectByQuestionType(from, to)).thenReturn(stats);

        List<ReportQuestionTypeStats> result = service.getQuestionTypeStats(from, to);
        assertEquals(stats, result);
        verify(reportDataRepository).countCorrectByQuestionType(from, to);
    }

    @Test
    void getQuestionTypeStatsWrong_delegatesToRepository() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        List<ReportQuestionTypeStatsWrong> wrong = List.of(mock(ReportQuestionTypeStatsWrong.class));
        when(reportDataRepository.countWrongByQuestionType(from, to)).thenReturn(wrong);

        List<ReportQuestionTypeStatsWrong> result = service.getQuestionTypeStatsWrong(from, to);
        assertEquals(wrong, result);
        verify(reportDataRepository).countWrongByQuestionType(from, to);
    }
}

