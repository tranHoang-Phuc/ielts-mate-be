package com.fptu.sep490.personalservice.model;

import com.fptu.sep490.personalservice.model.enumeration.LearningStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_flashcard_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserModuleProgress {
        @Id
        @GeneratedValue(generator = "UUID")
        @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
        @Column(name = "progress_id", updatable = false, nullable = false)
        private UUID progressId;

        @Column(name = "user_id", nullable = false)
        private String userId; //  reference a User id can access this module

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "module_id", nullable = false)
        private Module module;

        @Column(name = "status")
        private LearningStatus status;

        @Column(name="note" , columnDefinition = "TEXT")
        private String note;

        @Column(name = "last_read_index")
        private Integer lastReadIndex;

        @Column(name = "last_read_at")
        private LocalDateTime lastReadAt;

        @Column(name = "update_at")
        @UpdateTimestamp
        private LocalDateTime updatedAt;
}
