package com.infomedia.abacox.users.dto.role;

import com.infomedia.abacox.users.constants.Regexp;
import com.infomedia.abacox.users.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for {@link Role}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRole {
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "Visible name", example = "Administrator")
    private String name;

    @NotBlank
    @Size(min = 3, max = 255)
    @Pattern(regexp = Regexp.ROLENAME, message = Regexp.MSG_ROLENAME)
    @Schema(description = "Internal name", example = "admin")
    private String rolename;
}