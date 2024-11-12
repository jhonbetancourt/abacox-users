package com.infomedia.abacox.users.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TokenRequestDto {
    @NotBlank
    @Schema(description = "Nombre de usuario", example = "admin")
    private String username;
    @NotBlank
    @Schema(description = "Contraseña", example = "@Abcd1234")
    private String password;
}
