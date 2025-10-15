package com.infomedia.abacox.users.service;

import com.infomedia.abacox.users.component.export.excel.GenericExcelGenerator;
import com.infomedia.abacox.users.dto.role.CreateRole;
import com.infomedia.abacox.users.dto.role.UpdateRole;
import com.infomedia.abacox.users.entity.Role;
import com.infomedia.abacox.users.exception.ResourceNotFoundException;
import com.infomedia.abacox.users.repository.RoleRepository;
import com.infomedia.abacox.users.service.common.CrudService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleService extends CrudService<Role, UUID, RoleRepository> {

    public static final List<String> INMUTABLE_ROLES = List.of("system", "admin", "user");

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
        Role role = buildFromDto(get(id), uDto);
        if(INMUTABLE_ROLES.contains(role.getRolename())) {
            throw new IllegalArgumentException("The role "+role.getRolename()+" cannot be modified");
        }
        return save(buildFromDto(get(id), uDto));
    }

    @Override
    public Role changeActivation(UUID id, boolean active) {
        if(INMUTABLE_ROLES.contains(get(id).getRolename())) {
            throw new IllegalArgumentException("The role "+get(id).getRolename()+" cannot be activated/deactivated");
        }
        return super.changeActivation(id, active);
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

    @Transactional
    public void initDefaultSystemRole() {
        String rolename = "system";
        if (!getRepository().existsByRolename(rolename)) {
            Role role = Role.builder()
                    .name("System")
                    .rolename(rolename)
                    .build();
            save(role);
        }
    }

    @Transactional
    public void initDefaultUserRole() {
        String rolename = "user";
        if (!getRepository().existsByRolename(rolename)) {
            Role role = Role.builder()
                    .name("User")
                    .rolename(rolename)
                    .build();
            save(role);
        }
    }

    @Transactional
    public void initDefaultRoles() {
        initDefaultSystemRole();
        initDefaultAdminRole();
        initDefaultUserRole();
    }

    public Role getDefaultRoleSystem() {
        return getRepository().findByRolename("system")
                .orElseThrow(() -> new ResourceNotFoundException(Role.class, "rolename "+ "system"));
    }

    public Role getDefaultRoleUser() {
        return getRepository().findByRolename("user")
                .orElseThrow(() -> new ResourceNotFoundException(Role.class, "rolename "+ "user"));
    }

    public Role getDefaultRoleAdmin() {
        return getRepository().findByRolename("admin")
                .orElseThrow(() -> new ResourceNotFoundException(Role.class, "rolename "+ "admin"));
    }

    public ByteArrayResource exportExcel(Specification<Role> specification, Pageable pageable, Map<String, String> alternativeHeaders, Set<String> excludeColumns) {
        Page<Role> collection = find(specification, pageable);
        try {
            InputStream inputStream = GenericExcelGenerator.generateExcelInputStream(collection.toList(), alternativeHeaders, excludeColumns);
            return new ByteArrayResource(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
