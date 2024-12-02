package com.infomedia.abacox.users.component.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder(toBuilder = true)
public class WSMessage {
    private UUID id;
    private String source;
    private Instant timestamp;
    private MessageType messagetype;
}
