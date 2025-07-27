package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.FlashCard;
import com.fptu.sep490.personalservice.model.FlashCardModule;
import com.fptu.sep490.personalservice.model.Module;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FlashCardModuleRepository extends CrudRepository<FlashCardModule, UUID> {

    @Query("SELECT fcm FROM FlashCardModule fcm WHERE fcm.module.id = :moduleId")
    List<FlashCardModule> findByModuleId(@Param("moduleId") UUID moduleId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM flash_card_module WHERE module_id = :moduleId", nativeQuery = true)
    void deleteAllFlashCardsInModule(@Param("moduleId") UUID moduleId);


    boolean existsByFlashCardAndModule(FlashCard flashCard, Module module);

}
