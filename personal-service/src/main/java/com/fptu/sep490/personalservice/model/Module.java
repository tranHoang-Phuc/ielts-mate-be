package com.fptu.sep490.personalservice.model;

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
@Table(name ="modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "module_id", updatable = false, nullable = false)
    private UUID moduleId;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Builder.Default
    @ManyToMany(mappedBy = "modules")
    private Set<FlashCard> flashCards = new HashSet<>();


    @Column(name = "createdBy")
    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updatedBy")
    private String updatedBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
