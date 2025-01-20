package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.generic.UUIDBody;
import com.infomedia.abacox.users.dto.login.LoginDto;
import com.infomedia.abacox.users.entity.Login;
import com.infomedia.abacox.users.service.LoginService;
import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Tag(name = "Login", description = "Login controller")
@SecurityRequirements({
        @SecurityRequirement(name = "JWT_Token"),
        @SecurityRequirement(name = "Username")
})
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;
    private final ModelConverter modelConverter;

    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    private LoginDto get(@PathVariable("id") UUID id) {
        return modelConverter.map(loginService.get(id), LoginDto.class);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<LoginDto> find(@Parameter(hidden = true) @Filter Specification<Login> spec
            , @Parameter(hidden = true) Pageable pageable
            , @RequestParam(required = false) String filter, @RequestParam(required = false) Integer page
            , @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        return modelConverter.mapPage(loginService.find(spec, pageable), LoginDto.class);
    }

    @PostMapping(value = "invalidate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public LoginDto invalidate(@Valid @RequestBody UUIDBody body) {
        return modelConverter.map(loginService.registerLogout(body.getId()), LoginDto.class);
    }
}
