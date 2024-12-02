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

    @Transactional
    public User create(CreateUser cDto) {
        return save(buildFromDto(cDto));
    }

    @Transactional
    public User update(UUID id, UpdateUser uDto) {
        User user = buildFromDto(get(id), uDto);
        if(user.getUsername().equals("system")){
            throw new IllegalArgumentException("System user cannot be updated");
        }
        return save(user);
    }

    @Override
    public User changeActivation(UUID id, boolean active) {
        if(get(id).getUsername().equals("system")){
            throw new IllegalArgumentException("System user cannot be deactivated");
        }
        return super.changeActivation(id, active);
    }

    public Optional<User> findByUsername(String username) {
        return getRepository().findByUsername(username);
    }

    @Transactional
    public void initDefaultSystemUser() {
        String username = "system";
        if (!getRepository().existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .email("system@abacox.com")
                    .firstName("System")
                    .lastName("System")
                    .role(roleService.getDefaultRoleAdmin())
                    .build();
            save(user);
        }
    }

    @Transactional
    public void initDefaultAdminUser() {
        String username = "admin";
        if (!getRepository().existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("@Abcd1234"))
                    .email("admin@abacox.com")
                    .firstName("Admin")
                    .lastName("Admin")
                    .role(roleService.getDefaultRoleAdmin())
                    .build();
            save(user);
        }
    }
}
