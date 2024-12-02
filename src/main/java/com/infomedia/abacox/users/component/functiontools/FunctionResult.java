package com.infomedia.abacox.users.component.functiontools;

import com.fasterxml.jackson.databind.JsonNode;
import com.infomedia.abacox.users.config.JsonConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FunctionResult {
    private boolean success;
    private String exception;
    private String message;
    private JsonNode result;

    public <T> T getResult(Class<T> clazz) {
        return JsonConfig.getObjectMapper().convertValue(result, clazz);
    }
}
