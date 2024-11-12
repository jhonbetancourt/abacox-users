package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.dto.role.CreateRole;
import com.infomedia.abacox.users.dto.role.UpdateRole;
import com.infomedia.abacox.users.entity.Role;
import com.infomedia.abacox.users.exception.ResourceNotFoundException;
import com.infomedia.abacox.users.repository.RoleRepository;
import com.infomedia.abacox.users.service.common.CrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RoleService extends CrudService<Role, UUID, RoleRepository> {


    public RoleService(RoleRepository repository) {
        super(repository);
    }

    public Role buildFromDto(CreateRole cDto) {
        return Role.builder()
                .name(cDto.getName())
                .rolename(cDto.getRolename())
                .build();
    }

    public Role buildFromDto(Role role, UpdateRole uDto) {
        uDto.getName().ifPresent(role::setName);
        return role;
    }

    public Role create(CreateRole cDto) {
        return save(buildFromDto(cDto));
    }

    public Role update(UUID id, UpdateRole uDto) {
        return save(buildFromDto(get(id), uDto));
    }


    @Transactional
    public void initDefaultAdminRole() {
        String rolename = "admin";
        if (!getRepository().existsByRolename(rolename)) {
            Role role = Role.builder()
                    .name("Administrator")
                    .rolename(rolename)
                    .build();
            save(role);
        }
    }

    public Role getDefaultRoleAdmin() {
        return getRepository().findByRolename("admin")
                .orElseThrow(() -> new ResourceNotFoundException(Role.class, "rolename "+ "admin"));
    }
}
