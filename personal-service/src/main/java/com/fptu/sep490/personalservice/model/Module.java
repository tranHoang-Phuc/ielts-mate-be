    package com.fptu.sep490.personalservice.model;

    import jakarta.persistence.*;
    import lombok.*;
    import org.hibernate.annotations.CreationTimestamp;
    import org.hibernate.annotations.GenericGenerator;
    import org.hibernate.annotations.UpdateTimestamp;

    import java.time.LocalDateTime;
    import java.util.*;

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

        @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<FlashCardModule> flashCardModules = new ArrayList<>();

        @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private Set<ModuleUsers> moduleUsers = new HashSet<>();

        @Column(name = "createdBy")
        private String createdBy;
        @CreationTimestamp
        private LocalDateTime createdAt;
        @Column(name = "updatedBy")
        private String updatedBy;
        @UpdateTimestamp
        private LocalDateTime updatedAt;


        public List<FlashCard> getFlashCards() {
            return flashCardModules.stream()
                    .map(FlashCardModule::getFlashCard)
                    .toList();
        }

    }
