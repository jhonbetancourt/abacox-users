package com.infomedia.abacox.users.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class TokenRefreshResultDto {
    @Schema(description = "token de acceso", implementation = JwtTokenInfoDto.class)
    private JwtTokenInfoDto accessToken;
    @Schema(description = "token de descarga", implementation = JwtTokenInfoDto.class)
    private JwtTokenInfoDto downloadToken;
}
