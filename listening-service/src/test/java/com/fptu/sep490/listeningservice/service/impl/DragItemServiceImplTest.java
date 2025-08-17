package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.DragItem;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.repository.DragItemRepository;
import com.fptu.sep490.listeningservice.repository.QuestionGroupRepository;
import com.fptu.sep490.listeningservice.viewmodel.request.DragItemCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.DragItemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DragItemServiceImplTest {

    @Mock
    QuestionGroupRepository questionGroupRepository;
    @Mock
    DragItemRepository dragItemRepository;
    @Mock
    Helper helper;
    @Mock
    HttpServletRequest httpServletRequest;

    @InjectMocks
    DragItemServiceImpl dragItemService;

    private UUID groupId;
    private UUID itemId;
    private QuestionGroup group;
    private DragItem dragItem;
    private ListeningTask task;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        groupId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        task = ListeningTask.builder()
                .taskId(UUID.randomUUID())
                .build();
        group = QuestionGroup.builder()
                .groupId(groupId)
                .listeningTask(task)
                .sectionOrder(1)
                .sectionLabel("Section A")
                .instruction("Instruction")
                .createdBy("user1")
                .createdAt(LocalDateTime.now())
                .updatedBy("user1")
                .updatedAt(LocalDateTime.now())
                .isCurrent(true)
                .isDeleted(false)
                .version(1)
                .isOriginal(true)
                .build();

        dragItem = DragItem.builder()
                .dragItemId(itemId)
                .content("Item content")
                .isCurrent(true)
                .isDeleted(false)
                .isOriginal(true)
                .version(1)
                .questionGroup(group)
                .children(Collections.emptyList())
                .build();
    }

    // createDragItem
    @Test
    void createDragItem_ShouldThrow_WhenGroupNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                dragItemService.createDragItem(groupId.toString(),
                        new DragItemCreationRequest("content"), httpServletRequest)
        );
        assertEquals(Constants.ErrorCode.NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void createDragItem_ShouldReturnResponse_WhenSuccess() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.save(any(DragItem.class))).thenAnswer(inv -> {
            DragItem item = inv.getArgument(0);
            item.setDragItemId(UUID.randomUUID()); // simulate DB-generated ID
            return item;
        });
        DragItemResponse res = dragItemService.createDragItem(
                groupId.toString(),
                new DragItemCreationRequest("new content"),
                httpServletRequest
        );
        assertNotNull(res);
        assertEquals("new content", res.content());
        verify(dragItemRepository).save(any(DragItem.class));
    }

    // deleteDragItem
    @Test
    void deleteDragItem_ShouldThrow_WhenGroupNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                dragItemService.deleteDragItem(groupId.toString(), itemId.toString(), httpServletRequest)
        );
    }

    @Test
    void deleteDragItem_ShouldThrow_WhenItemNotFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                dragItemService.deleteDragItem(groupId.toString(), itemId.toString(), httpServletRequest)
        );
    }

    @Test
    void deleteDragItem_ShouldSuccess_WhenFound() {
        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(dragItem));
        when(dragItemRepository.save(any())).thenReturn(dragItem);

        DragItemResponse res = dragItemService.deleteDragItem(groupId.toString(), itemId.toString(), httpServletRequest);

        assertTrue(dragItem.getIsDeleted());
        assertEquals(dragItem.getDragItemId().toString(), res.dragItemId());
        verify(dragItemRepository).save(dragItem);
    }

    // getDragItemByGroupIdAndItemId
    @Test
    void getDragItemByGroupIdAndItemId_ShouldThrow_WhenGroupNotFound() {
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                dragItemService.getDragItemByGroupIdAndItemId(groupId.toString(), itemId.toString(), httpServletRequest)
        );
    }

    @Test
    void getDragItemByGroupIdAndItemId_ShouldThrow_WhenItemNotFound() {
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                dragItemService.getDragItemByGroupIdAndItemId(groupId.toString(), itemId.toString(), httpServletRequest)
        );
    }

    @Test
    void getDragItemByGroupIdAndItemId_ShouldThrow_WhenItemNotInGroup() {
        DragItem otherItem = DragItem.builder()
                .questionGroup(QuestionGroup.builder().groupId(UUID.randomUUID()).build())
                .build();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(otherItem));

        assertThrows(AppException.class, () ->
                dragItemService.getDragItemByGroupIdAndItemId(groupId.toString(), itemId.toString(), httpServletRequest)
        );
    }

    @Test
    void getDragItemByGroupIdAndItemId_ShouldReturn_WhenIsCurrentAndNotDeleted() {
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(dragItem));

        DragItemResponse res = dragItemService.getDragItemByGroupIdAndItemId(groupId.toString(), itemId.toString(), httpServletRequest);
        assertEquals(dragItem.getContent(), res.content());
    }

    @Test
    void getDragItemByGroupIdAndItemId_ShouldReturn_WhenFoundInChildren() {
        DragItem child = DragItem.builder()
                .dragItemId(UUID.randomUUID())
                .content("child")
                .isCurrent(true)
                .isDeleted(false)
                .questionGroup(group)
                .build();
        DragItem parent = DragItem.builder()
                .isCurrent(false)
                .children(List.of(child))
                .questionGroup(group)
                .build();

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(parent));

        DragItemResponse res = dragItemService.getDragItemByGroupIdAndItemId(groupId.toString(), itemId.toString(), httpServletRequest);
        assertEquals("child", res.content());
    }

    @Test
    void getDragItemByGroupIdAndItemId_ShouldThrow_WhenNoCurrentFound() {
        DragItem parent = DragItem.builder()
                .questionGroup(QuestionGroup.builder()
                        .groupId(groupId)
                        .build()
                )
                .isCurrent(false)
                .isDeleted(true)
                .children(Collections.emptyList())
                .build();

        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(parent));

        assertThrows(AppException.class, () ->
                dragItemService.getDragItemByGroupIdAndItemId(groupId.toString(), itemId.toString(), httpServletRequest)
        );
    }

    // getDragItemsByGroupId
    @Test
    void getDragItemsByGroupId_ShouldThrow_WhenGroupNotFound() {
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                dragItemService.getDragItemsByGroupId(groupId.toString(), 0, 10, httpServletRequest)
        );
    }

    @Test
    void getDragItemsByGroupId_ShouldReturnPage_WhenFound() {
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findCurrentVersionByGroupIdPaging(eq(groupId), any())).thenReturn(new PageImpl<>(List.of(dragItem)));

        Page<DragItemResponse> page = dragItemService.getDragItemsByGroupId(groupId.toString(), 0, 10, httpServletRequest);
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getDragItemsByGroupId_ShouldThrow_WhenNoCurrentInPage() {
        DragItem di = DragItem.builder()
                .isCurrent(false)
                .isDeleted(true)
                .children(Collections.emptyList())
                .build();
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.findCurrentVersionByGroupIdPaging(eq(groupId), any())).thenReturn(new PageImpl<>(List.of(di)));

        assertThrows(AppException.class, () ->
                dragItemService.getDragItemsByGroupId(groupId.toString(), 0, 10, httpServletRequest)
        );
    }

    // updateDragItem
    @Test
    void updateDragItem_ShouldThrow_WhenItemNotFound() {
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                dragItemService.updateDragItem(groupId.toString(), itemId.toString(),
                        new DragItemCreationRequest("c"), httpServletRequest)
        );
    }

    @Test
    void updateDragItem_ShouldThrow_WhenGroupNotFound() {
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(dragItem));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () ->
                dragItemService.updateDragItem(groupId.toString(), itemId.toString(),
                        new DragItemCreationRequest("c"), httpServletRequest)
        );
    }

    @Test
    void updateDragItem_ShouldThrow_WhenItemNotInGroup() {
        DragItem otherItem = DragItem.builder()
                .questionGroup(QuestionGroup.builder().groupId(UUID.randomUUID()).build())
                .build();
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(otherItem));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThrows(AppException.class, () ->
                dragItemService.updateDragItem(groupId.toString(), itemId.toString(),
                        new DragItemCreationRequest("c"), httpServletRequest)
        );
    }

    @Test
    void updateDragItem_ShouldThrow_WhenNoCurrentFound() {
        DragItem di = DragItem.builder()
                .isCurrent(false)
                .isDeleted(true)
                .children(Collections.emptyList())
                .questionGroup(group)
                .build();
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(di));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThrows(AppException.class, () ->
                dragItemService.updateDragItem(groupId.toString(), itemId.toString(),
                        new DragItemCreationRequest("c"), httpServletRequest)
        );
    }

    @Test
    void updateDragItem_ShouldReturn_WhenSuccess() throws Exception {
        dragItem.setDragItemId(itemId); // gán ID cho dragItem mock

        when(helper.getUserIdFromToken(httpServletRequest)).thenReturn("user1");
        when(dragItemRepository.findById(itemId)).thenReturn(Optional.of(dragItem));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(dragItemRepository.save(any(DragItem.class))).thenAnswer(inv -> {
            DragItem item = inv.getArgument(0);
            if (item.getDragItemId() == null) {
                item.setDragItemId(UUID.randomUUID()); // giả lập DB sinh ID
            }
            return item;
        });

        DragItemResponse res = dragItemService.updateDragItem(
                groupId.toString(),
                itemId.toString(),
                new DragItemCreationRequest("updated"),
                httpServletRequest
        );

        assertEquals("updated", res.content());
        assertNotNull(res.dragItemId()); // đảm bảo ID không null
        verify(dragItemRepository, times(2)).save(any());
    }
}
