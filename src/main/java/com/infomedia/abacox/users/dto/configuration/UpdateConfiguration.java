package com.infomedia.abacox.users.dto.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateConfiguration {
    @NotNull
    @Schema(description = "whether to allow only one session per user", example = "false")
    private JsonNullable<Boolean> singleSession = JsonNullable.undefined();

    @NotNull
    @Schema(description = "maximum session age in seconds", example = "43200")
    private JsonNullable<Integer> sessionMaxAge = JsonNullable.undefined();
}