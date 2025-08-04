package com.fptu.sep490.personalservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import com.fptu.sep490.personalservice.model.Module;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ModuleRepository extends CrudRepository<Module, UUID> {



    @Query("""
    SELECT m FROM Module m
    JOIN ModuleUsers mu ON mu.module = m
    WHERE m.isDeleted = false
      AND mu.userId = :userId
      AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(m.moduleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<Module> searchModuleByUser(@Param("keyword") String keyword,
                                    Pageable pageable,
                                    @Param("userId") String userId);

    @Query("""
    SELECT m FROM Module m
    WHERE m.isDeleted = false
      AND (
        m.createdBy = :userId
        OR m.isPublic = true
      )
      AND (
        :keyword IS NULL OR :keyword = '' OR
        LOWER(m.moduleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<Module> searchMyAndPublicModules(@Param("keyword") String keyword,
                                          Pageable pageable,
                                          @Param("userId") String userId);


}
