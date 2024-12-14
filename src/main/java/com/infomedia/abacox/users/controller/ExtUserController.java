package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.superclass.ActivationDto;
import com.infomedia.abacox.users.dto.user.CreateUser;
import com.infomedia.abacox.users.dto.user.UpdateUser;
import com.infomedia.abacox.users.dto.user.UserContactInfoDto;
import com.infomedia.abacox.users.dto.user.UserDto;
import com.infomedia.abacox.users.entity.User;
import com.infomedia.abacox.users.service.UserService;
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

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Tag(name = "ExtUser", description = "Ext User controller")
@SecurityRequirements({
        @SecurityRequirement(name = "JWT_Token"),
        @SecurityRequirement(name = "Username")
})
@RequestMapping("/api/ext/user")
public class ExtUserController {

    private final UserService userService;
    private final ModelConverter modelConverter;

    @GetMapping(value = "contactInfoByRoles", produces = MediaType.APPLICATION_JSON_VALUE)
    private List<UserContactInfoDto> getContactInfoByRoles(@RequestParam("roles") List<String> roles) {
        return userService.getContactInfoByRole(roles);
    }

    @GetMapping(value = "contactInfoByUsernames", produces = MediaType.APPLICATION_JSON_VALUE)
    private List<UserContactInfoDto> getContactInfoByUsernames(@RequestParam("usernames") List<String> usernames) {
        return userService.getContactInfoByUsername(usernames);
    }
}
