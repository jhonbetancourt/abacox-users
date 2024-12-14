package com.infomedia.abacox.users.repository;

import com.infomedia.abacox.users.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID>, JpaSpecificationExecutor<Role> {
    boolean existsByRolename(String rolename);

    Optional<Role> findByRolename(String rolename);

}