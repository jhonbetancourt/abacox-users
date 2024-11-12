package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.role.RoleDto;
import com.infomedia.abacox.users.dto.superclass.ActivationDto;
import com.infomedia.abacox.users.dto.user.CreateUser;
import com.infomedia.abacox.users.dto.user.UpdateUser;
import com.infomedia.abacox.users.dto.user.UserDto;
import com.infomedia.abacox.users.entity.User;
import com.infomedia.abacox.users.service.UserService;
import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "User", description = "User controller")
@SecurityRequirement(name = "JWT_Token")
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ModelConverter modelConverter;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto create(@Valid @RequestBody CreateUser createUser) {
        return modelConverter.map(userService.create(createUser), UserDto.class);
    }

    @PatchMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateUser updateUser) {
        return modelConverter.map(userService.update(id, updateUser), UserDto.class);
    }

    @PatchMapping(value = "/status/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDto activate(@PathVariable("id") UUID id, @Valid @RequestBody ActivationDto activationDto) {
        return modelConverter.map(userService.changeActivation(id, activationDto.getActive()), UserDto.class);
    }

    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    private UserDto get(@PathVariable("id") UUID id) {
        return modelConverter.map(userService.get(id), UserDto.class);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<UserDto> find(@Parameter(hidden = true) @Filter Specification<User> spec
            , @Parameter(hidden = true) Pageable pageable
            , @RequestParam(required = false) String filter, @RequestParam(required = false) Integer page
            , @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        return modelConverter.mapPage(userService.find(spec, pageable), UserDto.class);
    }
}