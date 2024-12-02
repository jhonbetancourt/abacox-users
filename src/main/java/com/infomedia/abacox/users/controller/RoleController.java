package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.role.CreateRole;
import com.infomedia.abacox.users.dto.role.RoleDto;
import com.infomedia.abacox.users.dto.role.UpdateRole;
import com.infomedia.abacox.users.dto.superclass.ActivationDto;
import com.infomedia.abacox.users.dto.user.UpdateUser;
import com.infomedia.abacox.users.dto.user.UserDto;
import com.infomedia.abacox.users.entity.Role;
import com.infomedia.abacox.users.service.RoleService;
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
@Tag(name = "Role", description = "Role controller")
@SecurityRequirements({
        @SecurityRequirement(name = "JWT_Token"),
        @SecurityRequirement(name = "Username")
})
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;
    private final ModelConverter modelConverter;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RoleDto create(@Valid @RequestBody CreateRole createRole) {
        return modelConverter.map(roleService.create(createRole), RoleDto.class);
    }

    @PatchMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RoleDto update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateRole uDto) {
        return modelConverter.map(roleService.update(id, uDto), RoleDto.class);
    }

    @PatchMapping(value = "/status/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RoleDto activate(@PathVariable("id") UUID id, @Valid @RequestBody ActivationDto activationDto) {
        return modelConverter.map(roleService.changeActivation(id, activationDto.getActive()), RoleDto.class);
    }

    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    private RoleDto get(@PathVariable("id") UUID id) {
        return modelConverter.map(roleService.get(id), RoleDto.class);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<RoleDto> find(@Parameter(hidden = true) @Filter Specification<Role> spec
            , @Parameter(hidden = true) Pageable pageable
            , @RequestParam(required = false) String filter, @RequestParam(required = false) Integer page
            , @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        return modelConverter.mapPage(roleService.find(spec, pageable), RoleDto.class);
    }
}
