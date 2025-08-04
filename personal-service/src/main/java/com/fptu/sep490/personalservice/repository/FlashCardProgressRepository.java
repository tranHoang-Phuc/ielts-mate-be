package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.FlashCardProgress;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FlashCardProgressRepository extends CrudRepository<FlashCardProgress, UUID> {


    @Query("SELECT fcp FROM FlashCardProgress fcp WHERE fcp.moduleUsers.id = :moduleUserId AND fcp.flashcardId = :flashCardId")
    Optional<FlashCardProgress> findByModuleUserIdAndFlashcardId(@Param("moduleUserId") String moduleUserId,
                                                                 @Param("flashCardId") String flashCardId);



}
