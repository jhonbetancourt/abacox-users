package com.infomedia.abacox.users.component.events;

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
    private Object result;
}
