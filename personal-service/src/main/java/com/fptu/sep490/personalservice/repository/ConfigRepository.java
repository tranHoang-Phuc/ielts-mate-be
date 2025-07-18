package com.fptu.sep490.personalservice.repository;

import com.fptu.sep490.personalservice.model.UserConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConfigRepository extends CrudRepository<UserConfig, Integer> {
    @Query(value = "select c.value from user_configs c where c.config_name = ?1 and c.account_id = ?2", nativeQuery = true)
    Optional<String> getConfigByKeyAndAccountId(String key, UUID accountId);

    UserConfig findByConfigNameAndAccountId(String targetConfig, UUID uuid);
}
