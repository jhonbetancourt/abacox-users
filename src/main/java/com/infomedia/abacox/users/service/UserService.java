package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.dto.user.CreateUser;
import com.infomedia.abacox.users.dto.user.UpdateUser;
import com.infomedia.abacox.users.entity.User;
import com.infomedia.abacox.users.repository.UserRepository;
import com.infomedia.abacox.users.service.common.CrudService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService extends CrudService<User, UUID, UserRepository> {
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository repository, RoleService roleService, PasswordEncoder passwordEncoder) {
        super(repository);
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    public User buildFromDto(CreateUser cDto) {
        return User.builder()
                .username(cDto.getUsername())
                .password(passwordEncoder.encode(cDto.getPassword()))
                .email(cDto.getEmail())
                .firstName(cDto.getFirstName())
                .lastName(cDto.getLastName())
                .dateOfBirth(cDto.getDateOfBirth())
                .role(roleService.getActive(cDto.getRoleId()))
                .build();
    }

    public User buildFromDto(User user, UpdateUser uDto) {
        uDto.getEmail().ifPresent(user::setEmail);
        uDto.getPhone().ifPresent(user::setPhone);
        uDto.getFirstName().ifPresent(user::setFirstName);
        uDto.getLastName().ifPresent(user::setLastName);
        uDto.getDateOfBirth().ifPresent(user::setDateOfBirth);
        uDto.getRoleId().ifPresent(roleId -> user.setRole(roleService.getActive(roleId)));
        return user;
    }

    public User create(CreateUser cDto) {
        return save(buildFromDto(cDto));
    }

    public User update(UUID id, UpdateUser uDto) {
        return save(buildFromDto(get(id), uDto));
    }

    public Optional<User> findByUsername(String username) {
        return getRepository().findByUsername(username);
    }

    @Transactional
    public void initDefaultAdminUser() {
        String username = "admin";
        if (!getRepository().existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("@Abcd1234"))
                    .email("admin@example.com")
                    .firstName("Admin")
                    .lastName("Admin")
                    .role(roleService.getDefaultRoleAdmin())
                    .build();
            save(user);
        }
    }
}
