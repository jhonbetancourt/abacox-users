package com.infomedia.abacox.users.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class TokenRefreshDto {
    @NotBlank
    @Schema(description = "token de refresco", example = "eyJhbG...")
    private String refreshToken;
}