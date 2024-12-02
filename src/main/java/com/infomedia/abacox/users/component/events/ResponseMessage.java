package com.infomedia.abacox.users.component.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.infomedia.abacox.users.config.JsonConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder(toBuilder = true)
public class ResponseMessage extends WSMessage {
    private UUID id;
    private String source;
    private Instant timestamp;
    private boolean success;
    private String exception;
    private String errorMessage;
    private JsonNode result;

    public <T> T getResult(Class<T> clazz) {
        return JsonConfig.getObjectMapper().convertValue(result, clazz);
    }
}
