package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.ReminderConfig;
import com.fptu.sep490.personalservice.model.enumeration.RecurrenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReminderConfigRepository extends JpaRepository<ReminderConfig, Integer> {
    Object findByAccountId(UUID accountId);

    boolean existsByAccountId(UUID accountId);

    @Query("""
        select c.email from ReminderConfig c where c.recurrence =:recurrenceType and c.reminderTime = :time
            and c.reminderDate in :currentDay and c.enabled = true
    """)
    List<String> findOneTimeEmails(@Param("time") LocalTime time,
                                   @Param("recurrenceType") RecurrenceType recurrenceType,
                                   @Param("currentDay") LocalDate today);

    @Query(
            value = """
        SELECT rc.email FROM reminder rc WHERE (rc.recurrence = ?2 OR rc.recurrence=?3) AND rc.reminder_time = CAST(?1 AS time) AND rc.enabled = true;
      """,
            nativeQuery = true
    )
    List<String> findDailyEmail(
            @Param("time")              String time,
            @Param("recurrenceOrdinal") int recurrenceOrdinal,
            @Param("recurrenceOrdinal1") int recurrenceOrdinal1
    );
    @Query(
            value = """
    SELECT c.email
      FROM reminder c
     WHERE c.recurrence    = :recurrenceOrdinal
       AND c.reminder_time = :time
       AND c.enabled       = TRUE
       AND EXTRACT(
             DOW 
             FROM c.created_at
           ) = EXTRACT(
             DOW 
             FROM NOW()
           )
  """,
            nativeQuery = true
    )
    List<String> findWeeklyEmail(@Param("time")               LocalTime time,
                                 @Param("recurrenceOrdinal")  int recurrenceOrdinal);

    @Query(
            value = """
        SELECT c.email
      FROM reminder c
     WHERE c.recurrence    = :recurrenceOrdinal
       AND c.reminder_time = :time
       AND c.enabled       = TRUE
       AND EXTRACT(DAY FROM c.created_at) = EXTRACT(DAY FROM NOW())
  """,
            nativeQuery = true
    )
    List<String> findMonthlyEmail(
            @Param("time")               LocalTime time,
            @Param("recurrenceOrdinal")  int recurrenceOrdinal
    );

    @Query(
            value = """
        SELECT c.email
          FROM reminder c
         WHERE c.recurrence     = :recurrenceOrdinal
           AND c.reminder_time  = :time
           AND c.enabled        = TRUE
           AND EXTRACT(MONTH FROM c.created_at) = EXTRACT(MONTH FROM NOW())
           AND EXTRACT(DAY   FROM c.created_at) = EXTRACT(DAY   FROM NOW())
      """,
            nativeQuery = true
    )
    List<String> findYearlyEmail(
            @Param("time")              LocalTime time,
            @Param("recurrenceOrdinal") int recurrenceOrdinal
    );
}
