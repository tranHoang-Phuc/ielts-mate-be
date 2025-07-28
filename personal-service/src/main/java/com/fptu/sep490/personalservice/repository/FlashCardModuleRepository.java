package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.FlashCard;
import com.fptu.sep490.personalservice.model.FlashCardModule;
import com.fptu.sep490.personalservice.model.Module;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Search for flash card modules by keyword if not full and user ID
    @Query("""
        SELECT fcm FROM FlashCardModule fcm
        WHERE fcm.module.isDeleted = false
          AND fcm.module.createdBy = :userId
          AND (
            :keyword IS NULL OR :keyword = '' OR
            LOWER(fcm.module.moduleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(fcm.module.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Module> searchShareModules(String keyword, Pageable pageable, String userId);
}
