package com.fptu.sep490.readingservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.model.enumeration.PartNumber;
import com.fptu.sep490.readingservice.repository.client.MarkupClient;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.ReadingExam;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.repository.ReadingExamRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamResponse;
import com.fptu.sep490.readingservice.viewmodel.response.TaskTitle;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReadingExamServiceImplTest {

    @Mock
    Helper helper;
    @Mock
    ReadingPassageRepository readingPassageRepository;
    @Mock
    ReadingExamRepository readingExamRepository;
    @Mock
    MarkupClient markupClient;

    @InjectMocks
    ReadingExamServiceImpl service;

    private UUID part1, part2, part3;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        part1 = UUID.randomUUID();
        part2 = UUID.randomUUID();
        part3 = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    //TODO ===== createReadingExam =====

    @Test
    void createReadingExam_nullRequest_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createReadingExam(null, mock(HttpServletRequest.class)));
    }

    @Test
    void createReadingExam_passageNotFoundPart1_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");
        when(readingPassageRepository.findById(part1)).thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                part1.toString(), null, null
        );

        assertThrows(AppException.class,
                () -> service.createReadingExam(request, req));
    }
    // part 2 not found, like upper method
    @Test
    void createReadingExam_passageNotFoundPart2_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");
        when(readingPassageRepository.findById(part2)).thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                "", part2.toString(), null
        );

        assertThrows(AppException.class,
                () -> service.createReadingExam(request, req));
    }
    @Test
    void createReadingExam_passageNotFoundPart3_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");
        when(readingPassageRepository.findById(part3)).thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                "", "", part3.toString()
        );

        assertThrows(AppException.class,
                () -> service.createReadingExam(request, req));
    }

    // Wrong part number of part 1
    @Test
    void createReadingExam_passageWrongPartNumberPart1_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(part1)
                .partNumber(PartNumber.PART_2) // Wrong part number
                .title("t1").content("c1").build();

        when(readingPassageRepository.findById(part1)).thenReturn(Optional.of(p1));

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                part1.toString(), null, null
        );

        assertThrows(AppException.class,
                () -> service.createReadingExam(request, req));
    }
    // Wrong part number of part 2
    @Test
    void createReadingExam_passageWrongPartNumberPart2_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p2 = ReadingPassage.builder()
                .passageId(part2)
                .partNumber(PartNumber.PART_1) // Wrong part number
                .title("t2").content("c2").build();

        when(readingPassageRepository.findById(part2)).thenReturn(Optional.of(p2));

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                "", part2.toString(), null
        );

        assertThrows(AppException.class,
                () -> service.createReadingExam(request, req));
    }
    // Wrong part number of part 3
    @Test
    void createReadingExam_passageWrongPartNumberPart3_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p3 = ReadingPassage.builder()
                .passageId(part3)
                .partNumber(PartNumber.PART_1) // Wrong part number
                .title("t3").content("c3").build();

        when(readingPassageRepository.findById(part3)).thenReturn(Optional.of(p3));

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                "", "", part3.toString()
        );

        assertThrows(AppException.class,
                () -> service.createReadingExam(request, req));
    }

    @Test
    void createReadingExam_success_returnsResponse() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        // Build passages with correct part numbers
        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(part1)
                .partNumber(PartNumber.PART_1)
                .title("t1").content("c1").build();
        ReadingPassage p2 = ReadingPassage.builder()
                .passageId(part2)
                .partNumber(PartNumber.PART_2)
                .title("t2").content("c2").build();
        ReadingPassage p3 = ReadingPassage.builder()
                .passageId(part3)
                .partNumber(PartNumber.PART_3)
                .title("t3").content("c3").build();

        // Mock repository returns
        when(readingPassageRepository.findById(part1)).thenReturn(Optional.of(p1));
        when(readingPassageRepository.findById(part2)).thenReturn(Optional.of(p2));
        when(readingPassageRepository.findById(part3)).thenReturn(Optional.of(p3));

        // Mock save to just return the exam
        when(readingExamRepository.save(any(ReadingExam.class)))
                .thenAnswer(inv -> {
                    ReadingExam exam = inv.getArgument(0);
                    exam.setReadingExamId(UUID.randomUUID()); // simulate DB-generated ID
                    return exam;
                });
        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                part1.toString(), part2.toString(), part3.toString()
        );

        ReadingExamResponse response = service.createReadingExam(request, req);

        // Verify save was called
        verify(readingExamRepository).save(any(ReadingExam.class));

        // Assert response content
        assertNotNull(response);
        assertEquals("exam1", response.readingExamName());
        assertEquals("desc", response.readingExamDescription());
        assertEquals("slug", response.urlSlug());

        assertEquals("t1", response.readingPassageIdPart1().readingPassageName());
        assertEquals("c1", response.readingPassageIdPart1().readingPassageContent());

        assertEquals("t2", response.readingPassageIdPart2().readingPassageName());
        assertEquals("c2", response.readingPassageIdPart2().readingPassageContent());

        assertEquals("t3", response.readingPassageIdPart3().readingPassageName());
        assertEquals("c3", response.readingPassageIdPart3().readingPassageContent());
    }


    //TODO ===== updateReadingExam =====

    @Test
    void updateReadingExam_examNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(readingExamRepository.findById(any())).thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1, null, null, null
        );

        assertThrows(AppException.class,
                () -> service.updateReadingExam(UUID.randomUUID().toString(), request, req));
    }

    // update reading exam when request is null
    @Test
    void updateReadingExam_nullRequest_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId()))
                .thenReturn(Optional.of(exam));

        AppException ex = assertThrows(AppException.class,
                () -> service.updateReadingExam(exam.getReadingExamId().toString(), null, req));

        // ✅ Optional: verify the error code/message matches expected
        assertEquals("INVALID_INPUT", ex.getMessage());
    }



    @Test
    void updateReadingExam_passageNotFoundPart2_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId()))
                .thenReturn(Optional.of(exam));
        when(readingPassageRepository.findById(part2))
                .thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                null, part2.toString(), null
        );

        assertThrows(AppException.class,
                () -> service.updateReadingExam(exam.getReadingExamId().toString(), request, req));
    }


    // update when part 1 is not found
    @Test
    void updateReadingExam_passageNotFoundPart1_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId()))
                .thenReturn(Optional.of(exam));
        when(readingPassageRepository.findById(part1))
                .thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                part1.toString(), null, null
        );

        assertThrows(AppException.class,
                () -> service.updateReadingExam(exam.getReadingExamId().toString(), request, req));
    }
    //update when part 3 not found
    @Test
    void updateReadingExam_passageNotFoundPart3_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId()))
                .thenReturn(Optional.of(exam));
        when(readingPassageRepository.findById(part3))
                .thenReturn(Optional.empty());

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "exam1", "desc", "slug", 1,
                null, null, part3.toString()
        );

        assertThrows(AppException.class,
                () -> service.updateReadingExam(exam.getReadingExamId().toString(), request, req));
    }

    @Test
    void updateReadingExam_successPart1Only() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .build();

        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(part1)
                .partNumber(PartNumber.PART_1)
                .title("t1").content("c1").build();

        when(readingExamRepository.findById(exam.getReadingExamId()))
                .thenReturn(Optional.of(exam));
        when(readingPassageRepository.findById(part1))
                .thenReturn(Optional.of(p1));
        when(readingExamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReadingExamCreationRequest request = new ReadingExamCreationRequest(
                "examUpdated", "desc2", "slug2", 2,
                part1.toString(), null, null
        );

        ReadingExamResponse resp = service.updateReadingExam(
                exam.getReadingExamId().toString(), request, req);

        // ✅ Assertions
        assertEquals("examUpdated", resp.readingExamName());
        assertEquals("t1", resp.readingPassageIdPart1().readingPassageName());

        // Instead of assertNull, check that the object exists but is empty
        assertNotNull(resp.readingPassageIdPart2());
        assertNull(resp.readingPassageIdPart2().readingPassageId());
        assertNull(resp.readingPassageIdPart2().readingPassageName());
        assertNull(resp.readingPassageIdPart2().readingPassageContent());

        // Same for part3
        assertNotNull(resp.readingPassageIdPart3());
        assertNull(resp.readingPassageIdPart3().readingPassageId());
        assertNull(resp.readingPassageIdPart3().readingPassageName());
        assertNull(resp.readingPassageIdPart3().readingPassageContent());
    }

    //TODO test for findCurrentChildByParentId
    @Test
    void findCurrentChildByParentId_parentIsCurrent_returnsParent() throws Exception {
        ReadingExam parent = ReadingExam.builder()
                .isCurrent(true)
                .isDeleted(false)
                .children(new ArrayList<>())
                .build();

        // Access private method via reflection
        Method method = ReadingExamServiceImpl.class.getDeclaredMethod(
                "findCurrentChildByParentId", ReadingExam.class);
        method.setAccessible(true);

        ReadingExam result = (ReadingExam) method.invoke(service, parent);

        assertSame(parent, result, "Should return parent because it is current and not deleted");
    }

    @Test
    void findCurrentChildByParentId_childIsCurrent_returnsChild() throws Exception {
        ReadingExam child = ReadingExam.builder()
                .isCurrent(true)
                .isDeleted(false)
                .children(new ArrayList<>())
                .build();

        ReadingExam parent = ReadingExam.builder()
                .isCurrent(false)
                .isDeleted(false)
                .children(List.of(child))
                .build();

        Method method = ReadingExamServiceImpl.class.getDeclaredMethod(
                "findCurrentChildByParentId", ReadingExam.class);
        method.setAccessible(true);

        ReadingExam result = (ReadingExam) method.invoke(service, parent);

        assertSame(child, result, "Should return child because parent is not current but child is");
    }

    @Test
    void findCurrentChildByParentId_noneCurrent_returnsParent() throws Exception {
        ReadingExam child = ReadingExam.builder()
                .isCurrent(false)
                .isDeleted(false)
                .children(new ArrayList<>())
                .build();

        ReadingExam parent = ReadingExam.builder()
                .isCurrent(false)
                .isDeleted(false)
                .children(List.of(child))
                .build();

        Method method = ReadingExamServiceImpl.class.getDeclaredMethod(
                "findCurrentChildByParentId", ReadingExam.class);
        method.setAccessible(true);

        ReadingExam result = (ReadingExam) method.invoke(service, parent);

        assertSame(parent, result, "Should return parent because neither parent nor children are current");
    }

    //TODO test for get reading exam detail
    @Test
    void getReadingExam_examNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");
        UUID examId = UUID.randomUUID();

        when(readingExamRepository.findById(examId)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.getReadingExam(examId.toString(), req));
    }

    @Test
    void getReadingExam_examDeleted_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isDeleted(true)
                .isCurrent(true)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId())).thenReturn(Optional.of(exam));

        assertThrows(AppException.class,
                () -> service.getReadingExam(exam.getReadingExamId().toString(), req));
    }

    @Test
    void getReadingExam_examCurrent_returnsExam() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p1 = ReadingPassage.builder().passageId(UUID.randomUUID()).title("t1").content("c1").partNumber(PartNumber.PART_1).build();

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isDeleted(false)
                .isCurrent(true)
                .examName("Exam 1")
                .examDescription("desc")
                .urlSlug("slug")
                .part1(p1)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId())).thenReturn(Optional.of(exam));

        ReadingExamResponse response = service.getReadingExam(exam.getReadingExamId().toString(), req);

        assertEquals("Exam 1", response.readingExamName());
        assertEquals("t1", response.readingPassageIdPart1().readingPassageName());
    }

    @Test
    void getReadingExam_examNotCurrentWithChild_returnsChild() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p1 = ReadingPassage.builder().passageId(UUID.randomUUID()).title("t1").content("c1").partNumber(PartNumber.PART_1).build();

        ReadingExam child = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isDeleted(false)
                .isCurrent(true)
                .examName("Child Exam")
                .examDescription("descChild")
                .urlSlug("slugChild")
                .part1(p1)
                .build();

        ReadingExam parent = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isDeleted(false)
                .isCurrent(false)
                .build();

        when(readingExamRepository.findById(parent.getReadingExamId())).thenReturn(Optional.of(parent));
        when(readingExamRepository.findCurrentChildByParentId(parent.getReadingExamId())).thenReturn(Optional.of(child));

        ReadingExamResponse response = service.getReadingExam(parent.getReadingExamId().toString(), req);

        assertEquals("Child Exam", response.readingExamName());
        assertEquals("t1", response.readingPassageIdPart1().readingPassageName());
    }

    @Test
    void getReadingExam_examNotCurrentNoChild_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingExam parent = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isDeleted(false)
                .isCurrent(false)
                .build();

        when(readingExamRepository.findById(parent.getReadingExamId())).thenReturn(Optional.of(parent));
        when(readingExamRepository.findCurrentChildByParentId(parent.getReadingExamId())).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.getReadingExam(parent.getReadingExamId().toString(), req));
    }
    @Test
    void deleteReadingExam_examNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        UUID examId = UUID.randomUUID();
        when(readingExamRepository.findById(examId)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.deleteReadingExam(examId.toString(), req));
    }

    //TODO test for deleteReadingExam

    @Test
    void deleteReadingExam_examCurrent_marksDeletedAndReturnsResponse() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p1 = ReadingPassage.builder().passageId(UUID.randomUUID()).title("t1").content("c1").partNumber(PartNumber.PART_1).build();

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .examName("Exam 1")
                .examDescription("desc")
                .urlSlug("slug")
                .part1(p1)
                .build();

        when(readingExamRepository.findById(exam.getReadingExamId())).thenReturn(Optional.of(exam));
        when(readingExamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReadingExamResponse resp = service.deleteReadingExam(exam.getReadingExamId().toString(), req);

        assertTrue(exam.getIsDeleted());
        assertEquals("Exam 1", resp.readingExamName());
        assertEquals("t1", resp.readingPassageIdPart1().readingPassageName());

        verify(readingExamRepository, times(2)).save(any(ReadingExam.class)); // parent + finalReadingExam
    }

    @Test
    void deleteReadingExam_parentWithCurrentChild_marksBothDeleted() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .title("t1").content("c1")
                .partNumber(PartNumber.PART_1)
                .build();

        // Child exam is current
        ReadingExam child = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(true)
                .isDeleted(false)
                .examName("Child Exam")
                .examDescription("descChild")
                .urlSlug("slugChild")
                .part1(p1)
                .build();

        // Parent exam is not current, has the child in its children list
        ReadingExam parent = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .isCurrent(false)
                .isDeleted(false)
                .children(List.of(child))
                .build();

        when(readingExamRepository.findById(parent.getReadingExamId()))
                .thenReturn(Optional.of(parent));
        when(readingExamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReadingExamResponse resp = service.deleteReadingExam(parent.getReadingExamId().toString(), req);

        // parent and child should both be marked deleted
        assertTrue(parent.getIsDeleted());
        assertTrue(child.getIsDeleted());

        assertEquals("Child Exam", resp.readingExamName());
        assertEquals("t1", resp.readingPassageIdPart1().readingPassageName());

        // repository should save parent and child
        verify(readingExamRepository, times(2)).save(any(ReadingExam.class));
    }


    //TODO test for get list
    @Test
    void getAllReadingExamsForCreator_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(UUID.randomUUID()).partNumber(PartNumber.PART_1)
                .title("t1").content("c1").build();
        ReadingPassage p2 = ReadingPassage.builder()
                .passageId(UUID.randomUUID()).partNumber(PartNumber.PART_2)
                .title("t2").content("c2").build();
        ReadingPassage p3 = ReadingPassage.builder()
                .passageId(UUID.randomUUID()).partNumber(PartNumber.PART_3)
                .title("t3").content("c3").build();

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .examName("exam1")
                .examDescription("desc")
                .urlSlug("slug")
                .part1(p1).part2(p2).part3(p3)
                .isDeleted(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "examName"));
        Page<ReadingExam> page = new PageImpl<>(List.of(exam), pageable, 1);

        when(readingExamRepository.findByIsDeletedFalse(pageable)).thenReturn(page);

        Page<ReadingExamResponse> responsePage = service.getAllReadingExamsForCreator(req, 0, 10, "examName", "asc");

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());

        ReadingExamResponse resp = responsePage.getContent().get(0);
        assertEquals("exam1", resp.readingExamName());
        assertEquals("t1", resp.readingPassageIdPart1().readingPassageName());
        assertEquals("t2", resp.readingPassageIdPart2().readingPassageName());
        assertEquals("t3", resp.readingPassageIdPart3().readingPassageName());
    }

    @Test
    void getAllReadingExamsForCreator_nullRequest_throws() {
        assertThrows(AppException.class, () ->
                service.getAllReadingExamsForCreator(null, 0, 10, "examName", "asc"));
    }

    @Test
    void getAllReadingExamsForCreator_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        assertThrows(AppException.class, () ->
                service.getAllReadingExamsForCreator(req, 0, 10, "examName", "asc"));
    }
    @Test
    void getAllReadingExams_withParentAndChildExams_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("u1");

        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        // Parent exam
        ReadingExam parentExam = ReadingExam.builder()
                .readingExamId(parentId)
                .examName("parentExam")
                .examDescription("parentDesc")
                .urlSlug("parentSlug")
                .isDeleted(false)
                .isCurrent(true)
                .build();

        // Child exam
        ReadingExam childExam = ReadingExam.builder()
                .readingExamId(childId)
                .examName("childExam")
                .examDescription("childDesc")
                .urlSlug("childSlug")
                .isDeleted(false)
                .isCurrent(true)
                .parent(parentExam)          // assign parent
                .build();

        // Passages
        ReadingPassage p1 = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .title("t1").content("c1")
                .isDeleted(false)
                .isCurrent(true).build();
        ReadingPassage p2 = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .title("t2").content("c2")
                .isDeleted(false)
                .isCurrent(true).build();
        ReadingPassage p3 = ReadingPassage.builder()
                .passageId(UUID.randomUUID())
                .title("t3").content("c3")
                .isDeleted(false)
                .isCurrent(true).build();

        childExam.setPart1(p1);
        childExam.setPart2(p2);
        childExam.setPart3(p3);

        Page<ReadingExam> pageMock = new PageImpl<>(List.of(childExam));
        when(readingExamRepository.searchCurrentExams(anyString(), any(Pageable.class))).thenReturn(pageMock);

        Page<ReadingExamResponse> result = service.getAllReadingExams(req, 0, 10, "createdAt", "asc", "keyword");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        ReadingExamResponse resp = result.getContent().get(0);

        // The examId should come from parent
        assertEquals(parentId.toString(), resp.readingExamId());

        // Passages should be correctly mapped
        assertEquals("t1", resp.readingPassageIdPart1().readingPassageName());
        assertEquals("t2", resp.readingPassageIdPart2().readingPassageName());
        assertEquals("t3", resp.readingPassageIdPart3().readingPassageName());
    }


    @Test
    void getAllReadingExams_nullRequest_throws() {
        assertThrows(AppException.class, () ->
                service.getAllReadingExams(null, 0, 10, "examName", "asc", ""));
    }

    @Test
    void getAllReadingExams_unauthorized_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn(null);

        assertThrows(AppException.class, () ->
                service.getAllReadingExams(req, 0, 10, "examName", "asc", ""));
    }

    @Test
    void getAllReadingExams_repositoryThrows_throwsAppException() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        when(readingExamRepository.searchCurrentExams("", pageable)).thenThrow(new RuntimeException("DB error"));

        assertThrows(AppException.class, () ->
                service.getAllReadingExams(req, 0, 10, null, null, ""));
    }
    @Test
    void getTaskTitle_success_returnsTaskTitles() {
        // Prepare mock data
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ReadingExam exam1 = ReadingExam.builder()
                .readingExamId(id1)
                .examName("Exam 1")
                .build();
        ReadingExam exam2 = ReadingExam.builder()
                .readingExamId(id2)
                .examName("Exam 2")
                .build();

        List<UUID> ids = List.of(id1, id2);

        // Mock repository
        when(readingExamRepository.findAllById(ids))
                .thenReturn(List.of(exam1, exam2));

        // Call the service
        List<TaskTitle> result = service.getTaskTitle(ids);

        // Verify repository call
        verify(readingExamRepository).findAllById(ids);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(id1, result.get(0).taskId());
        assertEquals("Exam 1", result.get(0).title());

        assertEquals(id2, result.get(1).taskId());
        assertEquals("Exam 2", result.get(1).title());
    }
    @Test
    void getAllActiveReadingExams_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        ReadingPassage p1 = ReadingPassage.builder().passageId(part1).title("t1").content("c1").isCurrent(true).isDeleted(false).build();
        ReadingPassage p2 = ReadingPassage.builder().passageId(part2).title("t2").content("c2").isCurrent(true).isDeleted(false).build();
        ReadingPassage p3 = ReadingPassage.builder().passageId(part3).title("t3").content("c3").isCurrent(true).isDeleted(false).build();

        ReadingExam exam = ReadingExam.builder()
                .readingExamId(UUID.randomUUID())
                .examName("Exam 1")
                .examDescription("Desc 1")
                .urlSlug("slug1")
                .part1(p1).part2(p2).part3(p3)
                .isDeleted(false)
                .isCurrent(true)
                .status(1)
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        when(readingExamRepository.findByIsDeletedFalseAndIsCurrentTrueAndStatusTrue("", pageable))
                .thenReturn(new PageImpl<>(List.of(exam), pageable, 1));

        Page<ReadingExamResponse> result = service.getAllActiveReadingExams(req, 0, 10, null, null, "");

        assertEquals(1, result.getTotalElements());
        assertEquals("Exam 1", result.getContent().get(0).readingExamName());
        assertEquals("t1", result.getContent().get(0).readingPassageIdPart1().readingPassageName());
    }




}
