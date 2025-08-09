package com.fptu.sep490.personalservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "flashcard_progress",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"module_user_id", "flashcard_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashCardProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_user_id", nullable = false)
    private ModuleUsers moduleUsers;

    @Column(name = "flashcard_id", nullable = false)
    private String flashcardId;

    @Column(name = "status", nullable = false)
    private Integer status; // 0:new, 1: learning, 2: learned

    @Column(name = "is_highlighted")
    private Boolean isHighlighted = false; // Default to false, indicating not highlighted

}
