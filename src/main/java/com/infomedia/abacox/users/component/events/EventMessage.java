package com.infomedia.abacox.users.component.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventMessage extends WSMessage{
    private EventType eventType;
    private String content;

    public EventMessage(String source, EventType eventType, String content) {
        super(UUID.randomUUID(), source, LocalDateTime.now(), MessageType.EVENT);
        this.eventType = eventType;
        this.content = content;
    }
}
