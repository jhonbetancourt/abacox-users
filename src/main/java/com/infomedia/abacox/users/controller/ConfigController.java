package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.dto.configuration.ConfigurationDto;
import com.infomedia.abacox.users.dto.configuration.UpdateConfiguration;
import com.infomedia.abacox.users.service.ConfigurationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Tag(name = "Configuration", description = "Configuration controller")
@SecurityRequirements({
        @SecurityRequirement(name = "JWT_Token"),
        @SecurityRequirement(name = "Username")
})
@RequestMapping("/api/configuration")
public class ConfigController {

    private final ConfigurationService configurationService;

    @PatchMapping
    public ConfigurationDto updateConfiguration(@Valid @RequestBody UpdateConfiguration uDto) {
        return configurationService.updateConfiguration(uDto);
    }

    @GetMapping
    public ConfigurationDto getConfiguration() {
        return configurationService.getConfiguration();
    }
}
