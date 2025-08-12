package com.fptu.sep490.personalservice.listener;

import com.fptu.sep490.commonlibrary.constants.Operation;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.event.TopicMasterRequest;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.model.TopicMaster;
import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import com.fptu.sep490.personalservice.repository.TopicMaterRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class TopicMasterListener {
    TopicMaterRepository topicMasterRepository;

    @KafkaListener(topics = "${kafka.topic.topic-master}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleTopicMaster(TopicMasterRequest request)  {
        switch (request.operation()) {
            case Operation.CREATE:
                 TopicMaster topicMaster = TopicMaster.builder()
                         .topicName(request.title())
                         .type(safeEnumFromOrdinal(TaskType.values(), request.type()))
                         .build();
                topicMasterRepository.save(topicMaster);
                break;

            case Operation.UPDATE :
                TopicMaster topic = topicMasterRepository.findByTaskId(request.taskId());
                topic.setTopicName(request.title());
                topicMasterRepository.save(topic);
                break;
            case Operation.DELETE :
                topicMasterRepository.deleteByTaskId(request.taskId());
                break;
        }
    }

    private <T extends Enum<T>> T safeEnumFromOrdinal(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        return values[ordinal];
    }
}
