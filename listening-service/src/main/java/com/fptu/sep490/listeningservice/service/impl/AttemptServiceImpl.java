package com.fptu.sep490.listeningservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.*;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.listeningservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.listeningservice.service.AttemptService;
import com.fptu.sep490.listeningservice.viewmodel.response.AttemptResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class AttemptServiceImpl implements AttemptService {

    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;

    ObjectMapper objectMapper;
    KeyCloakTokenClient keyCloakTokenClient;
    KeyCloakUserClient keyCloakUserClient;
    RedisService redisService;
    Helper helper;

    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;
    @Override
    @Transactional
    public AttemptResponse createAttempt(UUID listeningTaskId, HttpServletRequest request) {
        String userId = helper.getUserIdFromToken(request);
        ListeningTask originalTask = listeningTaskRepository.findById(listeningTaskId)
                .orElseThrow(
                        () -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        ListeningTask currentVersion = listeningTaskRepository.findLastestVersion(listeningTaskId);
        if (currentVersion.getStatus() == null || currentVersion.getStatus() != Status.PUBLISHED) {
            throw new AppException(
                    Constants.ErrorCodeMessage.LISTENING_TASK_NOT_ACTIVATED,
                    Constants.ErrorCode.LISTENING_TASK_NOT_ACTIVATED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        List<QuestionGroup> originalVersionGroupQuestion = questionGroupRepository
                .findOriginalVersionByTaskId(originalTask.getTaskId());

        List<QuestionGroup> currentVersionGroupQuestion = questionGroupRepository
                .findAllLatestVersionByTaskId(originalTask.getTaskId());

        Map<QuestionGroup, List<DragItem>> currentGroupMapCurrentDragItem = new HashMap<>();
        Map<UUID,List<Question>>  currentGroupIdQuestionId = new HashMap<>();
        originalVersionGroupQuestion.forEach(g -> {
            // Get All Current DragItem
            List<DragItem> currentVersionDragItem = dragItemRepository.findCurrentVersionByGroupId(g.getGroupId());
            QuestionGroup latestVersion = questionGroupRepository.findLatestVersionByOriginalId(g.getGroupId());
            currentGroupMapCurrentDragItem.put(latestVersion, currentVersionDragItem);

            // Get All Original Question
            List<UUID> originalQuestionId = questionRepository.findOriginalVersionByGroupId(g.getGroupId());
            List<Question> currentQuestion = questionRepository.findAllCurrentVersion(originalQuestionId);
            currentGroupIdQuestionId.put(g.getGroupId(), currentQuestion);

            Map<UUID, List<Choice>> questionIdMapCurrentChoice = new HashMap<>();
            currentQuestion.forEach(q -> {
                if(q.getIsOriginal()) {
                    List<Choice> currentChoice = choiceRepository.findCurrentVersionByQuestionId(q.getQuestionId());
                    questionIdMapCurrentChoice.put(q.getQuestionId(), currentChoice);
                } else {
                    List<Choice> currentChoice = choiceRepository.findCurrentVersionByQuestionId(q.getParent().getQuestionId());
                    questionIdMapCurrentChoice.put(q.getParent().getQuestionId(), currentChoice);
                }
            });
        });



        return null;
    }
}
