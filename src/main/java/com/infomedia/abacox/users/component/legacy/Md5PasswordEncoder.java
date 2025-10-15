package com.infomedia.abacox.users.component.legacy;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A Spring Security PasswordEncoder for validating legacy MD5 hashes.
 * This implementation is designed for migration purposes ONLY.
 * The encode() method is not supported and will throw an exception to prevent
 * the creation of new MD5 hashes.
 */
public class Md5PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        // We must never create new MD5 hashes. This encoder is for matching/migration only.
        throw new UnsupportedOperationException("MD5 encoding is not supported for security reasons.");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String md5OfRawPassword = hash(rawPassword.toString());
        // Case-insensitive comparison is safer for hashes
        return encodedPassword.equalsIgnoreCase(md5OfRawPassword);
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            // Convert to 16-base hexadecimal representation
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            // Pad with leading zeros to ensure 32 characters
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}