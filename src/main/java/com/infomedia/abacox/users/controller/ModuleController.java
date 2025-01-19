package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.dto.configuration.ConfigurationDto;
import com.infomedia.abacox.users.dto.configuration.UpdateConfiguration;
import com.infomedia.abacox.users.dto.module.EventTypesInfo;
import com.infomedia.abacox.users.dto.module.MEndpointInfo;
import com.infomedia.abacox.users.dto.module.ModuleInfo;
import com.infomedia.abacox.users.service.ConfigurationService;
import com.infomedia.abacox.users.service.ModuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Tag(name = "Module", description = "Module controller")
@RequestMapping("/api/module")
public class ModuleController {

    private final ModuleService moduleService;
    private final ConfigurationService configurationService;

    @GetMapping("/endpoints")
    public List<MEndpointInfo> getEndpoints() {
        return moduleService.getEndpoints();
    }

    @GetMapping("/info")
    public ModuleInfo getInfo() {
        return moduleService.getInfo();
    }

    @GetMapping("/eventTypes")
    public EventTypesInfo getEventTypes() {
        return moduleService.getEventTypes();
    }

    @PatchMapping("/configuration")
    public ConfigurationDto updateConfiguration(@Valid @RequestBody UpdateConfiguration uDto) {
        return configurationService.updateConfiguration(uDto);
    }

    @GetMapping("/configuration")
    public ConfigurationDto getConfiguration() {
        return configurationService.getConfiguration();
    }
}
