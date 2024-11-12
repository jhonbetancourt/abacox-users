package com.infomedia.abacox.users.dto.auth;

import com.infomedia.abacox.users.dto.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class TokenResultDto {
	@Schema(description = "usuario", implementation = UserDto.class)
	private UserDto user;
	@Schema(description = "token de acceso", implementation = JwtTokenInfoDto.class)
	private JwtTokenInfoDto accessToken;
	@Schema(description = "token de descarga", implementation = JwtTokenInfoDto.class)
	private JwtTokenInfoDto downloadToken;
	@Schema(description = "token de refresco", implementation = JwtTokenInfoDto.class)
	private JwtTokenInfoDto refreshToken;
}
