package com.infomedia.abacox.users.dto.user;

import com.infomedia.abacox.users.dto.role.RoleDto;
import com.infomedia.abacox.users.dto.superclass.ActivableDto;
import com.infomedia.abacox.users.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link User}
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto extends ActivableDto {
    private UUID id;
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private RoleDto role;
}