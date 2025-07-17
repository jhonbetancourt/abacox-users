package com.infomedia.abacox.users.component.configmanager;

import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

/**
 * Enum representing all configurable keys stored in the database.
 * Each key holds its default value to ensure the application can always run.
 */
@Getter
public enum ConfigKey {

    // PUBLIC KEYS
    SINGLE_SESSION("false", true),
    SESSION_MAX_AGE("86400", true), // Default is 1 day in seconds
    RECAPTCHA("false", true);

    private final String defaultValue;
    private final boolean isPublic;

    ConfigKey(String defaultValue, boolean isPublic) {
        this.defaultValue = defaultValue;
        this.isPublic = isPublic;
    }

    public static List<ConfigKey> getPublicKeys() {
        return Stream.of(values())
            .filter(ConfigKey::isPublic)
                .toList();
    }

    public static List<ConfigKey> getPrivateKeys() {
        return Stream.of(values())
            .filter(key -> !key.isPublic())
            .toList();
    }

    public static List<ConfigKey> getAllKeys() {
        return Stream.of(values())
            .toList();
    }

    /**
     * Converts the enum's name from UPPER_SNAKE_CASE to lowerCamelCase.
     * For example, PENDING_APPROVAL becomes pendingApproval.
     *
     * @return The lowerCamelCase representation of the enum name.
     */
    public String getKey() {
        // 1. Get the name and convert to lower case: "pending_approval"
        String lowerCaseName = this.name().toLowerCase();

        // 2. Split by underscore: ["pending", "approval"]
        String[] parts = lowerCaseName.split("_");

        // If there's only one part (e.g., "shipped"), return it directly.
        if (parts.length == 1) {
            return parts[0];
        }

        // 3. Build the camelCase string
        StringBuilder camelCaseString = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            // 4. Capitalize the first letter of subsequent parts
            camelCaseString.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }

        return camelCaseString.toString();
    }
}