// File: com/infomedia/abacox/users/service/MigrationService.java
package com.infomedia.abacox.users.service; // Use your actual package

import com.infomedia.abacox.users.component.migration.DataMigrationExecutor;
import com.infomedia.abacox.users.component.migration.MigrationParams;
import com.infomedia.abacox.users.component.migration.MigrationUtils;
import com.infomedia.abacox.users.component.migration.SourceDbConfig;
import com.infomedia.abacox.users.component.migration.TableMigrationConfig;
import com.infomedia.abacox.users.constants.PasswordEncodingAlgorithm;
import com.infomedia.abacox.users.dto.migration.MigrationStart;
import com.infomedia.abacox.users.dto.migration.MigrationStatus;
import com.infomedia.abacox.users.exception.MigrationAlreadyInProgressException;
import com.infomedia.abacox.users.entity.Role;
import com.infomedia.abacox.users.entity.User;
import com.infomedia.abacox.users.repository.RoleRepository;
import com.infomedia.abacox.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class MigrationService {

    private final DataMigrationExecutor dataMigrationExecutor;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExecutorService migrationExecutorService = Executors.newSingleThreadExecutor();

    // --- State for linking old Perfil IDs to new Role entities ---
    private final Map<Long, Role> oldPerfilIdToNewRoleMap = new ConcurrentHashMap<>();
    private String defaultEncodedPassword;

    // --- State Tracking Fields ---
    private final AtomicBoolean isMigrationRunning = new AtomicBoolean(false);
    private final AtomicReference<MigrationState> currentState = new AtomicReference<>(MigrationState.IDLE);
    private final AtomicReference<LocalDateTime> startTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> endTime = new AtomicReference<>();
    private final AtomicReference<String> errorMessage = new AtomicReference<>();
    private final AtomicInteger totalTables = new AtomicInteger(0);
    private final AtomicInteger migratedTables = new AtomicInteger(0);
    private final AtomicReference<String> currentStep = new AtomicReference<>("");
    private final AtomicReference<Future<?>> migrationTaskFuture = new AtomicReference<>(null);

    // Helper record for returning structured name data
    private record ParsedName(String firstName, String lastName) {}

    // ... (All state management methods like startAsync, resetMigrationState, getStatus, reportProgress remain unchanged) ...
    public void startAsync(MigrationStart runRequest) {
        if (!isMigrationRunning.compareAndSet(false, true)) {
            throw new MigrationAlreadyInProgressException("A data migration is already in progress.");
        }
        log.info("Submitting migration task to executor service.");
        try {
            resetMigrationState();
            Future<?> future = migrationExecutorService.submit(() -> start(runRequest));
            migrationTaskFuture.set(future);
        } catch (Exception e) {
            log.error("Failed to submit migration task to executor service", e);
            currentState.set(MigrationState.FAILED);
            errorMessage.set("Failed to start migration task: " + e.getMessage());
            endTime.set(LocalDateTime.now());
            isMigrationRunning.set(false);
            migrationTaskFuture.set(null);
        }
    }

    private void resetMigrationState() {
        currentState.set(MigrationState.STARTING);
        startTime.set(LocalDateTime.now());
        endTime.set(null);
        errorMessage.set(null);
        migratedTables.set(0);
        totalTables.set(0);
        currentStep.set("Initializing...");
        migrationTaskFuture.set(null);
        log.debug("Migration state reset.");
    }

    public MigrationStatus getStatus() {
        return MigrationStatus.builder()
                .state(currentState.get())
                .startTime(startTime.get())
                .endTime(endTime.get())
                .errorMessage(errorMessage.get())
                .tablesToMigrate(totalTables.get())
                .tablesMigrated(migratedTables.get())
                .currentStep(currentStep.get())
                .build();
    }

    private void reportProgress(TableMigrationConfig config, Exception error) {
        int currentTotal = totalTables.get();
        if (error == null) {
            int completedCount = migratedTables.incrementAndGet();
            String stepMessage = String.format("Migrated table %d/%d: %s",
                    completedCount, currentTotal, config.getSourceTableName());
            currentStep.set(stepMessage);
            log.debug(stepMessage);
        } else {
            int completedCount = migratedTables.get();
            String stepMessage = String.format("Failed on table %d/%d: %s. Error: %s",
                    completedCount + 1,
                    currentTotal,
                    config.getSourceTableName(),
                    error.getMessage());
            currentStep.set(stepMessage);
            errorMessage.set(error.getMessage());
            log.warn("Progress update: Failure on table {}. Error: {}", config.getSourceTableName(), error.getMessage());
        }
    }

    public void start(MigrationStart runRequest) {
        if (!isMigrationRunning.get()) {
            isMigrationRunning.set(true);
            resetMigrationState();
        }
        log.info("<<<<<<<<<< Starting User & Role Data Migration Execution >>>>>>>>>>");
        currentState.set(MigrationState.RUNNING);
        this.oldPerfilIdToNewRoleMap.clear();

        try {
            String url = "jdbc:sqlserver://" + runRequest.getHost() + ":" + runRequest.getPort() + ";databaseName="
                    + runRequest.getDatabase() + ";encrypt=" + runRequest.getEncryption() + ";trustServerCertificate="
                    + runRequest.getTrustServerCertificate() + ";";

            SourceDbConfig sourceDbConfig = SourceDbConfig.builder()
                    .url(url)
                    .username(runRequest.getUsername())
                    .password(runRequest.getPassword())
                    .build();

            List<TableMigrationConfig> tablesToMigrate = defineUserAndRoleMigration();
            totalTables.set(tablesToMigrate.size());
            MigrationParams params = new MigrationParams(sourceDbConfig, tablesToMigrate);
            log.info("Constructed migration params with {} tables. Starting execution.", totalTables.get());
            currentStep.set(String.format("Starting migration of %d tables...", totalTables.get()));
            dataMigrationExecutor.runMigration(params, this::reportProgress);
            currentState.set(MigrationState.COMPLETED);
            currentStep.set(String.format("Finished: Successfully migrated %d/%d tables.", migratedTables.get(), totalTables.get()));
            log.info("<<<<<<<<<< Full Data Migration Finished Successfully >>>>>>>>>>");

        } catch (Exception e) {
            log.error("<<<<<<<<<< Full Data Migration FAILED during execution >>>>>>>>>>", e);
            currentState.set(MigrationState.FAILED);
            String failureMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            errorMessage.set("Migration failed: " + failureMsg);
            if (currentStep.get() != null && !currentStep.get().toLowerCase().contains("failed")) {
                currentStep.set(String.format("Failed during migration (%d/%d tables completed). Error: %s",
                        migratedTables.get(), totalTables.get(), failureMsg));
            }
        } finally {
            endTime.set(LocalDateTime.now());
            isMigrationRunning.set(false);
            migrationTaskFuture.set(null);
            log.info("Migration process ended. State: {}, Duration: {}", currentState.get(),
                    startTime.get() != null && endTime.get() != null ?
                            java.time.Duration.between(startTime.get(), endTime.get()) : "N/A");
        }
    }

    private List<TableMigrationConfig> defineUserAndRoleMigration() {
        List<TableMigrationConfig> configs = new ArrayList<>();

        configs.add(TableMigrationConfig.builder()
                .sourceTableName("perfil")
                .sourceIdColumnName("PERFIL_ID")
                .columnsToFetch(Set.of("PERFIL_ID", "PERFIL_NOMBRE", "PERFIL_ACTIVO"))
                .fetchOnly(true)
                .rowProcessor(this::processAndMapPerfilRow)
                .postMigrationSuccessAction(this::saveAllMappedRoles)
                .build());

        configs.add(TableMigrationConfig.builder()
                .sourceTableName("usuario")
                .sourceIdColumnName("USUARIO_ID")
                .columnsToFetch(Set.of("USUARIO_NOMBRE", "USUARIO_LOGIN", "USUARIO_CORREO", "USUARIO_ACTIVO", "USUARIO_PERFIL_ID", "USUARIO_PASSWORD"))
                .fetchOnly(true)
                .rowProcessor(this::processUsuarioRow)
                .build());
        return configs;
    }

    public void processAndMapPerfilRow(Map<String, Object> sourceRow) {
        try {
            Long oldId = ((Number) sourceRow.get("PERFIL_ID")).longValue();
            String name = (String) sourceRow.get("PERFIL_NOMBRE");
            if (name == null || name.trim().isEmpty()) {
                log.warn("Skipping perfil row with null or empty name. Row: {}", sourceRow);
                return;
            }
            if (oldPerfilIdToNewRoleMap.containsKey(oldId)) {
                log.trace("Perfil ID {} already processed. Skipping.", oldId);
                return;
            }
            String rolename = sanitizeRolename(name);
            Boolean active = (Boolean) MigrationUtils.convertToFieldType(sourceRow.get("PERFIL_ACTIVO"), boolean.class, null);
            Role newRole = Role.builder()
                    .name(name.trim())
                    .rolename(rolename)
                    .build();
            newRole.setActive(active != null ? active : true);
            oldPerfilIdToNewRoleMap.put(oldId, newRole);
        } catch (Exception e) {
            log.error("Failed to process and map perfil row: {}. Error: {}", sourceRow, e.getMessage(), e);
        }
    }

    @Transactional
    public void saveAllMappedRoles() {
        if (oldPerfilIdToNewRoleMap.isEmpty()) {
            log.warn("No roles were mapped from the source. Nothing to save.");
            return;
        }
        log.info("Persisting {} mapped roles to the database...", oldPerfilIdToNewRoleMap.size());
        Collection<Role> rolesToSave = oldPerfilIdToNewRoleMap.values();
        List<Role> finalRolesToSave = rolesToSave.stream()
            .filter(role -> {
                boolean exists = roleRepository.findByRolename(role.getRolename()).isPresent();
                if (exists) {
                    log.warn("A role with rolename '{}' already exists in the database. It will be skipped.", role.getRolename());
                }
                return !exists;
            })
            .collect(Collectors.toList());
        if (!finalRolesToSave.isEmpty()) {
            roleRepository.saveAll(finalRolesToSave);
            log.info("Successfully saved {} new roles.", finalRolesToSave.size());
        } else {
            log.info("No new roles to save after filtering out existing ones.");
        }
    }

    @Transactional
    public void processUsuarioRow(Map<String, Object> sourceRow) {
        try {
            String originalUsername = (String) sourceRow.get("USUARIO_LOGIN");
            String email = (String) sourceRow.get("USUARIO_CORREO");
            String oldMd5Password = (String) sourceRow.get("USUARIO_PASSWORD"); // Get the password

            if (originalUsername == null || originalUsername.trim().isEmpty() || email == null || email.trim().isEmpty()) {
                log.warn("Skipping user with null/empty username or email. Row: {}", sourceRow);
                return;
            }

            String sanitizedUsername = sanitizeUsername(originalUsername);
            if (userRepository.existsByUsername(sanitizedUsername) || userRepository.existsByEmail(email)) {
                log.trace("User with sanitized username '{}' or email '{}' already exists. Skipping.", sanitizedUsername, email);
                return;
            }

            Long oldPerfilId = ((Number) sourceRow.get("USUARIO_PERFIL_ID")).longValue();
            Role role = oldPerfilIdToNewRoleMap.get(oldPerfilId);
            if (role == null) {
                log.warn("Skipping user '{}' because their role (old PERFIL_ID: {}) could not be found in the in-memory map.", sanitizedUsername, oldPerfilId);
                return;
            }

            ParsedName parsedName = parseFullName((String) sourceRow.get("USUARIO_NOMBRE"));
            Boolean active = (Boolean) MigrationUtils.convertToFieldType(sourceRow.get("USUARIO_ACTIVO"), boolean.class, null);

            User newUser = User.builder()
                    .username(sanitizedUsername)
                    .email(email)
                    .firstName(parsedName.firstName())
                    .lastName(parsedName.lastName())
                    .password(oldMd5Password) // <-- Store the raw MD5 hash
                    .passwordEncoder(PasswordEncodingAlgorithm.MD5) // <-- SET THE ENUM
                    .role(role)
                    .build();
            newUser.setActive(active != null ? active : true);

            userRepository.save(newUser);
            log.trace("Successfully migrated user: {}", sanitizedUsername);

        } catch (Exception e) {
            log.error("Failed to process usuario row: {}. Error: {}", sourceRow, e.getMessage(), e);
        }
    }

    private ParsedName parseFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new ParsedName("", "");
        }
        String[] parts = fullName.trim().split("\\s+");
        int numParts = parts.length;
        switch (numParts) {
            case 1: return new ParsedName(parts[0], "");
            case 2: return new ParsedName(parts[0], parts[1]);
            case 3: return new ParsedName(parts[0], String.join(" ", parts[1], parts[2]));
            case 4: return new ParsedName(String.join(" ", parts[0], parts[1]), String.join(" ", parts[2], parts[3]));
            default:
                int firstNameCount = (numParts + 1) / 2;
                String firstName = Arrays.stream(parts, 0, firstNameCount).collect(Collectors.joining(" "));
                String lastName = Arrays.stream(parts, firstNameCount, numParts).collect(Collectors.joining(" "));
                return new ParsedName(firstName, lastName);
        }
    }

    private String sanitizeRolename(String input) {
        if (input == null || input.trim().isEmpty()) return "default-role";
        String sanitized = input.toLowerCase().trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "role-" + UUID.randomUUID().toString().substring(0, 8);
        return sanitized;
    }

    /**
     * Sanitizes a string to conform to the username format: ^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*$
     * It allows '.', '_', and '-' as separators. A sequence of separators will be collapsed
     * into a single instance of the first separator in that sequence (e.g., "a.-_b" -> "a.b").
     *
     * @param input The original username string.
     * @return A sanitized, valid username.
     */
    private String sanitizeUsername(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "user" + UUID.randomUUID().toString().substring(0, 8);
        }

        String sanitized = input.trim()
                // 1. Keep only allowed characters: letters, numbers, and the three separators.
                .replaceAll("[^A-Za-z0-9._-]", "");

        // 2. Collapse sequences of separators into the first separator of the sequence.
        // This regex finds one or more separators ([._-]+) and replaces the whole sequence
        // with the first character of that sequence ($1).
        sanitized = sanitized.replaceAll("([._-])[._-]+", "$1");

        // 3. Remove leading/trailing separators.
        // This regex looks for a separator at the start (^) or end ($) of the string.
        sanitized = sanitized.replaceAll("^[._-]|[._-]$", "");

        // 4. Ensure it's not empty after sanitation.
        if (sanitized.isEmpty()) {
            // Fallback for cases like "--" which would become empty.
            return "user" + UUID.randomUUID().toString().substring(0, 8);
        }

        return sanitized;
    }

    public enum MigrationState {
        IDLE, STARTING, RUNNING, COMPLETED, FAILED
    }
}