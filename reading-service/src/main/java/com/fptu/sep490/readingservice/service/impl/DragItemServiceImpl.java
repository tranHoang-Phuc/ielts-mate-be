package com.fptu.sep490.readingservice.service.impl;


import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.repository.DragItemRepository;
import com.fptu.sep490.readingservice.repository.QuestionGroupRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.service.DragItemService;
import com.fptu.sep490.readingservice.viewmodel.request.CreateDragItemRequest;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemListResponse;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemResponse;
import com.fptu.sep490.readingservice.viewmodel.request.UpdateDragItemRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DragItemServiceImpl implements DragItemService {

    private final DragItemRepository dragItemRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final QuestionRepository questionRepository;
    private final Helper helper;
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    RedisService redisService;

    @Override
    public List<DragItemResponse> createDragItem(UUID groupId, CreateDragItemRequest request, HttpServletRequest httpServletRequest) throws AppException {
        // 1. Kiểm tra group tồn tại

        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        List<DragItem> dragItemList = new ArrayList<>();
        for (String item : request.getItems()) {
            if (item == null || item.trim().isEmpty()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.INVALID_REQUEST,
                        Constants.ErrorCode.INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST.value()
                );
            }

            DragItem entity = DragItem.builder()
                    .questionGroup(group)
                    .content(item.trim())
                    .isCurrent(true)
                    .version(1)
                    .isOriginal(true)
                    .isDeleted(false)
                    .build();

            dragItemList.add(entity);
        }






        List<DragItem> data =  dragItemRepository.saveAll(dragItemList);

        // 4. update user cho Group Question
        String userId = helper.getUserIdFromToken(httpServletRequest);
        group.setUpdatedBy(userId);
        questionGroupRepository.save(group);


        return data.stream().map(
                item -> DragItemResponse.builder()
                        .group_id(groupId.toString())
                        .item_id(item.getDragItemId().toString())
                        .content(item.getContent())
                        .build()
        ).toList();
    }

    @Override
    @Transactional
    public DragItemResponse updateDragItem(
            UUID groupId,
            UUID itemId,
            UpdateDragItemRequest request,
            HttpServletRequest httpServletRequest
    ) throws AppException {
        // 1. Kiểm tra QuestionGroup tồn tại
        int currentVersion = 0;
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // 2. Tìm DragItem theo groupId và itemId
        DragItem existing = dragItemRepository
                .findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(itemId, groupId, false)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.DRAG_ITEM_NOT_FOUND,
                        Constants.ErrorCode.DRAG_ITEM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        // Update current of previous items
        List<DragItem> previousItems = dragItemRepository.findPreviousDragItems( itemId);

        for( DragItem previousItem : previousItems) {
            previousItem.setIsCurrent(false);
            if( previousItem.getVersion() > currentVersion) {
                currentVersion = previousItem.getVersion();
            }
        }

        dragItemRepository.saveAll(previousItems);

        if (!existing.getQuestionGroup().getGroupId().equals(groupId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // Tạo dragItem mới với nội dung cập nhật
        String newContent = request.getContent().trim();
        if (newContent.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        DragItem updatedItem = DragItem.builder()
                .content(newContent)
                .questionGroup(group)
                .isCurrent(true)
                .version(currentVersion + 1)
                .isOriginal(false)
                .isDeleted(false)
                .build();


        // 4. Lưu DragItem
        DragItem updated = dragItemRepository.save(updatedItem);

        // 5. Cập nhật updatedBy, updatedAt cho QuestionGroup
        String userId = helper.getUserIdFromToken(httpServletRequest);
        group.setUpdatedBy(userId);
        questionGroupRepository.save(group);

        // 6. Trả về response
        return DragItemResponse.builder()
                .group_id(groupId.toString())
                .item_id(itemId.toString())
                .content(updated.getContent())
                .build();
    }

    @Override
    public void deleteDragItem(UUID groupId,
                               UUID itemId, HttpServletRequest httpServletRequest) throws AppException {

        // 1. Kiểm tra QuestionGroup tồn tại
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // 2. Tìm DragItem theo dragItemId & questionGroup.groupId
        DragItem existing = dragItemRepository
                .findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(itemId, groupId, false)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.DRAG_ITEM_NOT_FOUND,
                        Constants.ErrorCode.DRAG_ITEM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (!existing.getQuestionGroup().getGroupId().equals(groupId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        List<DragItem> previousItems = dragItemRepository.findPreviousDragItems( itemId);

        for( DragItem previousItem : previousItems) {
            previousItem.setIsDeleted(true);
            previousItem.setIsCurrent(false);
        }


        // 3. Xóa DragItem
        dragItemRepository.saveAll(previousItems);

        // 4. (Tuỳ chọn) Cập nhật updatedBy, updatedAt cho QuestionGroup
        // Nếu bạn muốn ghi lại ai xóa, có thể uncomment:
         String userId = helper.getUserIdFromToken(httpServletRequest);
         group.setUpdatedBy(userId);
         questionGroupRepository.save(group);
    }

    @Override
    @Transactional(readOnly = true)
    public DragItemResponse getDragItemById(UUID groupId,
                                            UUID itemId) throws AppException {

        // 2. Tìm DragItem theo dragItemId và questionGroup.groupId
        DragItem existing = dragItemRepository
                .findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(itemId, groupId, false)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.DRAG_ITEM_NOT_FOUND,
                        Constants.ErrorCode.DRAG_ITEM_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // 3. Trả về response
        return DragItemResponse.builder()
                .group_id(groupId.toString())
                .item_id(existing.getDragItemId().toString())
                .content(existing.getContent())
                .build();
    }

    @Override
    public DragItemListResponse getAllDragItemsByGroup(UUID groupId) throws AppException {
        // 1. Kiểm tra QuestionGroup tồn tại
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // 2. Lấy danh sách DragItem theo groupId
        var items = dragItemRepository.findAllByQuestionGroup_GroupId(groupId);

        // 3. Chuyển đổi sang response
        DragItemListResponse response = DragItemListResponse.builder()
                .group_id(groupId.toString())
                .items(items.stream()
                        .map(item -> new DragItemListResponse.DragItemSummaryResponse(item.getDragItemId().toString(), item.getContent()))
                        .toList())
                .build();

        return response;
    }
}
