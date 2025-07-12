package com.fptu.sep490.personalservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name ="user_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private int configId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "config_name")
    private String configName;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}
