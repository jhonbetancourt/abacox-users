package com.infomedia.abacox.users.repository;

import com.infomedia.abacox.users.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ConfigurationRepository extends JpaRepository<Configuration, Long>, JpaSpecificationExecutor<Configuration> {


    Optional<Configuration> findByKey(String key);
}