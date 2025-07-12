package com.fptu.sep490.personalservice.model;

import com.fptu.sep490.personalservice.model.enumeration.MarkupType;
import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name ="mark_up")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Markup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mark_up_id")
    private int markUpId;

    @Column(name = "mark_up_type")
    @Enumerated(EnumType.ORDINAL)
    private MarkupType markupType;

    @Column(name = "task_type")
    @Enumerated(EnumType.ORDINAL)
    private TaskType taskType;

    @Column(name = "practice_type")
    @Enumerated(EnumType.ORDINAL)
    private PracticeType practiceType;

    @Column(name = "task_id")
    private UUID taskId;
}
