package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.FlashCard;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

public interface FlashCardRepository extends CrudRepository<FlashCard, UUID> {

    @Query(
            """
            SELECT f FROM FlashCard f
            WHERE f.vocabulary.wordId = :vocabularyId
            AND f.createdBy = :userId
            
            """
    )
    Optional<FlashCard> findByVocabularyId( @Param("vocabularyId")UUID vocabularyId,  @Param("userId") String userId
    );

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM module_flash_card WHERE module_id = :moduleId", nativeQuery = true)
    void deleteAllFlashCardsInModule(@Param("moduleId") UUID moduleId);

}
