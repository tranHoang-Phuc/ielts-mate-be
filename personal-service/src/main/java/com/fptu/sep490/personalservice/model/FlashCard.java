package com.fptu.sep490.personalservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name ="flash_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashCard {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "card_id", updatable = false, nullable = false)
    private UUID cardId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "word_id", referencedColumnName = "word_id", nullable = false)
    private Vocabulary vocabulary;


    @OneToMany(mappedBy = "flashCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlashCardModule> flashCardModules = new ArrayList<>();

    @Column(name = "createdBy")
    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updatedBy")
    private String updatedBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


    public List<Module> getModules() {
        return flashCardModules.stream()
                .map(FlashCardModule::getModule)
                .toList();
    }

}
