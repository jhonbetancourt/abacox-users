package com.infomedia.abacox.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitService {

    private final UserService userService;
    private final RoleService roleService;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        roleService.initDefaultAdminRole();
        userService.initDefaultAdminUser();
    }
}
