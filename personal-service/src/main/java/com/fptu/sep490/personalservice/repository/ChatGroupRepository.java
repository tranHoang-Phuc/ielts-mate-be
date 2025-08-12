package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.ChatGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatGroupRepository  extends CrudRepository<ChatGroup, String> {


    @Query(value = "SELECT * FROM chat_group cg WHERE cg.name = :publicChat LIMIT 1", nativeQuery = true)
    Optional<ChatGroup> findByName(@Param("publicChat") String publicChat);


    // Additional query methods can be defined here if needed
}
