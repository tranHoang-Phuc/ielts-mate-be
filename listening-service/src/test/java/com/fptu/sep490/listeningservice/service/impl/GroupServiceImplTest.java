package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupServiceImplTest {

    @Mock
    ListeningTaskRepository listeningTaskRepository;
    @Mock
    QuestionGroupRepository questionGroupRepository;
    @Mock
    DragItemRepository dragItemRepository;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    ChoiceRepository choiceRepository;
    @Mock
    Helper helper;
    @Mock
    HttpServletRequest httpServletRequest;

    @InjectMocks
    GroupServiceImpl groupService;

    private UUID listeningTaskId;
    private UUID groupId;
    private ListeningTask listeningTask;
    private QuestionGroupCreationRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listeningTaskId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        listeningTask = ListeningTask.builder()
                .taskId(listeningTaskId)
                .build();

        request = new QuestionGroupCreationRequest(
                1, "Section A", QuestionType.MULTIPLE_CHOICE.ordinal(), "Instruction here"
        );
    }

    @Test
    void createGroup_ShouldThrow_WhenListeningTaskNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(listeningTaskId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupService.createGroup(listeningTaskId.toString(), request, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void createGroup_ShouldReturnResponse_WhenSuccess() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(listeningTaskId)).thenReturn(Optional.of(listeningTask));

        QuestionGroup savedGroup = QuestionGroup.builder()
                .groupId(groupId)
                .listeningTask(listeningTask)
                .sectionOrder(request.sectionOrder())
                .sectionLabel(request.sectionLabel())
                .instruction(request.instruction())
                .questionType(QuestionType.fromValue(request.questionType()))
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .version(1)
                .createdBy("user1")
                .updatedBy("user1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(questionGroupRepository.save(any(QuestionGroup.class))).thenReturn(savedGroup);

        QuestionGroupResponse response = groupService.createGroup(listeningTaskId.toString(), request, httpServletRequest);

        assertNotNull(response);
        assertEquals(groupId, response.groupId());
        assertEquals(request.sectionLabel(), response.sectionLabel());
        assertEquals(QuestionType.MULTIPLE_CHOICE.ordinal(), response.questionType());
        verify(questionGroupRepository).save(any(QuestionGroup.class));
    }

    @Test
    void deleteGroup_ShouldThrow_WhenListeningTaskNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(listeningTaskId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupService.deleteGroup(listeningTaskId, groupId, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void deleteGroup_ShouldThrow_WhenGroupNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(listeningTaskId)).thenReturn(Optional.of(listeningTask));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupService.deleteGroup(listeningTaskId, groupId, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void deleteGroup_ShouldThrow_WhenGroupListeningTaskMismatch() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(listeningTaskId)).thenReturn(Optional.of(listeningTask));

        ListeningTask otherTask = ListeningTask.builder().taskId(UUID.randomUUID()).build();
        QuestionGroup group = QuestionGroup.builder()
                .groupId(groupId)
                .listeningTask(otherTask)
                .build();

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        AppException ex = assertThrows(AppException.class, () ->
                groupService.deleteGroup(listeningTaskId, groupId, httpServletRequest)
        );

        assertEquals(Constants.ErrorCode.TASK_NOT_MATCH_GROUP, ex.getBusinessErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }

    @Test
    void deleteGroup_ShouldSuccess_WhenValid() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(listeningTaskRepository.findById(listeningTaskId)).thenReturn(Optional.of(listeningTask));

        QuestionGroup group = QuestionGroup.builder()
                .groupId(groupId)
                .listeningTask(listeningTask)
                .isDeleted(false)
                .build();

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        groupService.deleteGroup(listeningTaskId, groupId, httpServletRequest);

        assertTrue(group.getIsDeleted());
        assertEquals("user1", group.getUpdatedBy());
        verify(questionGroupRepository).save(group);
    }
}
