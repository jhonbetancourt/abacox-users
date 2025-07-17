package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.component.modeltools.ModelConverter;
import com.infomedia.abacox.users.dto.configuration.ConfigurationDto;
import com.infomedia.abacox.users.dto.configuration.UpdateConfiguration;
import com.infomedia.abacox.users.component.configmanager.ConfigService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@Tag(name = "Configuration", description = "Configuration controller")
@SecurityRequirements({
        @SecurityRequirement(name = "JWT_Token"),
        @SecurityRequirement(name = "Username")
})
@RequestMapping("/api/configuration")
public class ConfigController {

    private final ConfigService configService;
    private final ModelConverter modelConverter;

    @PatchMapping
    public ConfigurationDto updateConfiguration(@Valid @RequestBody UpdateConfiguration newConfig) {
        Map<String, Object> newConfigMap = modelConverter.toMap(newConfig);
        configService.updatePublicConfiguration(newConfigMap);
        return getConfiguration();
    }

    @GetMapping
    public ConfigurationDto getConfiguration() {
        Map<String, Object> configMap = configService.getPublicConfiguration();
        return modelConverter.fromMap(configMap, ConfigurationDto.class);
    }
}
