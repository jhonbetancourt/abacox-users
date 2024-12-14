package com.infomedia.abacox.users.service.remote;

import com.infomedia.abacox.users.component.events.EventsWebSocketServer;
import com.infomedia.abacox.users.dto.module.ModuleInfo;
import com.infomedia.abacox.users.exception.RemoteServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class ControlService {

    private final EventsWebSocketServer eventsWebSocketServer;

    public ModuleInfo getInfoByPrefix(String prefix) {
        try {
            return eventsWebSocketServer.sendCommandRequestAndAwaitResponse("getModuleInfoByPrefix"
                    , Map.of("prefix", prefix)).getResultAs(ModuleInfo.class);
        } catch (IOException | TimeoutException e) {
            throw new RemoteServiceException("Error getting module info by prefix", e);
        }
    }
}
