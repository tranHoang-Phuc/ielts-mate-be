package com.fptu.sep490.readingservice.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.repository.DragItemRepository;
import com.fptu.sep490.readingservice.repository.QuestionGroupRepository;
import com.fptu.sep490.readingservice.viewmodel.request.CreateDragItemRequest;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemListResponse;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemResponse;
import com.fptu.sep490.readingservice.viewmodel.request.UpdateDragItemRequest;
import com.fptu.sep490.readingservice.viewmodel.response.UserInformationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
public class DragItemServiceImplTest {

    private static final String GROUP_ID = "0f5c3b8a-2d44-4e1f-8b2a-5a6c7d8e9f10";
    private static final String NF_GROUP_ID = "12345678-1234-1234-1234-123456789012"; // Non-existing group ID for testing
    private static final String ITEM_ID = "9c2a1d3e-5b67-4c89-8def-0123456789ab";
    private static final String USER_ID = "creatortest123@gmail.com";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DragItemRepository dragItemRepository;
    @Mock
    private QuestionGroupRepository questionGroupRepository;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private Helper helper;

    @InjectMocks
    private DragItemServiceImpl dragItemService;

    private QuestionGroup group;

    private UserInformationResponse makeUserInfo(String id) {
        return new UserInformationResponse(id, "Student", "Test", id);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(helper.getUserIdFromToken(httpServletRequest)).thenReturn(USER_ID);
        lenient().when(helper.getUserInformationResponse(anyString()))
                .thenAnswer(inv -> makeUserInfo(inv.getArgument(0)));

        group = new QuestionGroup();
        group.setGroupId(UUID.fromString(GROUP_ID));

        lenient().when(questionGroupRepository.findById(UUID.fromString(GROUP_ID)))
                .thenReturn(Optional.of(group));

        lenient().when(questionGroupRepository.findById(UUID.fromString(NF_GROUP_ID)))
                .thenReturn(Optional.empty());
    }

    // Utility method to check exceptions
    private void assertAppEx(AppException ex, String bizCode, int httpStatus, String expectedMsgContainsOrExact) {
        assertThat(ex.getBusinessErrorCode()).isEqualTo(bizCode);
        assertThat(ex.getHttpStatusCode()).isEqualTo(httpStatus);
        assertThat(ex.getMessage()).contains(expectedMsgContainsOrExact);
    }

