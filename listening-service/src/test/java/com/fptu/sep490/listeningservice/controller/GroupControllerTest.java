package com.fptu.sep490.listeningservice.controller;

import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.listeningservice.service.GroupService;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @InjectMocks
    private GroupController groupController;

    @Mock
    private GroupService groupService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private UUID taskId;
    private UUID groupId;
    private QuestionGroupCreationRequest creationRequest;
    private QuestionGroupResponse mockResponse;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        creationRequest = QuestionGroupCreationRequest.builder()
                .sectionOrder(1)
                .sectionLabel("Section 1")
                .questionType(0)
                .instruction("Instruction text")
                .build();

        mockResponse = QuestionGroupResponse.builder()
                .groupId(groupId)
                .listeningTaskId(taskId)
                .sectionOrder(1)
                .sectionLabel("Section 1")
                .questionType(0)
                .instruction("Instruction text")
                .createdBy("user1")
                .createdAt(LocalDateTime.now())
                .updatedBy("user1")
                .updatedAt(LocalDateTime.now())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .isOriginal(true)
                .build();
    }

    @Test
    void createGroup_ShouldReturnCreatedGroup() throws Exception {
        when(groupService.createGroup(eq(taskId.toString()), any(), any())).thenReturn(mockResponse);

        ResponseEntity<BaseResponse<QuestionGroupResponse>> response =
                groupController.createGroup(taskId.toString(), creationRequest, httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(mockResponse, response.getBody().data());
        verify(groupService, times(1)).createGroup(taskId.toString(), creationRequest, httpServletRequest);
    }

    @Test
    void createGroup_WhenServiceThrowsException_ShouldPropagate() throws Exception {
        when(groupService.createGroup(eq(taskId.toString()), any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupController.createGroup(taskId.toString(), creationRequest, httpServletRequest));

        assertEquals("Service error", ex.getMessage());
        verify(groupService, times(1)).createGroup(taskId.toString(), creationRequest, httpServletRequest);
    }

    @Test
    void deleteGroup_ShouldReturnNoContent() throws Exception {
        doNothing().when(groupService).deleteGroup(taskId, groupId, httpServletRequest);

        ResponseEntity<BaseResponse<QuestionGroupResponse>> response =
                groupController.deleteGroup(taskId, groupId, httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals("Question group deleted successfully", response.getBody().message());
        verify(groupService, times(1)).deleteGroup(taskId, groupId, httpServletRequest);
    }

    @Test
    void deleteGroup_WhenServiceThrowsException_ShouldPropagate() throws Exception {
        doThrow(new RuntimeException("Delete error"))
                .when(groupService).deleteGroup(taskId, groupId, httpServletRequest);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupController.deleteGroup(taskId, groupId, httpServletRequest));

        assertEquals("Delete error", ex.getMessage());
        verify(groupService, times(1)).deleteGroup(taskId, groupId, httpServletRequest);
    }
}