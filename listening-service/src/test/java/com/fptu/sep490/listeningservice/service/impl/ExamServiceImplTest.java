package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningExam;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.enumeration.ExamStatus;
import com.fptu.sep490.listeningservice.model.enumeration.IeltsType;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.MarkupClient;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ExamResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExamServiceImplTest {

    @Mock ListeningTaskRepository listeningTaskRepository;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock DragItemRepository dragItemRepository;
    @Mock QuestionRepository questionRepository;
    @Mock ChoiceRepository choiceRepository;
    @Mock ListeningExamRepository listeningExamRepository;
    @Mock MarkupClient markupClient;
    @Mock Helper helper;
    @Mock HttpServletRequest httpServletRequest;

    @InjectMocks ExamServiceImpl examService;

    private UUID part1Id, part2Id, part3Id, part4Id, examId;
    private ListeningTask part1, part2, part3, part4;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        part1Id = UUID.randomUUID();
        part2Id = UUID.randomUUID();
        part3Id = UUID.randomUUID();
        part4Id = UUID.randomUUID();
        examId = UUID.randomUUID();

        part1 = ListeningTask.builder().taskId(part1Id).partNumber(PartNumber.PART_1).ieltsType(IeltsType.ACADEMIC).isCurrent(true).isDeleted(false).build();
        part2 = ListeningTask.builder().taskId(part2Id).partNumber(PartNumber.PART_2).ieltsType(IeltsType.ACADEMIC).isCurrent(true).isDeleted(false).build();
        part3 = ListeningTask.builder().taskId(part3Id).partNumber(PartNumber.PART_3).ieltsType(IeltsType.ACADEMIC).isCurrent(true).isDeleted(false).build();
        part4 = ListeningTask.builder().taskId(part4Id).partNumber(PartNumber.PART_4).ieltsType(IeltsType.ACADEMIC).isCurrent(true).isDeleted(false).build();
    }

    @Test
    void createExam_ShouldThrow_WhenUnauthorized() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(null);

        ExamRequest req = new ExamRequest("Test", "Desc",0, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () -> examService.createExam(req, httpServletRequest));

        assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }

    @Test
    void createExam_ShouldThrow_WhenPart1NotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.empty());

        ExamRequest req = new ExamRequest("Test", "Desc",1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () -> examService.createExam(req, httpServletRequest));

        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void createExam_ShouldThrow_WhenWrongPartNumber() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        ListeningTask wrongPart = ListeningTask.builder()
                .taskId(part1Id)
                .partNumber(PartNumber.PART_2)
                .isCurrent(true)
                .isDeleted(false)
                .build();
        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(wrongPart));

        ExamRequest req = new ExamRequest("Test", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id );

        AppException ex = assertThrows(AppException.class, () -> examService.createExam(req, httpServletRequest));

        assertEquals(Constants.ErrorCode.WRONG_PART, ex.getBusinessErrorCode());
    }@Test
    void createExam_ShouldThrow_WhenPart2WrongPartNumber() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(part1)); // correct
        ListeningTask wrongPart2 = ListeningTask.builder()
                .taskId(part2Id)
                .partNumber(PartNumber.PART_3) // wrong
                .isCurrent(true)
                .isDeleted(false)
                .build();
        when(listeningTaskRepository.findById(part2Id)).thenReturn(Optional.of(wrongPart2));

        ExamRequest req = new ExamRequest("Test", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () -> examService.createExam(req, httpServletRequest));

        assertEquals(Constants.ErrorCode.WRONG_PART, ex.getBusinessErrorCode());
    }

    @Test
    void createExam_ShouldThrow_WhenPart3WrongPartNumber() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(part1));
        when(listeningTaskRepository.findById(part2Id)).thenReturn(Optional.of(part2));
        ListeningTask wrongPart3 = ListeningTask.builder()
                .taskId(part3Id)
                .partNumber(PartNumber.PART_4) // wrong
                .isCurrent(true)
                .isDeleted(false)
                .build();
        when(listeningTaskRepository.findById(part3Id)).thenReturn(Optional.of(wrongPart3));

        ExamRequest req = new ExamRequest("Test", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () -> examService.createExam(req, httpServletRequest));

        assertEquals(Constants.ErrorCode.WRONG_PART, ex.getBusinessErrorCode());
    }

    @Test
    void createExam_ShouldThrow_WhenPart4WrongPartNumber() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(part1));
        when(listeningTaskRepository.findById(part2Id)).thenReturn(Optional.of(part2));
        when(listeningTaskRepository.findById(part3Id)).thenReturn(Optional.of(part3));
        ListeningTask wrongPart4 = ListeningTask.builder()
                .taskId(part4Id)
                .partNumber(PartNumber.PART_1) // wrong
                .isCurrent(true)
                .isDeleted(false)
                .build();
        when(listeningTaskRepository.findById(part4Id)).thenReturn(Optional.of(wrongPart4));

        ExamRequest req = new ExamRequest("Test", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () -> examService.createExam(req, httpServletRequest));

        assertEquals(Constants.ErrorCode.WRONG_PART, ex.getBusinessErrorCode());
    }


    @Test
    void createExam_ShouldReturnResponse_WhenSuccess() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(part1));
        when(listeningTaskRepository.findById(part2Id)).thenReturn(Optional.of(part2));
        when(listeningTaskRepository.findById(part3Id)).thenReturn(Optional.of(part3));
        when(listeningTaskRepository.findById(part4Id)).thenReturn(Optional.of(part4));

        ListeningExam savedExam = ListeningExam.builder()
                .listeningExamId(examId)
                .examName("Test")
                .examDescription("Desc")
                .urlSlug("slug")
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .status(ExamStatus.ACTIVE)
                .isCurrent(true)
                .isDeleted(false)
                .isOriginal(true)
                .createdBy("user1")
                .build();

        when(listeningExamRepository.save(any(ListeningExam.class))).thenReturn(savedExam);

        ExamRequest req = new ExamRequest("Test", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        ExamResponse res = examService.createExam(req, httpServletRequest);

        assertNotNull(res);
        assertEquals(examId, res.listeningExamId());
        assertEquals("Test", res.examName());
        verify(listeningExamRepository).save(any(ListeningExam.class));
    }

    @Test
    void deleteExam_ShouldThrow_WhenNotFound() {
        when(listeningExamRepository.findById(examId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> examService.deleteExam(examId.toString(), httpServletRequest));

        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void deleteExam_ShouldSuccess_WhenValid() throws Exception {
        ListeningExam exam = ListeningExam.builder().listeningExamId(examId).isDeleted(false).build();
        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(listeningExamRepository.findAllCurrentByParentId(examId)).thenReturn(List.of());

        examService.deleteExam(examId.toString(), httpServletRequest);

        assertTrue(exam.getIsDeleted());
        verify(listeningExamRepository).save(exam);
    }
    @Test
    void findCurrentOrChildCurrentExam_ShouldReturnExam_WhenCurrentAndNotDeleted() throws Exception {
        ListeningExam exam = ListeningExam.builder()
                .listeningExamId(examId)
                .isCurrent(true)
                .isDeleted(false)
                .build();

        ListeningExam result = examService.findCurrentOrChildCurrentExam(exam);

        assertEquals(exam, result);
    }

    @Test
    void findCurrentOrChildCurrentExam_ShouldReturnChild_WhenParentNotCurrent() throws Exception {
        ListeningExam child = ListeningExam.builder()
                .listeningExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .build();
        ListeningExam parent = ListeningExam.builder()
                .listeningExamId(examId)
                .isCurrent(false)
                .isDeleted(false)
                .children(List.of(child))
                .build();

        ListeningExam result = examService.findCurrentOrChildCurrentExam(parent);

        assertEquals(child, result);
    }

    @Test
    void findCurrentOrChildCurrentExam_ShouldReturnNull_WhenNoCurrentExam() throws Exception {
        ListeningExam child = ListeningExam.builder()
                .listeningExamId(UUID.randomUUID())
                .isCurrent(false)
                .isDeleted(true)
                .build();
        ListeningExam parent = ListeningExam.builder()
                .listeningExamId(examId)
                .isCurrent(false)
                .isDeleted(true)
                .children(List.of(child))
                .build();

        ListeningExam result = examService.findCurrentOrChildCurrentExam(parent);

        assertNull(result);
    }
    @Test
    void mapToExamResponse_ShouldMapAllFieldsCorrectly() {
        // Mock current parts
        ListeningTask task1 = ListeningTask.builder().taskId(UUID.randomUUID()).partNumber(PartNumber.PART_1).ieltsType(IeltsType.ACADEMIC).isCurrent(true)
                .isDeleted(false).build();
        ListeningTask task2 = ListeningTask.builder().taskId(UUID.randomUUID()).partNumber(PartNumber.PART_2).ieltsType(IeltsType.ACADEMIC).isCurrent(true)
                .isDeleted(false).build();
        ListeningTask task3 = ListeningTask.builder().taskId(UUID.randomUUID()).partNumber(PartNumber.PART_3).ieltsType(IeltsType.ACADEMIC).isCurrent(true)
                .isDeleted(false).build();
        ListeningTask task4 = ListeningTask.builder().taskId(UUID.randomUUID()).partNumber(PartNumber.PART_4).ieltsType(IeltsType.ACADEMIC).isCurrent(true)
                .isDeleted(false).build();

        ListeningExam exam = ListeningExam.builder()
                .listeningExamId(examId)
                .examName("Test Exam")
                .examDescription("Desc")
                .urlSlug("slug")
                .part1(task1)
                .part2(task2)
                .part3(task3)
                .part4(task4)
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .createdBy("user1")
                .build();

        Map<UUID, Integer> markedUpIdsMapping = Map.of(examId, 99);

        ExamResponse response = examService.mapToExamResponse(exam, markedUpIdsMapping);

        assertEquals(examId, response.listeningExamId());
        assertEquals("Test Exam", response.examName());
        assertEquals(task1.getTaskId(), response.part1().taskId());
        assertEquals(task2.getTaskId(), response.part2().taskId());
        assertEquals(task3.getTaskId(), response.part3().taskId());
        assertEquals(task4.getTaskId(), response.part4().taskId());
        assertTrue(response.isMarkedUp());
        }

    @Test
    void deleteExam_ShouldDeleteExamAndChildren_WhenChildrenExist() throws Exception {
        // Main exam
        ListeningExam mainExam = ListeningExam.builder()
                .listeningExamId(examId)
                .isDeleted(false)
                .build();

        // Child exams
        ListeningExam child1 = ListeningExam.builder()
                .listeningExamId(UUID.randomUUID())
                .isDeleted(false)
                .build();
        ListeningExam child2 = ListeningExam.builder()
                .listeningExamId(UUID.randomUUID())
                .isDeleted(false)
                .build();

        // Mock repository responses
        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(mainExam));
        when(listeningExamRepository.findAllCurrentByParentId(examId)).thenReturn(List.of(child1, child2));

        // Call the method
        examService.deleteExam(examId.toString(), httpServletRequest);

        // Verify main exam is deleted
        assertTrue(mainExam.getIsDeleted());

        // Verify child exams are deleted
        assertTrue(child1.getIsDeleted());
        assertTrue(child2.getIsDeleted());

        // Verify save is called for all
        verify(listeningExamRepository).save(mainExam);
        verify(listeningExamRepository).save(child1);
        verify(listeningExamRepository).save(child2);
    }

    @Test
    void getExamById_ShouldThrow_WhenUnauthorized() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> examService.getExamById(examId.toString(), httpServletRequest));

        assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }

    @Test
    void getExamById_ShouldThrow_WhenNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningExamRepository.findById(examId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> examService.getExamById(examId.toString(), httpServletRequest));

        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void getExamById_ShouldReturn_WhenSuccess() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        ListeningExam exam = ListeningExam.builder()
                .listeningExamId(examId)
                .isCurrent(true)
                .isDeleted(false)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .examName("Test")
                .examDescription("Desc")
                .urlSlug("slug")
                .build();

        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamResponse res = examService.getExamById(examId.toString(), httpServletRequest);

        assertNotNull(res);
        assertEquals("Test", res.examName());
        assertEquals(examId, res.listeningExamId());
    }


    // Unauthorized
    @Test
    void updateExam_ShouldThrow_WhenUnauthorized() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(null);

        ExamRequest req = new ExamRequest("Name", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () ->
                examService.updateExam(examId.toString(), req, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
    }

    // Exam deleted
    @Test
    void updateExam_ShouldThrow_WhenExamDeleted() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        ListeningExam deletedExam = ListeningExam.builder().isDeleted(true).listeningExamId(examId).build();
        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(deletedExam));

        ExamRequest req = new ExamRequest("Name", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () ->
                examService.updateExam(examId.toString(), req, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.EXAM_DELETED, ex.getBusinessErrorCode());
    }

    // Part deleted - part 1


    @Test
    void updateExam_ShouldThrow_WhenPart2IsDeleted() throws Exception {
        testPartDeleted(part2Id, PartNumber.PART_2);
    }

    @Test
    void updateExam_ShouldThrow_WhenPart3IsDeleted() throws Exception {
        testPartDeleted(part3Id, PartNumber.PART_3);
    }

    @Test
    void updateExam_ShouldThrow_WhenPart4IsDeleted() throws Exception {
        testPartDeleted(part4Id, PartNumber.PART_4);
    }

    private void testPartDeleted(UUID partId, PartNumber partNumber) throws Exception {
        // Mock user ID retrieval
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        // Original exam setup
        ListeningExam originalExam = ListeningExam.builder()
                .listeningExamId(examId)
                .isDeleted(false)
                .isCurrent(true)
                .status(ExamStatus.ACTIVE)
                .version(1)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .build();

        // Current exam used internally by service
        ListeningExam currentExam = ListeningExam.builder()
                .listeningExamId(UUID.randomUUID())
                .isDeleted(false)
                .isCurrent(true)
                .version(1)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .build();

        // Deleted part setup
        ListeningTask deletedPart = ListeningTask.builder()
                .taskId(partId)
                .partNumber(partNumber)
                .isDeleted(true)
                .build();

        // Mock repository calls
        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(originalExam));
        when(listeningTaskRepository.findById(partId)).thenReturn(Optional.of(deletedPart));

        // Spy the service to mock findCurrentOrChildCurrentExam
        ExamServiceImpl spyService = Mockito.spy(examService);
        doReturn(currentExam).when(spyService).findCurrentOrChildCurrentExam(originalExam);

        // Mock save to prevent NPE
        when(listeningExamRepository.save(any(ListeningExam.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Build request with only the part being tested
        ExamRequest req = switch (partNumber) {
            case PART_1 -> new ExamRequest("Name", "Desc", 1, "slug", partId, null, null, null);
            case PART_2 -> new ExamRequest("Name", "Desc", 1, "slug", null, partId, null, null);
            case PART_3 -> new ExamRequest("Name", "Desc", 1, "slug", null, null, partId, null);
            case PART_4 -> new ExamRequest("Name", "Desc", 1, "slug", null, null, null, partId);
        };

        // Assert that AppException is thrown
        AppException ex = assertThrows(AppException.class, () ->
                spyService.updateExam(examId.toString(), req, httpServletRequest)
        );

        // Verify the business error code
        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }



    // Wrong part number
    @Test
    void updateExam_ShouldThrow_WhenWrongPartNumber() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        ListeningExam originalExam = ListeningExam.builder()
                .listeningExamId(examId)
                .isDeleted(false)
                .isCurrent(true)
                .status(ExamStatus.ACTIVE)
                .version(1)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .build();

        ListeningTask wrongPart = ListeningTask.builder()
                .taskId(part1Id)
                .partNumber(PartNumber.PART_2) // wrong number
                .isDeleted(false)
                .build();

        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(originalExam));
        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(wrongPart));

        ExamRequest req = new ExamRequest("Name", "Desc", 1, "slug", part1Id, part2Id, part3Id, part4Id);

        AppException ex = assertThrows(AppException.class, () ->
                examService.updateExam(examId.toString(), req, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.WRONG_PART, ex.getBusinessErrorCode());
    }

    @Test
    void updateExam_ShouldReturn_WhenSuccess() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        ListeningExam originalExam = ListeningExam.builder()
                .listeningExamId(examId)
                .isDeleted(false)
                .isCurrent(true)
                .status(ExamStatus.ACTIVE)
                .version(1)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .build();
        ListeningExam savedExam = ListeningExam.builder()
                .listeningExamId(examId)
                .examName("New Name")
                .examDescription("New Desc")
                .urlSlug("new-slug")
                .status(ExamStatus.ACTIVE)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .createdBy("user1")
                .updatedBy("user1")
                .isCurrent(true)
                .version(2)
                .isOriginal(false)
                .isDeleted(false)
                .build();

        when(listeningExamRepository.save(any(ListeningExam.class))).thenReturn(savedExam);

        when(listeningExamRepository.findById(examId)).thenReturn(Optional.of(originalExam));
        when(listeningTaskRepository.findById(part1Id)).thenReturn(Optional.of(part1));
        when(listeningTaskRepository.findById(part2Id)).thenReturn(Optional.of(part2));
        when(listeningTaskRepository.findById(part3Id)).thenReturn(Optional.of(part3));
        when(listeningTaskRepository.findById(part4Id)).thenReturn(Optional.of(part4));

        ExamRequest req = new ExamRequest("New Name", "New Desc", 1, "new-slug", part1Id, part2Id, part3Id, part4Id);

        ExamResponse res = examService.updateExam(examId.toString(), req, httpServletRequest);

        assertNotNull(res);
        assertEquals("New Name", res.examName());
        verify(listeningExamRepository, atLeastOnce()).save(any(ListeningExam.class));
    }

    @Test
    void getAllExamsForCreator_ShouldThrow_WhenUnauthorized() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> examService.getAllExamsForCreator(httpServletRequest, 0, 10, "createdAt", "asc", null));

        assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
    }

    @Test
    void getAllExamsForCreator_ShouldReturnPage() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        ListeningExam exam = ListeningExam.builder()
                .listeningExamId(examId)
                .isCurrent(true)
                .isDeleted(false)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .examName("Test")
                .examDescription("Desc")
                .urlSlug("slug")
                .build();

        when(listeningExamRepository.searchCurrentExamsByCreator(eq("user1"), any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(exam)));

        Page<ExamResponse> res = examService.getAllExamsForCreator(httpServletRequest, 0, 10, "createdAt", "asc", null);

        assertEquals(1, res.getTotalElements());
        assertEquals("Test", res.getContent().get(0).examName());
    }

    @Test
    void getActiveExams_ShouldThrow_WhenUnauthorized() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> examService.getActiveExams(0, 10, "createdAt", "asc", httpServletRequest, null));

        assertEquals(Constants.ErrorCode.UNAUTHORIZED, ex.getBusinessErrorCode());
    }

    @Test
    void getActiveExams_ShouldReturnPage_WhenNoMarkup() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");

        // Tạo exam giả
        ListeningExam exam = ListeningExam.builder()
                .listeningExamId(examId)
                .isCurrent(true)
                .isDeleted(false)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .examName("Test")
                .examDescription("Desc")
                .urlSlug("slug")
                .build();

        // Mock repository trả về page chứa exam
        when(listeningExamRepository.searchCurrentExamsActivated(eq("user1"), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(exam)));

        // Mock request.getCookies() trả về cookie hợp lệ (markup = null)
        Cookie[] cookies = new Cookie[]{ new Cookie("markup", null) };
        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // Gọi service
        Page<ExamResponse> res = examService.getActiveExams(0, 10, "createdAt", "asc", httpServletRequest, null);

        // Kiểm tra
        assertEquals(1, res.getTotalElements());
    }


    @Test
    void getExamTitle_ShouldReturnList() {
        ListeningExam exam = ListeningExam.builder().listeningExamId(examId).examName("Title").build();
        when(listeningExamRepository.findAllById(List.of(examId))).thenReturn(List.of(exam));

        List<TaskTitle> titles = examService.getExamTitle(List.of(examId));

        assertEquals(1, titles.size());
        assertEquals("Title", titles.get(0).title());
    }

}
