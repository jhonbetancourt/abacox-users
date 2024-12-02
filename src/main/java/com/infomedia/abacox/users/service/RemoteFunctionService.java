package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.events.EventsWebSocketServer;
import com.infomedia.abacox.users.component.functiontools.FunctionCall;
import com.infomedia.abacox.users.component.functiontools.FunctionResult;
import com.infomedia.abacox.users.dto.module.ModuleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RemoteFunctionService {

    private final AuthService authService;
    private final EventsWebSocketServer eventsWebSocketServer;
    private final ModuleService moduleService;


    public FunctionResult callFunction(String modulePrefix, String service, String function, Map<String, Object> arguments) {
        try {
            String thisModulePrefix = moduleService.getPrefix();
            ModuleInfo moduleInfo = eventsWebSocketServer.sendRequestMessageAndAwaitResponse(thisModulePrefix, "moduleService"
                    ,"getInfoByPrefix", Map.of("prefix", modulePrefix)).getResult(ModuleInfo.class);

            RestClient restClient = RestClient.builder()
                    .baseUrl(moduleInfo.getUrl())
                    .build();

            FunctionCall functionCall = FunctionCall.builder()
                    .service(service)
                    .function(function)
                    .arguments(arguments)
                    .build();

            String username = authService.getUsername();

            return restClient.post()
                    .uri("/module/function/call")
                    .header("X-Username", username)
                    .body(functionCall).retrieve().body(FunctionResult.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
