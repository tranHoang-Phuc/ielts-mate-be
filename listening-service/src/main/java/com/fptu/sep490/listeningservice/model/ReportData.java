package com.fptu.sep490.listeningservice.model;

import com.fptu.sep490.listeningservice.model.enumeration.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "report_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "question_type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private QuestionType questionType;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @CreationTimestamp
    @Column(name = "checked_date", nullable = false)
    private LocalDate checkedDate;
}
