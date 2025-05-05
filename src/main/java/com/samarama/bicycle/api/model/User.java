package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    @Column(name = "first_name") // Nazwa kolumny
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name") // Nazwa kolumny
    private String lastName;

    @NotBlank
    @Size(max = 120)
    @Column(name = "password") // Nazwa kolumny
    private String password;

    @Column(name = "verified") // Nazwa kolumny
    private boolean verified = false;
}