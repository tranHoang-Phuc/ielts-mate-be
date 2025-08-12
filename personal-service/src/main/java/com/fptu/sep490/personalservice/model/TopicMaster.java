package com.fptu.sep490.personalservice.model;

import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "topic_master")
public class TopicMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    int id;

    @Column(name = "topic_name")
    String topicName;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    TaskType type;

    @Column(name = "task_id")
    UUID taskId;
}
