package com.infomedia.abacox.users.component.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.jasypt.util.binary.AES256BinaryEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Log4j2
public class JwtManager {

    @Value("${auth.jwt.secret}")
    private String secretString;

    private SecretKey secret;

    @Value("${auth.jwt.encryption-key}")
    private String encryptionKey;

    @Setter
    @Value("${auth.jwt.access-token-duration}")
    private Long accessTokenDurationSec;

    @Setter
    @Value("${auth.jwt.download-token-duration}")
    private Long downloadTokenDuration;

    @Setter
    @Value("${auth.jwt.refresh-token-duration}")
    private Long refreshTokenDurationSec;
    private final AES256BinaryEncryptor encryptor = new AES256BinaryEncryptor();

    @Value("${abacox.client-name}")
    private String clientName;

    private static final String MSG_INVALID_TOKEN_TYPE = "Invalid token type";
    private static final String MSG_INVALID_TOKEN_FORMAT = "Invalid token format";
    private static final String MSG_INVALID_TOKEN = "Invalid token";
    private static final String DOWNLOAD_TOKEN_PREFIX = "DL_";

    @PostConstruct
    private void init() {
        secret = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        encryptor.setPassword(encryptionKey);
    }

    public TokenInfo generateAccessToken(Map<String, Object> claims) {
        if (claims == null) claims = new LinkedHashMap<>();
        LocalDateTime nowLdt = LocalDateTime.now();
        Date now = Timestamp.valueOf(nowLdt);
        Date expiration = Timestamp.valueOf(nowLdt.plusSeconds(accessTokenDurationSec));
        return new TokenInfo(Jwts.builder()
                .claims(claims).header()
                .add("typ", "JWT")
                .add("mod", Mode.ACCESS.name())
                .and()
                .claim("clt", clientName)
                .expiration(expiration)
                .notBefore(now)
                .issuedAt(now)
                .signWith(secret)
                .compact(), accessTokenDurationSec, nowLdt, nowLdt.plusSeconds(accessTokenDurationSec));
    }


    public TokenInfo generateDownloadToken(Map<String, Object> claims) {
        if (claims == null) claims = new LinkedHashMap<>();
        LocalDateTime nowLdt = LocalDateTime.now();
        Date now = Timestamp.valueOf(nowLdt);
        Date expiration = Timestamp.valueOf(nowLdt.plusSeconds(downloadTokenDuration));
        return new TokenInfo(DOWNLOAD_TOKEN_PREFIX+Base58.encode(encryptor.encrypt(Jwts.builder()
                .claims(claims).header()
                .add("typ", "JWT")
                .add("mod", Mode.DOWNLOAD.name())
                .add("su", true)
                .and()
                .claim("clt", clientName)
                .expiration(expiration)
                .notBefore(now)
                .issuedAt(now)
                .signWith(secret)
                .compressWith(Jwts.ZIP.DEF)
                .compact()
                .getBytes(StandardCharsets.UTF_8))), downloadTokenDuration, nowLdt, nowLdt.plusSeconds(downloadTokenDuration));
    }

    public TokenInfo generateRefreshToken(Map<String, Object> claims) {
        if (claims == null) claims = new LinkedHashMap<>();
        LocalDateTime nowLdt = LocalDateTime.now();
        Date now = Timestamp.valueOf(nowLdt);
        Date expiration = Timestamp.valueOf(nowLdt.plusSeconds(refreshTokenDurationSec));
        return new TokenInfo(Jwts.builder()
                .claims(claims).header()
                .add("typ", "JWT")
                .add("mod", Mode.REFRESH.name())
                .and()
                .claim("clt", clientName)
                .expiration(expiration)
                .notBefore(now)
                .issuedAt(now)
                .signWith(secret)
                .compact(), refreshTokenDurationSec, nowLdt, nowLdt.plusSeconds(refreshTokenDurationSec));
    }

    public Claims validateAccessToken(@NonNull String accessToken) {
        Jws<Claims> jwt = Jwts.parser().verifyWith(secret).build().parseSignedClaims(accessToken);
        String mode = (String) jwt.getHeader().get("mod");
        if (mode == null || !mode.equals(Mode.ACCESS.name())) {
            throw new InvalidJwtTokenException(MSG_INVALID_TOKEN_TYPE);
        }
        validateClient(jwt.getPayload());
        return jwt.getPayload();
    }

    private void validateClient(Claims claims){
        String tokenClient = claims.get("clt", String.class);
        if(tokenClient==null||!tokenClient.equals(clientName)){
            throw new InvalidJwtTokenException(MSG_INVALID_TOKEN);
        }
    }

    public Claims validateDownloadToken(@NonNull String downloadToken) {
        String tokenWithoutPrefix;
        if (downloadToken.startsWith(DOWNLOAD_TOKEN_PREFIX)) {
            tokenWithoutPrefix = downloadToken.substring(DOWNLOAD_TOKEN_PREFIX.length());
        } else {
            throw new InvalidJwtTokenException(MSG_INVALID_TOKEN_FORMAT);
        }
        String jwtToken;
        try {
            jwtToken = new String(encryptor.decrypt(Base58.decode(tokenWithoutPrefix)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new InvalidJwtTokenException(MSG_INVALID_TOKEN_FORMAT);
        }
        Jws<Claims> jwt = Jwts.parser().verifyWith(secret).build().parseSignedClaims(jwtToken);
        String mode = (String) jwt.getHeader().get("mod");
        if (mode == null || !mode.equals(Mode.DOWNLOAD.name())) {
            throw new InvalidJwtTokenException(MSG_INVALID_TOKEN_TYPE);
        }
        validateClient(jwt.getPayload());
        return jwt.getPayload();
    }

    public Claims validateRefreshToken(@NonNull String jwtToken) {
        Jws<Claims> jwt = Jwts.parser().verifyWith(secret).build().parseSignedClaims(jwtToken);
        String mode = (String) jwt.getHeader().get("mod");
        if (mode == null || !mode.equals(Mode.REFRESH.name())) {
            throw new InvalidJwtTokenException(MSG_INVALID_TOKEN_TYPE);
        }
        validateClient(jwt.getPayload());
        return jwt.getPayload();
    }

    public enum Mode {
        ACCESS, REFRESH, DOWNLOAD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInfo{
        private String token;
        private Long duration;
        private LocalDateTime issuedAt;
        private LocalDateTime expiration;
    }
}
