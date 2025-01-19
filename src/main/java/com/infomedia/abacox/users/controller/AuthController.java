package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.auth.TokenRequestDto;
import com.infomedia.abacox.users.dto.auth.TokenResultDto;
import com.infomedia.abacox.users.dto.auth.JwtTokenDto;
import com.infomedia.abacox.users.dto.user.UserDto;
import com.infomedia.abacox.users.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Tag(name = "Auth", description = "Auth controller")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ModelConverter modelConverter;


    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TokenResultDto token(@Valid @RequestBody TokenRequestDto tokenRequestDto){
        return authService.token(tokenRequestDto);
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TokenResultDto refresh(@Valid @RequestBody JwtTokenDto dto){
        return authService.refresh(dto.getToken());
    }

    @PostMapping(value = "/validateAccessToken", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto validateAccessToken(@Valid @RequestBody JwtTokenDto dto){
        return modelConverter.map(authService.validateAccessToken(dto.getToken()), UserDto.class);
    }

    @PostMapping(value = "/validateDownloadToken", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto validateDownloadToken(@Valid @RequestBody JwtTokenDto dto){
        return modelConverter.map(authService.validateDownloadToken(dto.getToken()), UserDto.class);
    }

    @PostMapping(value = "/invalidate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void invalidate(@Valid @RequestBody JwtTokenDto dto){
        authService.invalidate(dto.getToken());
    }

}
