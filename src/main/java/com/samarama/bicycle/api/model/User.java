package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;



import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
@PrimaryKeyJoinColumn(name = "id")
public class User extends IncompleteUser {

    public enum UserRole {
        CLIENT, SERVICEMAN, ADMIN, MODERATOR
    }

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 120)
    private String password;

    private boolean verified = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User(Long id, String email, String phoneNumber, String firstName, String lastName,
                String password, Set<String> roles, boolean verified, LocalDateTime createdAt) {
        super(id, email, phoneNumber, roles);
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.verified = verified;
        this.createdAt = createdAt;
    }


}