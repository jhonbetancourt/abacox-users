package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.remotefunction.BeanScanner;
import com.infomedia.abacox.users.component.remotefunction.DynamicFunctionCaller;
import com.infomedia.abacox.users.component.remotefunction.FunctionResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Log4j2
public class RemoteFunctionService {
    private final BeanScanner beanScanner;
    private Map<String, Object> serviceInstances;

    public RemoteFunctionService(BeanScanner beanScanner) {
        this.beanScanner = beanScanner;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void init() {
        serviceInstances = beanScanner.scanAndGetBeans(getClass().getPackageName());
        serviceInstances.remove("remoteFunctionService");
        log.info("Loaded service instances: {}", serviceInstances.keySet());
    }

    public FunctionResult callFunction(String serviceName, String functionName, Map<String, Object> args) {
        try {
            Object serviceInstance = serviceInstances.get(serviceName);
            if (serviceInstance == null) {
                Exception e = new IllegalArgumentException("Service not found: " + serviceName);
                return FunctionResult.builder()
                        .success(false)
                        .exception(e.getClass().getName())
                        .message(e.getMessage())
                        .build();
            }
            Object result = DynamicFunctionCaller.callFunction(serviceInstance, functionName, args);
            return FunctionResult.builder()
                    .success(true)
                    .result(result)
                    .build();
        } catch (Throwable e) {
            log.error("Error calling function {}:{}", serviceName, functionName, e);
            return FunctionResult.builder()
                    .success(false)
                    .exception(e.getClass().getName())
                    .message(e.getMessage())
                    .build();
        }
    }

    public Map<String, Object> getAvailableServices() {
        return serviceInstances;
    }
}