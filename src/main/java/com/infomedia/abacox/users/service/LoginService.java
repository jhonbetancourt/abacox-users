package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.entity.Login;
import com.infomedia.abacox.users.exception.ResourceNotFoundException;
import com.infomedia.abacox.users.repository.LoginRepository;
import com.infomedia.abacox.users.service.common.CrudService;
import jakarta.validation.ValidationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LoginService extends CrudService<Login, UUID, LoginRepository> {

    private final UserService userService;

    public LoginService(LoginRepository repository, UserService userService) {
        super(repository);
        this.userService = userService;
    }


    public Login registerLogin(UUID userId, String token, LocalDateTime loginDate, LocalDateTime expirationDate) {
        Login login = Login.builder()
                .user(userService.get(userId))
                .token(token)
                .loginDate(loginDate)
                .expirationDate(expirationDate)
                .build();

        return save(login);
    }

    public Login registerLogoutToken(String token) {
        Login login = getRepository().findByTokenAndLogoutDateIsNull(token)
                .orElseThrow(() -> new ResourceNotFoundException(Login.class));
        if(login.getLogoutDate() != null) {
            throw new ValidationException("Token already invalidated");
        }
        login.setLogoutDate(LocalDateTime.now());

        return save(login);
    }

    public Login registerLogout(UUID loginId) {
        Login login = get(loginId);
        if(login.getLogoutDate() != null) {
            throw new ValidationException("Login already invalidated");
        }
        login.setLogoutDate(LocalDateTime.now());
        return save(login);
    }

    @Transactional
    public List<Login> registerLogoutAll(UUID userId) {
        List<Login> logins = getRepository().findByUserAndLogoutDateIsNull(userService.get(userId));
        if(logins.isEmpty()) {
            throw new ResourceNotFoundException(Login.class);
        }
        LocalDateTime now = LocalDateTime.now();
        logins.forEach(login -> login.setLogoutDate(now));
        return saveAll(logins);
    }

    public boolean sessionIsValid(String token) {
        return getRepository().isTokenValidForUser(token, LocalDateTime.now());
    }

    public boolean sessionIsValid(UUID loginId) {
        return getRepository().isLoginValidForUser(loginId, LocalDateTime.now());
    }

    @Scheduled(fixedRate = 300000) // Check every five minutes
    public void checkForExpiredLogins() {
        LocalDateTime now = LocalDateTime.now();
        List<Login> expiredLogins = getRepository().findByExpirationDateBeforeAndLogoutDateIsNull(now);

        expiredLogins.forEach(login -> login.setLogoutDate(login.getExpirationDate()));

        saveAll(expiredLogins);
    }
}
