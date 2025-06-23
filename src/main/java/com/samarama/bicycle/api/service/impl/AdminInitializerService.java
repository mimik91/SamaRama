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
import java.util.Set;

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


    public AdminInitializerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

    }

    @PostConstruct
    @Transactional
    public void initializeAdmin() {
        Optional<User> existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin.isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setFirstName(adminFirstName);
            admin.setLastName(adminLastName);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setVerified(true);
            admin.setCreatedAt(LocalDateTime.now());

            // Add ONLY the ADMIN role, not CLIENT
            admin.setRoles(Set.of("ROLE_ADMIN"));

            userRepository.save(admin);

            System.out.println("Administrator account created: " + adminEmail);
        } else {
            User admin = existingAdmin.get();

            // Replace all existing roles with just ADMIN role
            if (!admin.getRoles().equals(Set.of("ROLE_ADMIN"))) {
                admin.setRoles(Set.of("ROLE_ADMIN"));
                userRepository.save(admin);
                System.out.println("Updated roles for admin user: " + adminEmail);
            }
        }
    }
}