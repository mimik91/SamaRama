package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminInitializerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:dominiklach@poczta.fm}")
    private String adminEmail;

    @Value("${admin.password:misiek}")
    private String adminPassword;

    @Value("${admin.firstName:Admin}")
    private String adminFirstName;

    @Value("${admin.lastName:Admin}")
    private String adminLastName;

    @Value("${moderator.email:moderator@example.com}")
    private String moderatorEmail;

    @Value("${moderator.password:misiek}")
    private String moderatorPassword;

    @Value("${moderator.firstName:Moderator}")
    private String moderatorFirstName;

    @Value("${moderator.lastName:System}")
    private String moderatorLastName;

    public AdminInitializerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void initializeAdminAndModerator() {
        // Initialize Admin
        initializeAdmin();

        // Initialize Moderator
        initializeModerator();
    }

    private void initializeAdmin() {
        Optional<User> existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin.isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setFirstName(adminFirstName);
            admin.setLastName(adminLastName);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setVerified(true);
            admin.setCreatedAt(LocalDateTime.now());

            // Add both CLIENT and ADMIN roles
            admin.addRole("ROLE_CLIENT");
            admin.addRole("ROLE_ADMIN");

            userRepository.save(admin);

            System.out.println("Administrator account created: " + adminEmail);
        } else {
            System.out.println("Administrator account already exists: " + adminEmail);

            // Ensure the existing user has the ADMIN role
            User admin = existingAdmin.get();
            if (!admin.hasRole("ROLE_ADMIN")) {
                admin.addRole("ROLE_ADMIN");
                userRepository.save(admin);
                System.out.println("Added ADMIN role to existing user: " + adminEmail);
            }
        }
    }

    private void initializeModerator() {
        Optional<User> existingModerator = userRepository.findByEmail(moderatorEmail);

        if (existingModerator.isEmpty()) {
            User moderator = new User();
            moderator.setEmail(moderatorEmail);
            moderator.setFirstName(moderatorFirstName);
            moderator.setLastName(moderatorLastName);
            moderator.setPassword(passwordEncoder.encode(moderatorPassword));
            moderator.setVerified(true);
            moderator.setCreatedAt(LocalDateTime.now());

            // Add both CLIENT and MODERATOR roles
            moderator.addRole("ROLE_CLIENT");
            moderator.addRole("ROLE_MODERATOR");

            userRepository.save(moderator);

            System.out.println("Moderator account created: " + moderatorEmail);
        } else {
            System.out.println("Moderator account already exists: " + moderatorEmail);

            // Ensure the existing user has the MODERATOR role
            User moderator = existingModerator.get();
            if (!moderator.hasRole("ROLE_MODERATOR")) {
                moderator.addRole("ROLE_MODERATOR");
                userRepository.save(moderator);
                System.out.println("Added MODERATOR role to existing user: " + moderatorEmail);
            }
        }
    }
}