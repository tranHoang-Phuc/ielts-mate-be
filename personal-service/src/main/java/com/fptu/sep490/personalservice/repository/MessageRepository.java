package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.ChatGroup;
import com.fptu.sep490.personalservice.model.Message;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Long> {

    @Query(
            "SELECT m FROM Message m WHERE m.group = :groupId ORDER BY m.sentAt ASC"
    )
    List<Message> findByGroupIdOrderByCreatedAtAsc(
            @Param("groupId") ChatGroup groupId);
}
