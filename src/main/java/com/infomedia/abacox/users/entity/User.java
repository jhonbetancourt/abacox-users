package com.infomedia.abacox.users.entity;

import com.infomedia.abacox.users.constants.PasswordEncodingAlgorithm;
import com.infomedia.abacox.users.entity.superclass.ActivableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "users")
public class User extends ActivableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // --- NEW FIELD ---
    /**
     * Specifies the encoding algorithm used for the password hash.
     * A null value implies the system's default, modern algorithm (BCRYPT).
     * This field is crucial for the on-demand password migration strategy.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "password_encoder", length = 20)
    private PasswordEncodingAlgorithm passwordEncoder;
}