package com.fptu.sep490.personalservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name ="task_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private int configId;

    @Column(name = "type")
    private String type;

    @Column(name = "value", length = 1000)
    private String value;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name = "status")
    private int status;
}
