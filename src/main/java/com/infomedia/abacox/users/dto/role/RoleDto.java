package com.infomedia.abacox.users.dto.role;

import com.infomedia.abacox.users.dto.superclass.ActivableDto;
import com.infomedia.abacox.users.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for {@link Role}
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto extends ActivableDto {
    private UUID id;
    private String name;
    private String rolename;
}