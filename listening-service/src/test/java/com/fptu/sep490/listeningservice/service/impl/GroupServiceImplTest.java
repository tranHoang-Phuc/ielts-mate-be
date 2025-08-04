package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.repository.ListeningTaskRepository;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @InjectMocks
    private GroupServiceImpl groupService;

    @Mock
    private ListeningTaskRepository listeningTaskRepository;

    @Mock
    private QuestionGroupRepository questionGroupRepository;

    @Mock
    private Helper helper;

    @Mock
    private HttpServletRequest httpServletRequest;

    private UUID groupId;
    private UUID taskId;
    private String userId;
    private ListeningTask mockListeningTask;
    private QuestionGroupCreationRequest creationRequest;
    private QuestionGroup mockGroup;

    @BeforeEach
    void setUp() {
        groupId = UUID.fromString("test-group-id");
        taskId = UUID.fromString("test-task-id");
        userId = "mocked-user-id";
        mockListeningTask = ListeningTask.builder()
                .taskId(taskId)
                .build();

        creationRequest = QuestionGroupCreationRequest.builder()
                .sectionOrder(1)
                .sectionLabel("Section 1")
                .questionType(QuestionType.MULTIPLE_CHOICE.ordinal())
                .instruction("Instruction")
                .build();

        mockGroup = QuestionGroup.builder()
                .groupId(groupId)
                .listeningTask(mockListeningTask)
                .sectionOrder(1)
                .sectionLabel("Section 1")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .instruction("Instruction")
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .version(1)
                .createdBy(userId)
                .updatedBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createGroup_success_shouldReturnResponse() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(mockListeningTask));
        when(questionGroupRepository.save(any(QuestionGroup.class))).thenReturn(mockGroup);

        QuestionGroupResponse response = groupService.createGroup(taskId.toString(), creationRequest, httpServletRequest);

        assertNotNull(response);
        assertEquals(groupId, response.groupId());
        assertEquals(taskId, response.listeningTaskId());
        assertEquals("Section 1", response.sectionLabel());
        assertEquals(userId, response.createdBy());
        verify(questionGroupRepository).save(any(QuestionGroup.class));
    }

    @Test
    void createGroup_listeningTaskNotFound_shouldThrowAppException() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupService.createGroup(taskId.toString(), creationRequest, httpServletRequest));
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.NOT_FOUND), ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void createGroup_invalidTaskId_shouldThrowException() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                groupService.createGroup("invalid-uuid", creationRequest, httpServletRequest));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void createGroup_repositoryThrows_shouldPropagate() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(mockListeningTask));
        when(questionGroupRepository.save(any(QuestionGroup.class))).thenThrow(new RuntimeException("DB error"));

        Exception ex = assertThrows(RuntimeException.class, () ->
                groupService.createGroup(taskId.toString(), creationRequest, httpServletRequest));
        assertEquals("DB error", ex.getMessage());
    }

    @Test
    void deleteGroup_success_shouldMarkDeleted() throws Exception {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(mockListeningTask));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(questionGroupRepository.save(any(QuestionGroup.class))).thenReturn(mockGroup);

        assertDoesNotThrow(() -> groupService.deleteGroup(taskId, groupId, httpServletRequest));
        verify(questionGroupRepository).save(any(QuestionGroup.class));
    }

    @Test
    void deleteGroup_listeningTaskNotFound_shouldThrowAppException() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupService.deleteGroup(taskId, groupId, httpServletRequest));
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.NOT_FOUND), ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void deleteGroup_groupNotFound_shouldThrowAppException() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(mockListeningTask));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupService.deleteGroup(taskId, groupId, httpServletRequest));
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.NOT_FOUND), ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatusCode());
    }

    @Test
    void deleteGroup_groupTaskMismatch_shouldThrowAppException() {
        ListeningTask otherTask = ListeningTask.builder().taskId(UUID.randomUUID()).build();
        QuestionGroup wrongGroup = QuestionGroup.builder()
                .groupId(groupId)
                .listeningTask(otherTask)
                .build();

        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(userId);
        when(listeningTaskRepository.findById(taskId)).thenReturn(Optional.of(mockListeningTask));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(wrongGroup));

        AppException ex = assertThrows(AppException.class, () ->
                groupService.deleteGroup(taskId, groupId, httpServletRequest));
        assertEquals(MessagesUtils.getMessage(Constants.ErrorCodeMessage.INVALID_REQUEST), ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatusCode());
    }
}