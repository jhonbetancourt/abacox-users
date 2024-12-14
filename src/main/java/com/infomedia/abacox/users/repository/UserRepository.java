package com.infomedia.abacox.users.repository;

import com.infomedia.abacox.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole_RolenameInAndActive(Collection<String> rolenames, boolean active);

    List<User> findByUsernameInAndActive(Collection<String> usernames, boolean active);


}