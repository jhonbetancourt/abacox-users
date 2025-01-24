package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.export.GenericExcelGenerator;
import com.infomedia.abacox.users.dto.user.CreateUser;
import com.infomedia.abacox.users.dto.user.UpdateUser;
import com.infomedia.abacox.users.dto.user.UserContactInfoDto;
import com.infomedia.abacox.users.entity.User;
import com.infomedia.abacox.users.repository.UserRepository;
import com.infomedia.abacox.users.service.common.CrudService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

    @Transactional
    public List<UserContactInfoDto> getContactInfoByRole(List<String> rolenames) {
        return getRepository().findByRole_RolenameInAndActive(rolenames, true).stream()
                .map(user -> UserContactInfoDto.builder()
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .rolename(user.getRole().getRolename())
                        .build())
                .toList();
    }

    @Transactional
    public List<UserContactInfoDto> getContactInfoByUsername(List<String> usernames) {
        return getRepository().findByUsernameInAndActive(usernames, true).stream()
                .map(user -> UserContactInfoDto.builder()
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .rolename(user.getRole().getRolename())
                        .build())
                .toList();
    }

    public ByteArrayResource exportExcel(Specification<User> specification, Pageable pageable, List<String> alternativeHeaders) {
        Page<User> collection = find(specification, pageable);
        try {
            InputStream inputStream = GenericExcelGenerator.generateExcelInputStream(collection.toList(), Set.of("password"), alternativeHeaders);
            return new ByteArrayResource(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
