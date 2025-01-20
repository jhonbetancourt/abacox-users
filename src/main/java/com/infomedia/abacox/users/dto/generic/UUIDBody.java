package com.infomedia.abacox.users.dto.generic;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UUIDBody {
    @NotNull
    private UUID id;
}
