package com.infomedia.abacox.users.component.events;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder(toBuilder = true)
public class EventMessage extends WSMessage{
    private UUID id;
    private String source;
    private Instant timestamp;
    private EventType eventType;
    private String content;

    public EventMessage(String source, EventType eventType, String content) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.content = content;
        this.source = source;
    }
}
