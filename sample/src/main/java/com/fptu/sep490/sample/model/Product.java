package com.fptu.sep490.sample.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("javaarchitecture:S7027")
public class Product extends AbstractAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String shortDescription;
}
