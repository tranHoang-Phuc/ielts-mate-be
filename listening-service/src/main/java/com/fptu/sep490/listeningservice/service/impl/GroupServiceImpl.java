// file: listening-service/src/main/java/com/fptu/sep490/listeningservice/service/impl/GroupServiceImpl.java
package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.GroupService;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;

    Helper helper;


    // GroupServiceImpl.java
    @Override
    public QuestionGroupResponse createGroup(String listeningTaskId, QuestionGroupCreationRequest request, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        ListeningTask listeningTask = listeningTaskRepository.findById(UUID.fromString(listeningTaskId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        QuestionGroup group = QuestionGroup.builder()
                .listeningTask(listeningTask)
                .sectionOrder(request.sectionOrder())
                .sectionLabel(request.sectionLabel())
                .instruction(request.instruction())
                .questionType(QuestionType.fromValue(request.questionType()))
                .isCurrent(true)
                .isOriginal(true)
                .isDeleted(false)
                .version(1)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        group = questionGroupRepository.save(group);

        return QuestionGroupResponse.builder()
                .groupId(group.getGroupId())
                .listeningTaskId(UUID.fromString(listeningTaskId))
                .sectionOrder(group.getSectionOrder())
                .sectionLabel(group.getSectionLabel())
                .instruction(group.getInstruction())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .isCurrent(group.getIsCurrent())
                .isDeleted(group.getIsDeleted())
                .questionType(group.getQuestionType().ordinal())
                .version(group.getVersion())
                .isOriginal(group.getIsOriginal())
                .parentId(group.getParent() != null ? group.getParent().getGroupId() : null)
                .children(null)
                .dragItems(null)
                .build();
    }

    @Override
    public void deleteGroup(UUID listeningTaskId, UUID groupId, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        ListeningTask listeningTask = listeningTaskRepository.findById(listeningTaskId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (!group.getListeningTask().getTaskId().equals(listeningTask.getTaskId())) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        group.setIsDeleted(true);
        group.setUpdatedBy(userId);
        questionGroupRepository.save(group);
    }
}