    // Print the request and response in a pretty JSON format for debugging
    private void printRequestAndResponse(String label, Object obj) {
        try {
            System.out.println(label + ": " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // Print the request and exception message for debugging
    private void printRequestAndException(String label, Exception ex) {
        try {
            System.out.println(label + ": " + ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create Drag Item Tests

    @Test
    void createDragItem_groupNotFound_shouldThrowException() {
        // Arrange: Prepare the request
        CreateDragItemRequest request = new CreateDragItemRequest("Drag Item Content");

        // Act: Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.createDragItem(UUID.fromString(NF_GROUP_ID), request, httpServletRequest));

//        // Print request and exception message
//        printRequestAndResponse("Request", request);
//        printRequestAndException("Exception", ex);

        // Assert: Check the exception details
        assertAppEx(ex, Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "Question group not found");
    }

    @Test
    void createDragItem_invalidContent_shouldThrowException() {
        // Arrange: Prepare an invalid request with null content
        CreateDragItemRequest request = new CreateDragItemRequest(null);

        // Act: Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.createDragItem(UUID.fromString(GROUP_ID), request, httpServletRequest));

//        // Print request and exception message
//        printRequestAndResponse("Request", request);
//        printRequestAndException("Exception", ex);

        // Assert: Check the exception details
        assertAppEx(ex, Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value(), "Invalid request");
    }

    @Test
    void createDragItem_emptyContent_shouldThrowException() {
        // Arrange: Prepare an invalid request with empty content
        CreateDragItemRequest request = new CreateDragItemRequest("   ");  // Empty content after trimming

        // Act: Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.createDragItem(UUID.fromString(GROUP_ID), request, httpServletRequest));

//        // Print request and exception message
//        printRequestAndResponse("Request", request);
//        printRequestAndException("Exception", ex);

        // Assert: Check the exception details
        assertAppEx(ex, Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value(), "Invalid request");
    }
    @Test
    void createDragItem_success() {
        // Arrange: Prepare the request
        CreateDragItemRequest request = new CreateDragItemRequest("Drag Item Content");

        // Mock the DragItem with valid UUID for dragItemId and content
        DragItem mockDragItem = new DragItem();
        mockDragItem.setDragItemId(UUID.randomUUID()); // Ensure dragItemId is not null
        mockDragItem.setContent("Drag Item Content"); // Set content to match the request

        lenient().when(dragItemRepository.saveAll(any())).thenReturn(Collections.singletonList(mockDragItem));

//        // Print request for debugging
//        printRequestAndResponse("Request", request);

        // Act: Call the service method
        var response = dragItemService.createDragItem(UUID.fromString(GROUP_ID), request, httpServletRequest);

//        // Print response for debugging
//        printRequestAndResponse("Response", response);

        // Assert: Check that the response is correct
        assertThat(response).isNotNull();
        assertThat(response.get(0).getContent()).isEqualTo("Drag Item Content"); // Ensure content is set properly
        assertThat(response.get(0).getItem_id()).isNotNull(); // Ensure the itemId is correctly returned
    }


    // Update Drag Item Tests

    @Test
    void updateDragItem_groupNotFound_shouldThrowException() {
        // Arrange: Prepare the request
        UpdateDragItemRequest request = new UpdateDragItemRequest("Updated Content");

        // Mock the group lookup to return an empty Optional
        lenient().when(questionGroupRepository.findById(UUID.fromString(GROUP_ID)))
                .thenReturn(Optional.empty());

//        // Print request for debugging
//        printRequestAndResponse("Request to update DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        // Act: Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.updateDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), request, httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        // Assert: Check the exception details
        assertAppEx(ex, Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "Question group not found");
    }

    @Test
    void updateDragItem_dragItemNotFound_shouldThrowException() {
        // Arrange: Prepare the request
        UpdateDragItemRequest request = new UpdateDragItemRequest("Updated Content");

        // Mock the DragItem lookup to return an empty Optional
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.empty());

//        // Print request for debugging
//        printRequestAndResponse("Request to update DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        // Act: Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.updateDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), request, httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        // Assert: Check the exception details
        assertAppEx(ex, Constants.ErrorCode.DRAG_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "Drag item not found");
    }

    @Test
    void updateDragItem_groupIdMismatch_shouldThrowException() {
        // Arrange: Prepare the existing DragItem object
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));
        // Act: Simulate the mismatch of groupId (with a random valid UUID for mismatched groupId)
        UUID mismatchedGroupId = UUID.randomUUID();
        QuestionGroup mismatchedGroup = new QuestionGroup();
        mismatchedGroup.setGroupId(mismatchedGroupId);

        existing.setQuestionGroup(mismatchedGroup);

        // Prepare the request with the updated content
        UpdateDragItemRequest request = new UpdateDragItemRequest("Updated Content");

        // Mock the behavior of finding the existing DragItem by its ID and group
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing)); // Return the existing item if found

//        // Print request for debugging
//        printRequestAndResponse("Request to update DragItem", "Group ID: " + mismatchedGroupId + ", Item ID: " + ITEM_ID);

        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.updateDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), request, httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        // Assert: Check that the exception message contains "Invalid request"
        assertAppEx(ex, Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value(), "Invalid request");
    }

    @Test
    void updateDragItem_invalidContent_shouldThrowException() {
        // Arrange: Prepare the existing DragItem object
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));
        existing.setQuestionGroup(group);

        // Prepare the request with empty content
        UpdateDragItemRequest request = new UpdateDragItemRequest("   ");  // Empty content after trimming

        // Mock the behavior of finding the existing DragItem by its ID and group
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing)); // Return the existing item if found

//        // Print request for debugging
//        printRequestAndResponse("Request to update DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        // Act: Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.updateDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), request, httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        // Assert: Check the exception details
        assertAppEx(ex, Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value(), "Invalid request");
    }

    @Test
    void updateDragItem_previousItemsUpdate() {
        // Arrange: Prepare the existing DragItem object
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));
        existing.setQuestionGroup(group);
        existing.setContent("Original Content");

        // Prepare the request with the updated content
        UpdateDragItemRequest request = new UpdateDragItemRequest("Updated Content");

        // Mock repository behavior
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing));  // Return the existing item
        lenient().when(dragItemRepository.findPreviousDragItems(UUID.fromString(ITEM_ID)))
                .thenReturn(Collections.singletonList(existing)); // Simulate previous items

        // Create a mock DragItem with the updated content
        DragItem updatedDragItem = new DragItem();
        updatedDragItem.setDragItemId(UUID.fromString(ITEM_ID));
        updatedDragItem.setContent("Updated Content");
        lenient().when(dragItemRepository.save(any(DragItem.class))).thenReturn(updatedDragItem);

