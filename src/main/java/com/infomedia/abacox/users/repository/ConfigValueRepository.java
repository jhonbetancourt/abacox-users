package com.infomedia.abacox.users.repository;

import com.infomedia.abacox.users.entity.ConfigValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ConfigValueRepository extends JpaRepository<ConfigValue, Long>, JpaSpecificationExecutor<ConfigValue> {


    Optional<ConfigValue> findByKey(String key);

    boolean existsByKey(String key);

    List<ConfigValue> findAllByKeyIn(List<String> keys);
}