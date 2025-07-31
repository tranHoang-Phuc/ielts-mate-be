package com.fptu.sep490.personalservice.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name ="modules_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleUsers {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name ="id", updatable = false, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;

    @Column(name = "user_id")
    private String userId;

    @Column(name="created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "status")
    private Integer status = 0; // 0: pending, 1: allowed, 2: denied

    @Column(name ="updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "last_index_read") // this is index that user last read in module, then when user open it, it will show the last read
    private Integer lastIndexRead = 0;

    @ElementCollection
    @CollectionTable(
            name = "highlighted_flashcards",
            joinColumns = @JoinColumn(name = "module_user_id") // Khóa ngoại trỏ về ModuleUser.id
    )
    @Column(name = "flashcard_id")
    private List<String> highlightedFlashcardIds = new ArrayList<>();

}
