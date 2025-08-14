package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStats;
import com.fptu.sep490.listeningservice.model.ReportQuestionTypeStatsWrong;
import com.fptu.sep490.listeningservice.model.UserInBranch;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.viewmodel.response.DataStats;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class DashboardServiceImplTest {

	@InjectMocks
	DashboardServiceImpl dashboardService;
	@Mock ListeningTaskRepository listeningTaskRepository;
	@Mock ListeningExamRepository listeningExamRepository;
	@Mock AttemptRepository attemptRepository;
	@Mock ExamAttemptRepository examAttemptRepository;
	@Mock ReportDataRepository reportDataRepository;
	@Mock Helper helper;
	@Mock HttpServletRequest request;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void getDataStats_returnsAggregatedData() {
		// Arrange
		when(listeningTaskRepository.getNumberOfTasks()).thenReturn(5);
		when(listeningExamRepository.getNumberOfExams()).thenReturn(3);
		when(attemptRepository.getNumberOfAttempts()).thenReturn(7);
		when(examAttemptRepository.getNumberOfExamAttempts()).thenReturn(4);
		List<UserInBranch> avg = List.of(mock(UserInBranch.class));
		List<UserInBranch> highest = List.of(mock(UserInBranch.class));
		List<ReportQuestionTypeStats> correct = List.of(mock(ReportQuestionTypeStats.class));
		List<ReportQuestionTypeStatsWrong> wrong = List.of(mock(ReportQuestionTypeStatsWrong.class));
		when(examAttemptRepository.getNumberOfUsersInBranchAvg()).thenReturn(avg);
		when(examAttemptRepository.getNumberOfUsersInBranchHighest()).thenReturn(highest);
		when(reportDataRepository.countCorrectByQuestionType(null, null)).thenReturn(correct);
		when(reportDataRepository.countWrongByQuestionType(null, null)).thenReturn(wrong);

		// Act
		DataStats res = dashboardService.getDataStats();

		// Assert
		assertEquals(5, res.numberOfTasks());
		assertEquals(3, res.numberOfExams());
		assertEquals(7, res.taskAttempted());
		assertEquals(4, res.examAttempted());
		assertSame(avg, res.userInBranchAvg());
		assertSame(highest, res.userInBranchHighest());
		assertSame(correct, res.questionTypeStats());
		assertSame(wrong, res.questionTypeStatsWrong());
	}

	@Test
	void getQuestionTypeStats_delegatesToRepository() {
		LocalDate from = LocalDate.of(2025, 1, 1);
		LocalDate to = LocalDate.of(2025, 1, 31);
		List<ReportQuestionTypeStats> expected = List.of(mock(ReportQuestionTypeStats.class));
		when(reportDataRepository.countCorrectByQuestionType(from, to)).thenReturn(expected);

		List<ReportQuestionTypeStats> actual = dashboardService.getQuestionTypeStats(from, to, request);

		assertSame(expected, actual);
		verify(reportDataRepository).countCorrectByQuestionType(eq(from), eq(to));
	}

	@Test
	void getQuestionTypeStatsWrong_delegatesToRepository() {
		LocalDate from = LocalDate.of(2025, 2, 1);
		LocalDate to = LocalDate.of(2025, 2, 28);
		List<ReportQuestionTypeStatsWrong> expected = List.of(mock(ReportQuestionTypeStatsWrong.class));
		when(reportDataRepository.countWrongByQuestionType(from, to)).thenReturn(expected);

		List<ReportQuestionTypeStatsWrong> actual = dashboardService.getQuestionTypeStatsWrong(from, to, request);

		assertSame(expected, actual);
		verify(reportDataRepository).countWrongByQuestionType(eq(from), eq(to));
	}
}