//        // Print request for debugging
//        printRequestAndResponse("Request to update DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        // Act: Call the service method
        DragItemResponse response = dragItemService.updateDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), request, httpServletRequest);

//        // Print response for debugging
//        printRequestAndResponse("Response", response);

        // Assert: Ensure the previous DragItems are updated and saved
        verify(dragItemRepository, times(1)).saveAll(anyList());  // Verify that the previous items are updated

        // Assert the response content is updated correctly
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Updated Content");  // Ensure the content is updated
        assertThat(response.getItem_id()).isEqualTo(ITEM_ID);  // Ensure the correct itemId is returned
    }

    @Test
    void updateDragItem_success() {
        // Prepare the existing DragItem object
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));
        existing.setQuestionGroup(group);
        existing.setContent("Original Content");  // Initial content

        // Prepare the request with the updated content
        UpdateDragItemRequest request = new UpdateDragItemRequest("Updated Content");

        // Mock repository behavior
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing));  // Return the existing item
        lenient().when(dragItemRepository.findPreviousDragItems(UUID.fromString(ITEM_ID))).thenReturn(Collections.emptyList());

        // Create a mock DragItem with the updated content
        DragItem updatedDragItem = new DragItem();
        updatedDragItem.setDragItemId(UUID.fromString(ITEM_ID));
        updatedDragItem.setContent("Updated Content");
        lenient().when(dragItemRepository.save(any(DragItem.class))).thenReturn(updatedDragItem);

//        // Print request for debugging
//        printRequestAndResponse("Request to update DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        // Act: Call the service method
        DragItemResponse response = dragItemService.updateDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), request, httpServletRequest);

//        // Print the response in a pretty format
//        printRequestAndResponse("Response", response);

        // Assert the response content is updated correctly
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Updated Content");  // Ensure the content is updated
        assertThat(response.getItem_id()).isEqualTo(ITEM_ID);  // Ensure the correct itemId is returned
    }

    // Delete Drag Item Tests

    // Test Case 2: Group not found
    @Test
    void deleteDragItem_groupNotFound_shouldThrowException() {

//        // Print request for debugging
//        printRequestAndResponse("Request to delete DragItem", "Group ID: " + NF_GROUP_ID + ", Item ID: " + ITEM_ID);

        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.deleteDragItem(UUID.fromString(NF_GROUP_ID), UUID.fromString(ITEM_ID), httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        assertAppEx(ex, Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "Question group not found");
    }

    // Test Case 3: DragItem not found
    @Test
    void deleteDragItem_dragItemNotFound_shouldThrowException() {
        UUID nfItemId = UUID.randomUUID(); // Non-existing item ID for testing
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(nfItemId, UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.empty()); // Simulate DragItem not found

//        // Print request for debugging
//        printRequestAndResponse("Request to delete DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + nfItemId);

        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.deleteDragItem(UUID.fromString(GROUP_ID), nfItemId, httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        assertAppEx(ex, Constants.ErrorCode.DRAG_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "Drag item not found");
    }

    // Test Case 4: Mismatched GroupId
    @Test
    void deleteDragItem_groupIdMismatch_shouldThrowException() {
        // Simulate mismatched groupId
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));

        QuestionGroup mismatchedGroup = new QuestionGroup();
        mismatchedGroup.setGroupId(UUID.randomUUID());
        existing.setQuestionGroup(mismatchedGroup);

        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing)); // Simulate finding the DragItem

//        // Print request for debugging
//        printRequestAndResponse("Request to delete DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.deleteDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), httpServletRequest));

//        // Print exception for debugging
//        printRequestAndException("Exception", ex);

        assertAppEx(ex, Constants.ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST.value(), "Invalid request");
    }

    // Test Case 5: Successful deletion with updatedBy
    @Test
    void deleteDragItem_updateGroupWithUser() {
        // Arrange: Prepare the existing DragItem object
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));
        existing.setQuestionGroup(group);

        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing));
        lenient().when(dragItemRepository.findPreviousDragItems(UUID.fromString(ITEM_ID))).thenReturn(Collections.emptyList());

