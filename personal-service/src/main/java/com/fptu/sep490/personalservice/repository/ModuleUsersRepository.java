package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.ModuleUsers;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ModuleUsersRepository extends CrudRepository<ModuleUsers, UUID> {

    @Query("SELECT mu FROM ModuleUsers mu WHERE mu.module.id = :moduleId AND mu.userId = :userId")
    Optional<ModuleUsers> findByModuleIdAndUserId(@Param("moduleId") UUID moduleId, @Param("userId") String userId);


}
