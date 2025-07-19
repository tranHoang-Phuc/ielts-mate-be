package com.fptu.sep490.personalservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="vocabularies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "word_id", updatable = false, nullable = false)
    private UUID wordId;

    @Column(name = "word", unique = true)
    private String word;

    @Column(name ="context", columnDefinition = "TEXT")
    private String context;

    @Column(name = "meaning", columnDefinition = "TEXT")
    private String meaning;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "createdBy")
    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updatedBy")
    private String updatedBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
