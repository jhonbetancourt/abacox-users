package com.infomedia.abacox.users.constants;

/**
 * Defines the password hashing algorithm used for a user's password.
 * This is used for migrating from legacy hashing algorithms to modern ones.
 */
public enum PasswordEncodingAlgorithm {
    /**
     * Modern, secure, and the default hashing algorithm.
     */
    BCRYPT,

    /**
     * Legacy, insecure hashing algorithm. Passwords with this encoding
     * should be upgraded to BCRYPT upon the user's next successful login.
     */
    MD5
}