package com.infomedia.abacox.users.dto.login;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.infomedia.abacox.users.constants.DateTimePattern;
import com.infomedia.abacox.users.dto.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link com.infomedia.abacox.users.entity.Login}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {
    private UUID id;
    private UserDto user;
    private String token;
    @JsonFormat(pattern = DateTimePattern.DATE_TIME)
    private LocalDateTime loginDate;
    @JsonFormat(pattern = DateTimePattern.DATE_TIME)
    private LocalDateTime expirationDate;
    @JsonFormat(pattern = DateTimePattern.DATE_TIME)
    private LocalDateTime logoutDate;
}