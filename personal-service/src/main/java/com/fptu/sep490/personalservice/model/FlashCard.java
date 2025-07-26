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

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "word_id", referencedColumnName = "word_id", nullable = false)
    private Vocabulary vocabulary;


    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "flashcard_module",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id")
    )
    private Set<Module> modules = new HashSet<>();

    @Column(name = "createdBy")
    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updatedBy")
    private String updatedBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
