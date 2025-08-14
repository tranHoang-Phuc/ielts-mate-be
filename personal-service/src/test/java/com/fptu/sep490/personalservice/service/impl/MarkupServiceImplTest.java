package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.constants.DataMarkup;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.Markup;
import com.fptu.sep490.personalservice.model.enumeration.MarkupType;
import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import com.fptu.sep490.personalservice.repository.MarkupRepository;
import com.fptu.sep490.personalservice.repository.client.ListeningClient;
import com.fptu.sep490.personalservice.repository.client.ReadingClient;
import com.fptu.sep490.personalservice.viewmodel.request.MarkupCreationRequest;
import com.fptu.sep490.personalservice.viewmodel.response.MarkUpResponse;
import com.fptu.sep490.personalservice.viewmodel.response.MarkedUpResponse;
import com.fptu.sep490.personalservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarkupServiceImplTest {

	@Mock
	MarkupRepository markupRepository;
	@Mock
	Helper helper;
	@Mock
	ReadingClient readingClient;
	@Mock
	ListeningClient listeningClient;

	MarkupServiceImpl service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new MarkupServiceImpl(markupRepository, helper, readingClient, listeningClient);
	}

	@Test
	void addMarkup_saves_withEnumValidation() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
		MarkupCreationRequest request = new MarkupCreationRequest(0, 1, 0, UUID.randomUUID());
		service.addMarkup(req, request);
		verify(markupRepository).save(any(Markup.class));
	}

	@Test
	void addMarkup_invalidEnum_throwsAppException() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
		MarkupCreationRequest request = new MarkupCreationRequest(999, 1, 0, UUID.randomUUID());
		assertThrows(AppException.class, () -> service.addMarkup(req, request));
	}

	@Test
	void deleteMarkup_notFound_throwsAppException() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
		when(markupRepository.findByAccountIdAndTaskId(any(), any())).thenReturn(Optional.empty());
		assertThrows(AppException.class, () -> service.deleteMarkup(req, UUID.randomUUID()));
	}

	@Test
	void deleteMarkup_found_deletes() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn(UUID.randomUUID().toString());
		when(markupRepository.findByAccountIdAndTaskId(any(), any())).thenReturn(Optional.of(Markup.builder().build()));
		service.deleteMarkup(req, UUID.randomUUID());
		verify(markupRepository).delete(any(Markup.class));
	}

	@Test
	void getMarkup_mapsAllBranches_andAsyncFetches() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		String userId = UUID.randomUUID().toString();
		when(helper.getAccessToken(req)).thenReturn("tok");
		when(helper.getUserIdFromToken(req)).thenReturn(userId);

		UUID t1 = UUID.randomUUID(); // READING TASK
		UUID t2 = UUID.randomUUID(); // READING EXAM
		UUID t3 = UUID.randomUUID(); // LISTENING TASK
		UUID t4 = UUID.randomUUID(); // LISTENING EXAM
		LocalDateTime base = LocalDateTime.now();
		List<Markup> markups = List.of(
				Markup.builder().markupType(MarkupType.BOOKMARK).taskType(TaskType.READING).practiceType(PracticeType.TASK).taskId(t1).createdAt(base.plusSeconds(3)).build(),
				Markup.builder().markupType(MarkupType.FAVORITE).taskType(TaskType.READING).practiceType(PracticeType.EXAM).taskId(t2).createdAt(base.plusSeconds(2)).build(),
				Markup.builder().markupType(MarkupType.BOOKMARK).taskType(TaskType.LISTENING).practiceType(PracticeType.TASK).taskId(t3).createdAt(base.plusSeconds(1)).build(),
				Markup.builder().markupType(MarkupType.FAVORITE).taskType(TaskType.LISTENING).practiceType(PracticeType.EXAM).taskId(t4).createdAt(base).build()
		);
		Page<Markup> page = new PageImpl<>(markups, PageRequest.of(0, 10), markups.size());
		when(markupRepository.findAll(Mockito.<Specification<Markup>>any(), any(Pageable.class))).thenReturn(page);

		var readingTitleBody = BaseResponse.<List<TaskTitle>>builder()
				.data(List.of(TaskTitle.builder().taskId(t1).title("R-T1").build()))
				.build();
		when(readingClient.getReadingTitle(anyList(), anyString()))
				.thenReturn(ResponseEntity.ok(readingTitleBody));

		var readingExamTitleBody = BaseResponse.<List<TaskTitle>>builder()
				.data(List.of(TaskTitle.builder().taskId(t2).title("R-E1").build()))
				.build();
		when(readingClient.getExamTitle(anyList(), anyString()))
				.thenReturn(ResponseEntity.ok(readingExamTitleBody));

		var listeningTitleBody = BaseResponse.<List<TaskTitle>>builder()
				.data(List.of(TaskTitle.builder().taskId(t3).title("L-T1").build()))
				.build();
		when(listeningClient.getListeningTitle(anyList(), anyString()))
				.thenReturn(ResponseEntity.ok(listeningTitleBody));

		var listeningExamTitleBody = BaseResponse.<List<TaskTitle>>builder()
				.data(List.of(TaskTitle.builder().taskId(t4).title("L-E1").build()))
				.build();
		when(listeningClient.getExamTitle(anyList(), anyString()))
				.thenReturn(ResponseEntity.ok(listeningExamTitleBody));

		var result = service.getMarkup(0, 10, List.of(), List.of(), List.of(), req);
		assertEquals(4, result.getTotalElements());
		List<MarkUpResponse> content = result.getContent();
		assertEquals("R-T1", content.get(0).taskTitle());
		assertEquals("R-E1", content.get(1).taskTitle());
		assertEquals("L-T1", content.get(2).taskTitle());
		assertEquals("L-E1", content.get(3).taskTitle());
	}

	@Test
	void getMarkedUpData_allTypes_andFallbackToSecurityContext() {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		when(helper.getUserIdFromToken(req)).thenReturn(null);
		when(helper.getUserIdFromToken()).thenReturn(UUID.randomUUID().toString());

		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		UUID id4 = UUID.randomUUID();

		when(markupRepository.findMarkupByAccountIdAndTaskTypeAndPracticeType(any(), eq(TaskType.READING.ordinal()), eq(PracticeType.TASK.ordinal())))
				.thenReturn(List.of(Markup.builder().taskId(id1).markupType(MarkupType.BOOKMARK).build()));
		when(markupRepository.findMarkupByAccountIdAndTaskTypeAndPracticeType(any(), eq(TaskType.READING.ordinal()), eq(PracticeType.EXAM.ordinal())))
				.thenReturn(List.of(Markup.builder().taskId(id2).markupType(MarkupType.FAVORITE).build()));
		when(markupRepository.findMarkupByAccountIdAndTaskTypeAndPracticeType(any(), eq(TaskType.LISTENING.ordinal()), eq(PracticeType.EXAM.ordinal())))
				.thenReturn(List.of(Markup.builder().taskId(id3).markupType(MarkupType.BOOKMARK).build()));
		when(markupRepository.findMarkupByAccountIdAndTaskTypeAndPracticeType(any(), eq(TaskType.LISTENING.ordinal()), eq(PracticeType.TASK.ordinal())))
				.thenReturn(List.of(Markup.builder().taskId(id4).markupType(MarkupType.FAVORITE).build()));

		MarkedUpResponse r1 = service.getMarkedUpData(DataMarkup.READING_TASK, req);
		assertTrue(r1.markedUpIdsMapping().containsKey(id1));
		MarkedUpResponse r2 = service.getMarkedUpData(DataMarkup.READING_EXAM, req);
		assertTrue(r2.markedUpIdsMapping().containsKey(id2));
		MarkedUpResponse r3 = service.getMarkedUpData(DataMarkup.LISTENING_EXAM, req);
		assertTrue(r3.markedUpIdsMapping().containsKey(id3));
		MarkedUpResponse r4 = service.getMarkedUpData(DataMarkup.LISTENING_TASK, req);
		assertTrue(r4.markedUpIdsMapping().containsKey(id4));
		assertNull(service.getMarkedUpData("UNKNOWN", req));
	}
}
