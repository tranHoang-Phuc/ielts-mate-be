package com.fptu.sep490.personalservice.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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

    @Column(name = "status")
    private Integer status = 0; // 0: pending, 1: allowed, 2: denied
}
