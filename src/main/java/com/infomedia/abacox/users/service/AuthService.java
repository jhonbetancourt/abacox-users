package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.jwt.InvalidJwtTokenException;
import com.infomedia.abacox.users.component.jwt.JwtManager;
import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.constants.ConfigKey;
import com.infomedia.abacox.users.dto.auth.JwtTokenInfoDto;
import com.infomedia.abacox.users.dto.auth.TokenRequestDto;
import com.infomedia.abacox.users.dto.auth.TokenResultDto;
import com.infomedia.abacox.users.dto.user.UserDto;
import com.infomedia.abacox.users.entity.Login;
import com.infomedia.abacox.users.entity.User;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtManager jwtManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ModelConverter modelConverter;
    private final LoginService loginService;
    private final ConfigurationService configurationService;
    private boolean singleSession = false;

    @PostConstruct
    public void init(){
        singleSession = configurationService.getAsBoolean(ConfigKey.SINGLE_SESSION).orElse(false);
        jwtManager.setRefreshTokenDurationSec(configurationService.getAsLong(ConfigKey.SESSION_MAX_AGE).orElse(43200L));
        configurationService.registerUpdateCallback(ConfigKey.SESSION_MAX_AGE, new ConfigurationService.UpdateCallback() {
            @Override
            public <T> void onUpdate(T value) {
                jwtManager.setRefreshTokenDurationSec((Long) value);
            }
        });
        configurationService.registerUpdateCallback(ConfigKey.SINGLE_SESSION, new ConfigurationService.UpdateCallback() {
            @Override
            public <T> void onUpdate(T value) {
                singleSession = (Boolean) value;
            }
        });
    }

    @Transactional
    public TokenResultDto token(TokenRequestDto tokenRequestDto){
        if(tokenRequestDto.getUsername().equals("system")){
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = findActiveUser(tokenRequestDto.getUsername()).orElse(null);

        if(user!=null&&passwordEncoder.matches(tokenRequestDto.getPassword(), user.getPassword())){

            if(singleSession){
                loginService.registerLogoutAll(user.getId());
            }

            Map<String, Object> claims1 = new HashMap<>();
            Map<String, Object> claims2 = new HashMap<>();
            claims1.put("userId", user.getId());
            claims1.put("username", user.getUsername());
            claims1.put("roleId", user.getRole().getId());
            claims1.put("rolename", user.getRole().getName());
            claims2.put("userId", user.getId());
            claims2.put("username", user.getUsername());
            claims2.put("roleId", user.getRole().getId());
            claims2.put("rolename", user.getRole().getName());
            JwtManager.TokenInfo refreshTokenInfo = jwtManager.generateRefreshToken(claims1);


            Login login = loginService.registerLogin(user.getId(), refreshTokenInfo.getToken()
                    , refreshTokenInfo.getIssuedAt(), refreshTokenInfo.getExpiration());

            claims2.put("loginId", login.getId());

            JwtManager.TokenInfo downloadTokenInfo = jwtManager.generateDownloadToken(claims2);
            JwtManager.TokenInfo accessTokenInfo = jwtManager.generateAccessToken(claims2);

            return TokenResultDto.builder()
                    .user(modelConverter.map(user, UserDto.class))
                    .accessToken(JwtTokenInfoDto.builder()
                            .token(accessTokenInfo.getToken())
                            .expiresIn(accessTokenInfo.getDuration())
                            .build())
                    .downloadToken(JwtTokenInfoDto.builder()
                            .token(downloadTokenInfo.getToken())
                            .expiresIn(downloadTokenInfo.getDuration())
                            .build())
                    .refreshToken(JwtTokenInfoDto.builder()
                            .token(refreshTokenInfo.getToken())
                            .expiresIn(refreshTokenInfo.getDuration())
                            .build())
                    .build();
        }

        throw new BadCredentialsException("Invalid credentials");
    }

    public TokenResultDto refresh(String token) {
        Claims claims = jwtManager.validateRefreshToken(token);

        if(!loginService.sessionIsValid(token)){
            throw new InvalidJwtTokenException("Invalid token");
        }

        String username = claims.get("username", String.class);
        User user = findActiveUser(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));

        Login login = loginService.getByRefreshToken(token);

        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("userId", user.getId());
        newClaims.put("username", user.getUsername());
        newClaims.put("roleId", user.getRole().getId());
        newClaims.put("rolename", user.getRole().getName());
        newClaims.put("loginId", login.getId());
        JwtManager.TokenInfo accessTokenInfo = jwtManager.generateAccessToken(newClaims);
        JwtManager.TokenInfo downloadTokenInfo = jwtManager.generateDownloadToken(newClaims);

        // Get the expiration time of the original refresh token
        Date refreshTokenExpiration = claims.getExpiration();
        long currentTimeMillis = System.currentTimeMillis();
        long refreshTokenDurationSeconds = Math.max(0, (refreshTokenExpiration.getTime() - currentTimeMillis) / 1000);

        return TokenResultDto.builder()
                .user(modelConverter.map(user, UserDto.class))
                .accessToken(JwtTokenInfoDto.builder()
                        .token(accessTokenInfo.getToken())
                        .expiresIn(accessTokenInfo.getDuration())
                        .build())
                .downloadToken(JwtTokenInfoDto.builder()
                        .token(downloadTokenInfo.getToken())
                        .expiresIn(downloadTokenInfo.getDuration())
                        .build())
                .refreshToken(JwtTokenInfoDto.builder()
                        .token(token)  // Use the original refresh token
                        .expiresIn(refreshTokenDurationSeconds)  // Use the remaining duration in seconds
                        .build())
                .build();
    }

    public Login invalidate(String token){
        return loginService.registerLogoutToken(token);
    }

    private Optional<User> findActiveUser(String username){
        User user = userService.findByUsername(username).orElse(null);
        if(user==null||!user.isActive()){
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public User validateAccessToken(String token){
        Claims claims = jwtManager.validateAccessToken(token);
        String username = claims.get("username", String.class);
        User user = findActiveUser(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));
        UUID loginId = UUID.fromString(claims.get("loginId", String.class));
        if(!loginService.sessionIsValid(loginId)){
            throw new InvalidJwtTokenException("Invalid token");
        }
        return user;
    }

    public User validateDownloadToken(String token){
        Claims claims = jwtManager.validateDownloadToken(token);
        String username = claims.get("username", String.class);
        User user = findActiveUser(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));
        UUID loginId = UUID.fromString(claims.get("loginId", String.class));
        if(!loginService.sessionIsValid(loginId)){
            throw new InvalidJwtTokenException("Invalid token");
        }
        return user;
    }

    public String getUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication==null?"anonymousUser":authentication.getName();
    }
}
