package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.jwt.InvalidJwtTokenException;
import com.infomedia.abacox.users.component.jwt.JwtManager;
import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.auth.JwtTokenInfoDto;
import com.infomedia.abacox.users.dto.auth.TokenRequestDto;
import com.infomedia.abacox.users.dto.auth.TokenResultDto;
import com.infomedia.abacox.users.dto.user.UserDto;
import com.infomedia.abacox.users.entity.User;
import com.infomedia.abacox.users.exception.ResourceDisabledException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtManager jwtManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ModelConverter modelConverter;

    public TokenResultDto token(TokenRequestDto tokenRequestDto){
        if(tokenRequestDto.getUsername().equals("system")){
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = findActiveUser(tokenRequestDto.getUsername()).orElse(null);

        if(user!=null&&passwordEncoder.matches(tokenRequestDto.getPassword(), user.getPassword())){

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("roleId", user.getRole().getId());
            claims.put("rolename", user.getRole().getName());
            JwtManager.TokenInfo accessTokenInfo = jwtManager.generateAccessToken(claims);
            JwtManager.TokenInfo refreshTokenInfo = jwtManager.generateRefreshToken(claims);
            JwtManager.TokenInfo downloadTokenInfo = jwtManager.generateDownloadToken(claims);


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
        String username = claims.get("username", String.class);
        User user = findActiveUser(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));

        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("userId", user.getId());
        newClaims.put("username", user.getUsername());
        newClaims.put("roleId", user.getRole().getId());
        newClaims.put("rolename", user.getRole().getName());
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
        return findActiveUser(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));
    }

    public User validateDownloadToken(String token){
        Claims claims = jwtManager.validateDownloadToken(token);
        String username = claims.get("username", String.class);
        return findActiveUser(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));
    }

    public String getUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication==null?"anonymousUser":authentication.getName();
    }
}
