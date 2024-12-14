package com.infomedia.abacox.users.component.events;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CommandResult {
    private boolean success;
    private String exception;
    private String message;
    private JsonNode result;
}
