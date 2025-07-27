package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.Module;
import com.fptu.sep490.personalservice.model.ModuleUsers;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ModuleUsersRepository extends CrudRepository<ModuleUsers, UUID> {

    @Query("SELECT mu FROM ModuleUsers mu WHERE mu.module.id = :moduleId AND mu.userId = :userId")
    Optional<ModuleUsers> findByModuleIdAndUserId(@Param("moduleId") UUID moduleId, @Param("userId") String userId);

    // Search for module_user by keyword of module in module_user if not full and user ID
    @Query("""
    SELECT mu FROM ModuleUsers mu
    WHERE mu.module.isDeleted = false
      AND mu.userId = :userId
      AND mu.status = :status
      AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(mu.module.moduleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(mu.module.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<ModuleUsers> searchShareModules(
            @Param("keyword") String keyword,
            Pageable pageable,
            @Param("userId") String userId,
            @Param("status") Integer status
    );


    @Query("""
    SELECT mu FROM ModuleUsers mu
    WHERE mu.module.isDeleted = false
      AND mu.createdBy = :userId
      AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(mu.module.moduleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(mu.module.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<ModuleUsers> searchMyShareModules(
            @Param("keyword") String keyword,
            Pageable pageable,
            @Param("userId") String userId
    );
}
