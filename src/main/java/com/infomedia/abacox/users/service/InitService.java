package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.events.EventsWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Log4j2
public class InitService {

    private final UserService userService;
    private final RoleService roleService;
    private final EventsWebSocketServer eventsWebSocketServer;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        roleService.initDefaultAdminRole();
        userService.initDefaultSystemUser();
        userService.initDefaultAdminUser();
        new Thread(() -> {
            try {
                Thread.sleep(30000);
                try {
                    log.info(eventsWebSocketServer.sendRequestMessageAndAwaitResponse("users", "moduleService"
                            ,"getInfoByPrefix", Map.of("prefix", "users")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }
}
