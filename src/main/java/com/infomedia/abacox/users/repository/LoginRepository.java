package com.infomedia.abacox.users.repository;

import com.infomedia.abacox.users.entity.Login;
import com.infomedia.abacox.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoginRepository extends JpaRepository<Login, Long>, JpaSpecificationExecutor<Login> {


    Optional<Login> findByTokenAndLogoutDateIsNull(String token);

    List<Login> findByUserAndLogoutDateIsNull(User user);

    @Query("""
        select (count(l) > 0) from Login l where l.token = ?1 and l.logoutDate is null and l.expirationDate > ?2""")
    boolean isTokenValidForUser(String token, LocalDateTime currentTime);

    @Query("""
        select (count(l) > 0) from Login l where l.id = ?1 and l.logoutDate is null and l.expirationDate > ?2""")
    boolean isLoginValidForUser(UUID id, LocalDateTime currentTime);

    List<Login> findByExpirationDateBeforeAndLogoutDateIsNull(LocalDateTime currentTime);

}