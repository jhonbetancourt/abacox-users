package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.events.EventType;
import com.infomedia.abacox.users.config.SecurityConfig;
import com.infomedia.abacox.users.dto.module.EventTypesInfo;
import com.infomedia.abacox.users.dto.module.MEndpointInfo;
import com.infomedia.abacox.users.dto.module.ModuleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

@RequiredArgsConstructor
@Service
public class ModuleService {

    private final ApplicationContext applicationContext;
    private final SecurityConfig securityConfig;
    private static final String moduleName = "Users";
    private static final String moduleType = "USERS";
    private static final String moduleDescription = "Module for user management and authentication";
    private static final String moduleVersion = "1.0.0";
    private static final String modulePrefix = "users";
    private static final List<EventType> eventTypesProduces =
            List.of(EventType.TEST);
    private static final List<EventType> eventTypesConsumes =
            List.of(EventType.TEST);

    public ModuleInfo getInfo() {
        return ModuleInfo.builder()
                .name(moduleName)
                .type(moduleType)
                .description(moduleDescription)
                .version(moduleVersion)
                .prefix(modulePrefix)
                .build();
    }

    public EventTypesInfo getEventTypes() {
        return EventTypesInfo.builder()
                .produces(eventTypesProduces)
                .consumes(eventTypesConsumes)
                .build();
    }

    public List<MEndpointInfo> getEndpoints() {
        List<MEndpointInfo> mEndpointInfos = new ArrayList<>();
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        handlerMethods.forEach((key, value) -> {
            Set<String> patterns = key.getDirectPaths();
            if (patterns.isEmpty()) {
                // Fall back to the old method if patterns is empty
                patterns = key.getPatternsCondition() != null ? key.getPatternsCondition().getPatterns() : Set.of();
            }

            Set<RequestMethod> methods = key.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                // If no method is specified, assume it handles all methods
                methods = Set.of(RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH);
            }

            for (RequestMethod method : methods) {
                for (String pattern : patterns) {
                    mEndpointInfos.add(new MEndpointInfo(method.toString(), pattern, !securityConfig.isPublicPath(pattern)));
                }
            }
        });
        //add swagger path
        mEndpointInfos.add(new MEndpointInfo("GET", "/swagger-ui/**", false));
        mEndpointInfos.sort(Comparator.comparing(MEndpointInfo::getPath));
        return mEndpointInfos;
    }
}
