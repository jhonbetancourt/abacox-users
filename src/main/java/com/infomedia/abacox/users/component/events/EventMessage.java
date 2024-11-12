package com.infomedia.abacox.users.component.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EventMessage {
    private UUID id;
    private String source;
    private EventType type;
    private Instant timestamp;
    private String content;

    public EventMessage(String source, EventType type, String content) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.timestamp = Instant.now();
        this.content = content;
        this.source = source;
    }
}
