package com.infomedia.abacox.users.dto.user;

import com.infomedia.abacox.users.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for {@link User}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserContactInfoDto {
    private String username;
    private String email;
    private String phone;
    private String rolename;
}