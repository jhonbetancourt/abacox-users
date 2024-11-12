package com.infomedia.abacox.users.dto.role;

import com.infomedia.abacox.users.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.UUID;

/**
 * DTO for {@link Role}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRole {
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "Visible name", example = "Administrator")
    private JsonNullable<String> name = JsonNullable.undefined();
}
