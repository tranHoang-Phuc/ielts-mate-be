package com.fptu.sep490.personalservice.model;

import com.fptu.sep490.personalservice.model.enumeration.RecurrenceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "reminder")
public class ReminderConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer configId;

    @Column(name = "email")
    String email;

    @Column(name = "account_id")
    UUID accountId;

    @Column(name ="message")
    String message;

    @Column(name ="reminder_date")
    List<LocalDate> reminderDate;

    @Column(name = "reminder_time")
    LocalTime reminderTime;

    @Column(name ="recurrence")
    @Enumerated(EnumType.ORDINAL)
    RecurrenceType recurrence;

    @Column(name ="time_zone")
    String timeZone;

    @Column(name ="enabled")
    boolean enabled;

    @Column(name ="created_at")
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name ="updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;
}
