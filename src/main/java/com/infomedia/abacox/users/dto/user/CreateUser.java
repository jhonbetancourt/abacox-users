package com.infomedia.abacox.users.dto.user;

import com.infomedia.abacox.users.constants.Regexp;
import com.infomedia.abacox.users.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link User}
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUser {
    @NotBlank
    @Size(min = 3, max = 255)
    @Pattern(regexp = Regexp.USERNAME, message = Regexp.MSG_USERNAME)
    @Schema(example = "username", description = "Username of the user")
    private String username;
    @NotBlank
    @Size(min = 8, max = 20)
    @Pattern(regexp = Regexp.PASSWORD, message = Regexp.MSG_PASSWORD)
    @Schema(example = "@P4ssw0rd", description = "Password of the user")
    private String password;
    @NotBlank
    @Pattern(regexp = Regexp.EMAIL, message = Regexp.MSG_EMAIL)
    @Schema(example = "example@email.com", description = "Email of the user")
    private String email;
    @Size(min = 10, max = 20)
    @Pattern(regexp = Regexp.PHONE, message = Regexp.MSG_PHONE)
    private String phone;
    @Schema(example = "John", description = "First name of the user")
    private String firstName;
    @Size(max = 50)
    @Schema(example = "Doe", description = "Last name of the user")
    private String lastName;
    @Schema(example = "1990-01-01", description = "Date of birth of the user")
    private LocalDate dateOfBirth;
    @NotNull
    @Schema(example = "00000000-0000-0000-0000-000000000000", description = "Role id of the user")
    private UUID roleId;
}