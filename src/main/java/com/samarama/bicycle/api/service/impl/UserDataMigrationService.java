package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceUserDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Serwis odpowiedzialny za migrację danych z IncompleteUser do User
 * Obsługuje rejestrację użytkowników i przenoszenie ich danych (rowerów, zamówień)
 */
@Service
public class UserDataMigrationService {

    private static final Logger logger = Logger.getLogger(UserDataMigrationService.class.getName());

    private final UserRepository userRepository;
    private final ServiceUserRepository serviceUserRepository;
    private final IndividualUserRepository individualUserRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public UserDataMigrationService(
            UserRepository userRepository, ServiceUserRepository serviceUserRepository,
            IndividualUserRepository individualUserRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.serviceUserRepository = serviceUserRepository;
        this.individualUserRepository = individualUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sprawdza poprawność formatu email
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@(.+)$") &&
                email.length() <= 50;
    }


    @Transactional
    public ServiceUser registerServiceUserAndMigrateData(ServiceUserDto serviceUserDto) {
        String email = serviceUserDto.email().toLowerCase().trim();

        // 1. WALIDACJA - sprawdź czy użytkownik już nie istnieje
        validateServiceUserRegistration(email);

        // 2. SPRAWDŹ czy istnieje IncompleteUser z tym emailem
        Optional<IndividualUser> existingIncompleteUser = individualUserRepository.findByEmail(email);

        if (existingIncompleteUser.isPresent()) {
            // MIGRACJA - konwertuj IncompleteUser na ServiceUser
            return migrateFromIncompleteUserToServiceUser(existingIncompleteUser.get(), serviceUserDto);
        } else {
            // NOWY SERVICEUSER - utwórz od zera
            return createNewServiceUser(serviceUserDto);
        }
    }

    private void validateServiceUserRegistration(String email) {
        // Sprawdź czy nie ma już zwykłego użytkownika
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email jest już zajęty przez zarejestrowanego użytkownika");
        }

        // Sprawdź czy nie ma już ServiceUser
        if (serviceUserRepository.existsByEmail(email)) {
            throw new RuntimeException("Email jest już zajęty przez innego użytkownika serwisu");
        }

        // Walidacja formatu email
        if (!isValidEmail(email)) {
            throw new RuntimeException("Nieprawidłowy format email");
        }
    }

    @Transactional
    private ServiceUser migrateFromIncompleteUserToServiceUser(User user, ServiceUserDto serviceUserDto) {
        logger.info("Migrating IncompleteUser to ServiceUser: " + user.getEmail());

        try {
            Long userId = user.getId();

            // 1. NATIVE SQL - wstaw do tabeli service_users
            String insertServiceUserSql = """
            INSERT INTO service_users (id, bike_service_id, password, verified) 
            VALUES (?1, ?2, ?3, ?4)
            """;

            entityManager.createNativeQuery(insertServiceUserSql)
                    .setParameter(1, userId)
                    .setParameter(2, serviceUserDto.bikeServiceId())
                    .setParameter(3, passwordEncoder.encode(serviceUserDto.password()))
                    .setParameter(4, false)
                    .executeUpdate();

            // 2. ZAKTUALIZUJ ROLE - ServiceUser może mieć inne role
            entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = ?1")
                    .setParameter(1, userId)
                    .executeUpdate();

            entityManager.createNativeQuery("INSERT INTO user_roles (user_id, role) VALUES (?1, 'ROLE_SERVICE')")
                    .setParameter(1, userId)
                    .executeUpdate();

            // 3. FLUSH I CLEAR CACHE
            entityManager.flush();
            entityManager.clear();

            // 4. POBIERZ JAKO SERVICEUSER
            ServiceUser migratedServiceUser = serviceUserRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve migrated service user"));

            logger.info("Successfully migrated to ServiceUser: " + migratedServiceUser.getEmail());

            return migratedServiceUser;

        } catch (Exception e) {
            logger.severe("Failed to migrate to ServiceUser " + user.getEmail() + ": " + e.getMessage());
            throw new RuntimeException("Błąd podczas migracji danych użytkownika serwisu: " + e.getMessage(), e);
        }
    }

    private ServiceUser createNewServiceUser(ServiceUserDto serviceUserDto) {
        logger.info("Creating new ServiceUser: " + serviceUserDto.email());

        ServiceUser serviceUser = createServiceUserFromDto(serviceUserDto);
        return serviceUserRepository.save(serviceUser);
    }

    /**
     * Tworzy obiekt ServiceUser z DTO
     */
    private ServiceUser createServiceUserFromDto(ServiceUserDto serviceUserDto) {
        ServiceUser serviceUser = new ServiceUser();

        // Podstawowe dane
        serviceUser.setEmail(serviceUserDto.email().toLowerCase().trim());
        serviceUser.setPassword(passwordEncoder.encode(serviceUserDto.password()));
        serviceUser.setBikeServiceId(serviceUserDto.bikeServiceId());

        // Status i role
        serviceUser.setVerified(false);
        serviceUser.setCreatedAt(LocalDateTime.now());

        // Ustaw role dla ServiceUser
        serviceUser.setRoles(Set.of("ROLE_SERVICE"));

        return serviceUser;
    }

    public User registerAndMigrateData(UserRegistrationDto registrationDto) {
        //To DO
        return null;
    }




}