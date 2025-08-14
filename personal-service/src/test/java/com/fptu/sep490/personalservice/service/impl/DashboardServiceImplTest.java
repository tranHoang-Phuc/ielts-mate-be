package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceImplTest {

	@Mock
	ReadingClient readingClient;
	@Mock
	ListeningClient listeningClient;
	@Mock
	Helper helper;

	DashboardServiceImpl service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new DashboardServiceImpl(readingClient, listeningClient, helper);
	}

	@Test
	void getDashboard_success_combinesStats() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getAccessToken(req)).thenReturn("tok");

		DataStats reading = DataStats.builder()
				.numberOfTasks(2)
				.numberOfExams(1)
				.userInBranchAvg(List.of(new UserInBranch("5.5", 10)))
				.questionTypeStats(List.of(new ReportQuestionTypeStats(1, 7)))
				.questionTypeStatsWrong(List.of(ReportQuestionTypeStatsWrong.builder().questionType(2).wrongCount(3).build()))
				.build();
		DataStats listening = DataStats.builder()
				.numberOfTasks(3)
				.numberOfExams(4)
				.userInBranchAvg(List.of(new UserInBranch("6.0", 5)))
				.questionTypeStats(List.of(new ReportQuestionTypeStats(3, 11)))
				.questionTypeStatsWrong(List.of(ReportQuestionTypeStatsWrong.builder().questionType(4).wrongCount(2).build()))
				.build();

		when(readingClient.getReadingStats(anyString()))
				.thenReturn(ResponseEntity.ok(BaseResponse.<DataStats>builder().data(reading).build()));
		when(listeningClient.getListeningStats(anyString()))
				.thenReturn(ResponseEntity.ok(BaseResponse.<DataStats>builder().data(listening).build()));

		CreatorDefaultDashboard dashboard = service.getDashboard(req);
		assertEquals(2, dashboard.numberOfReadingTasks());
		assertEquals(3, dashboard.numberOfListeningTasks());
		assertEquals(1, dashboard.numberOfReadingExams());
		assertEquals(4, dashboard.numberOfListeningExams());
		assertEquals(1, dashboard.userInAvgBranchScoreReading().size());
		assertEquals(1, dashboard.userInAvgBranchScoreListening().size());
		assertEquals(1, dashboard.questionTypeStatsReading().size());
		assertEquals(1, dashboard.questionTypeStatsListening().size());
		assertEquals(1, dashboard.questionTypeStatsReadingWrong().size());
		assertEquals(1, dashboard.questionTypeStatsListeningWrong().size());
	}

	@Test
	void getQuestionTypeStatsReading_success_and_failureBranches() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getAccessToken(req)).thenReturn("tok");

		var okBody = BaseResponse.<List<ReportQuestionTypeStats>>builder()
				.data(List.of(new ReportQuestionTypeStats(1, 2)))
				.build();
		when(readingClient.getQuestionTypeStatsReading(any(), any(), anyString()))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(null, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.BAD_REQUEST));

		var res1 = service.getQuestionTypeStatsReading(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertEquals(1, res1.size());

		var res2 = service.getQuestionTypeStatsReading(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res2.isEmpty());

		var res3 = service.getQuestionTypeStatsReading(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res3.isEmpty());
	}

	@Test
	void getQuestionTypeStatsReadingWrong_success_and_failureBranches() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getAccessToken(req)).thenReturn("tok");

		var okBody = BaseResponse.<List<ReportQuestionTypeStatsWrong>>builder()
				.data(List.of(ReportQuestionTypeStatsWrong.builder().questionType(2).wrongCount(3).build()))
				.build();
		when(readingClient.getQuestionTypeStatsReadingWrong(any(), any(), anyString()))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(null, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.BAD_REQUEST));

		var res1 = service.getQuestionTypeStatsReadingWrong(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertEquals(1, res1.size());

		var res2 = service.getQuestionTypeStatsReadingWrong(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res2.isEmpty());

		var res3 = service.getQuestionTypeStatsReadingWrong(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res3.isEmpty());
	}

	@Test
	void getQuestionTypeStatsListening_success_and_failureBranches() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getAccessToken(req)).thenReturn("tok");

		var okBody = BaseResponse.<List<ReportQuestionTypeStats>>builder()
				.data(List.of(new ReportQuestionTypeStats(5, 9)))
				.build();
		when(listeningClient.getQuestionTypeStatsListening(any(), any(), anyString()))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(null, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.BAD_REQUEST));

		var res1 = service.getQuestionTypeStatsListening(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertEquals(1, res1.size());

		var res2 = service.getQuestionTypeStatsListening(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res2.isEmpty());

		var res3 = service.getQuestionTypeStatsListening(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res3.isEmpty());
	}

	@Test
	void getQuestionTypeStatsListeningWrong_success_and_failureBranches() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getAccessToken(req)).thenReturn("tok");

		var okBody = BaseResponse.<List<ReportQuestionTypeStats>>builder()
				.data(List.of(new ReportQuestionTypeStats(6, 4)))
				.build();
		when(listeningClient.getQuestionTypeStatsListening(any(), any(), anyString()))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(null, HttpStatus.OK))
				.thenReturn(new ResponseEntity<>(okBody, HttpStatus.BAD_REQUEST));

		var res1 = service.getQuestionTypeStatsListeningWrong(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertEquals(1, res1.size());

		var res2 = service.getQuestionTypeStatsListeningWrong(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res2.isEmpty());

		var res3 = service.getQuestionTypeStatsListeningWrong(LocalDate.now().minusDays(1), LocalDate.now(), req);
		assertTrue(res3.isEmpty());
	}
}
