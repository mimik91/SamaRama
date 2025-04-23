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
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 120)
    private String password;

    private boolean verified = false;
}