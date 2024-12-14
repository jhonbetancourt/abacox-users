package com.infomedia.abacox.users.controller;

import com.infomedia.abacox.users.dto.module.EventTypesInfo;
import com.infomedia.abacox.users.dto.module.MEndpointInfo;
import com.infomedia.abacox.users.dto.module.ModuleInfo;
import com.infomedia.abacox.users.service.ModuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Tag(name = "Module", description = "Module controller")
@RequestMapping("/api/module")
public class ModuleController {

    private final ModuleService moduleService;

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
}