//        // Print request for debugging
//        printRequestAndResponse("Request to delete DragItem", "Group ID: " + GROUP_ID + ", Item ID: " + ITEM_ID);

        dragItemService.deleteDragItem(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID), httpServletRequest);

//        // Print success message and verify save method is called
//        System.out.println("DragItem deleted successfully. Verifying group save call...");
        verify(questionGroupRepository, times(1)).save(group); // Verify that group is updated with user info
//        System.out.println("Group save call verified after deletion.");
    }



    // Get Drag Item Tests

    @Test
    void getDragItemById_success() {
        DragItem existing = new DragItem();
        existing.setDragItemId(UUID.fromString(ITEM_ID));
        existing.setContent("Test Content");
        existing.setQuestionGroup(group);

        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.of(existing));

        DragItemResponse response = dragItemService.getDragItemById(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID));

//        // Print the response for debugging
//        printRequestAndResponse("Response", response);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Test Content");
    }

    @Test
    void getDragItemById_notFound_shouldThrowException() {
        lenient().when(dragItemRepository.findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID.fromString(ITEM_ID), UUID.fromString(GROUP_ID), false))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.getDragItemById(UUID.fromString(GROUP_ID), UUID.fromString(ITEM_ID)));

//        // Print the exception message for debugging
//        printRequestAndException("Exception", ex);

        assertThat(ex.getMessage()).contains("Drag item not found");
    }

    // Get All Drag Items By Group Tests

    @Test
    void getAllDragItemsByGroup_success() {
        // Prepare the mock DragItems
        DragItem item1 = new DragItem();
        item1.setDragItemId(UUID.randomUUID());
        item1.setContent("Content 1");

        DragItem item2 = new DragItem();
        item2.setDragItemId(UUID.randomUUID());
        item2.setContent("Content 2");

        // Mock the repository behavior
        lenient().when(dragItemRepository.findCurrentVersionsByGroupId(UUID.fromString(GROUP_ID)))
                .thenReturn(List.of(item1, item2)); // Simulate DragItems found

//        // Print request for debugging
//        printRequestAndResponse("Request", GROUP_ID);

        // Call the service method
        DragItemListResponse response = dragItemService.getAllDragItemsByGroup(UUID.fromString(GROUP_ID));

//        // Print response for debugging
//        printRequestAndResponse("Response", response);

        // Assert that the response is not null and contains the correct number of items
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getItem_content()).isEqualTo("Content 1");
        assertThat(response.getItems().get(1).getItem_content()).isEqualTo("Content 2");
    }

    // Test Case 2: Get all DragItems by groupId when no DragItems exist
    @Test
    void getAllDragItemsByGroup_emptyList_shouldReturnEmptyResponse() {
        // Mock the repository behavior to return an empty list
        lenient().when(dragItemRepository.findCurrentVersionsByGroupId(UUID.fromString(GROUP_ID)))
                .thenReturn(Collections.emptyList()); // Simulate no DragItems found

//        // Print request for debugging
//        printRequestAndResponse("Request", GROUP_ID);

        // Call the service method
        DragItemListResponse response = dragItemService.getAllDragItemsByGroup(UUID.fromString(GROUP_ID));

//        // Print response for debugging
//        printRequestAndResponse("Response", response);

        // Assert that the response is not null and the items list is empty
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
    }

    // Test Case 3: Get all DragItems by groupId when QuestionGroup does not exist
    @Test
    void getAllDragItemsByGroup_groupNotFound_shouldThrowException() {
        // Mock the repository behavior to simulate QuestionGroup not found

//        // Print request for debugging
//        printRequestAndResponse("Request", NF_GROUP_ID);

        // Call the service method and assert that an exception is thrown
        AppException ex = assertThrows(AppException.class,
                () -> dragItemService.getAllDragItemsByGroup(UUID.fromString(NF_GROUP_ID)));

//        // Print the exception message for debugging
//        printRequestAndException("Exception", ex);

        // Assert that the exception is of the expected type and contains the correct message
        assertAppEx(ex, Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "Question group not found");
    }

}