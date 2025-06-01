package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
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
    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final TransportOrderRepository transportOrderRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataMigrationService(
            UserRepository userRepository,
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            TransportOrderRepository transportOrderRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Rejestruje nowego użytkownika i migruje jego dane z IncompleteUser jeśli istnieją
     *
     * @param registrationDto dane rejestracyjne
     * @return zapisany użytkownik User
     */
    @Transactional
    public User registerAndMigrateData(UserRegistrationDto registrationDto) {
        String email = registrationDto.email().toLowerCase().trim();

        // 1. WALIDACJA - sprawdź czy użytkownik już nie istnieje
        validateUserRegistration(email);

        // 2. SPRAWDŹ czy istnieje IncompleteUser z tym emailem
        Optional<IncompleteUser> existingIncompleteUser = incompleteUserRepository.findByEmail(email);

        if (existingIncompleteUser.isPresent()) {
            // MIGRACJA - konwertuj IncompleteUser na User
            return migrateFromIncompleteUser(existingIncompleteUser.get(), registrationDto);
        } else {
            // NOWY UŻYTKOWNIK - utwórz od zera
            return createNewUser(registrationDto);
        }
    }

    /**
     * Waliduje dane rejestracyjne
     */
    private void validateUserRegistration(String email) {
        // Sprawdź czy użytkownik już nie istnieje
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email jest już zajęty przez zarejestrowanego użytkownika");
        }

        // Walidacja formatu email
        if (!isValidEmail(email)) {
            throw new RuntimeException("Nieprawidłowy format email");
        }
    }

    /**
     * Migruje dane z IncompleteUser na User
     */
    @Transactional
    private User migrateFromIncompleteUser(IncompleteUser incompleteUser, UserRegistrationDto registrationDto) {
        logger.info("Migrating IncompleteUser to User: " + incompleteUser.getEmail());

        try {
            // 1. UTWÓRZ NOWEGO USER z danymi z rejestracji
            User newUser = createUserFromRegistration(registrationDto);

            // 2. SKOPIUJ METADANE z IncompleteUser
            copyMetadataFromIncompleteUser(newUser, incompleteUser);

            // 3. ZAPISZ USER (dziedziczy ID od IncompleteUser)
            newUser.setId(incompleteUser.getId());
            User savedUser = userRepository.save(newUser);

            // 4. MIGRUJ ROWERY - zmień właściciela na nowego User
            migrateBicycles(incompleteUser, savedUser);

            // 5. MIGRUJ ZAMÓWIENIA - zmień klienta na nowego User
            migrateOrders(incompleteUser, savedUser);

            // 6. USUŃ STARY IncompleteUser (kaskadowo przez dziedziczenie)
            // Nie usuwamy ręcznie - User dziedziczy po IncompleteUser

            logger.info("Successfully migrated user: " + savedUser.getEmail() +
                    " with " + getBicycleCount(savedUser) + " bicycles and " +
                    getOrderCount(savedUser) + " orders");

            return savedUser;

        } catch (Exception e) {
            logger.severe("Failed to migrate user " + incompleteUser.getEmail() + ": " + e.getMessage());
            throw new RuntimeException("Błąd podczas migracji danych użytkownika: " + e.getMessage(), e);
        }
    }

    /**
     * Tworzy nowego użytkownika od zera
     */
    private User createNewUser(UserRegistrationDto registrationDto) {
        logger.info("Creating new user: " + registrationDto.email());

        User newUser = createUserFromRegistration(registrationDto);
        return userRepository.save(newUser);
    }

    /**
     * Tworzy obiekt User z danych rejestracyjnych
     */
    private User createUserFromRegistration(UserRegistrationDto registrationDto) {
        User user = new User();

        // Podstawowe dane
        user.setEmail(registrationDto.email().toLowerCase().trim());
        user.setFirstName(registrationDto.firstName());
        user.setLastName(registrationDto.lastName());
        user.setPassword(passwordEncoder.encode(registrationDto.password()));

        // Status i role
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        // POPRAWKA: Ustaw poprawne role dla klienta
        user.setRoles(Set.of("ROLE_CLIENT"));

        return user;
    }

    /**
     * Kopiuje metadane z IncompleteUser do User
     */
    private void copyMetadataFromIncompleteUser(User user, IncompleteUser incompleteUser) {
        // Skopiuj telefon jeśli istnieje
        if (incompleteUser.getPhoneNumber() != null) {
            user.setPhoneNumber(incompleteUser.getPhoneNumber());
        }

        // POPRAWKA: Zachowaj oryginalną datę utworzenia z IncompleteUser
        if (incompleteUser.getCreatedAt() != null) {
            user.setCreatedAt(incompleteUser.getCreatedAt());
        }

        // POPRAWKA: Nie kopiuj ról z IncompleteUser - użyj standardowych dla klienta
        // IncompleteUser może mieć inne role lub ich brak
        user.setRoles(Set.of("ROLE_CLIENT"));
    }

    /**
     * Migruje rowery z IncompleteUser na User
     */
    @Transactional
    private void migrateBicycles(IncompleteUser oldOwner, User newOwner) {
        List<IncompleteBike> bikes = incompleteBikeRepository.findByOwner(oldOwner);

        if (!bikes.isEmpty()) {
            logger.info("Migrating " + bikes.size() + " bicycles for user: " + newOwner.getEmail());

            // POPRAWKA: Zmień właściciela wszystkich rowerów
            for (IncompleteBike bike : bikes) {
                bike.setOwner(newOwner);
            }

            incompleteBikeRepository.saveAll(bikes);
            logger.info("Successfully migrated " + bikes.size() + " bicycles");
        }
    }

    /**
     * Migruje zamówienia z IncompleteUser na User
     */
    @Transactional
    private void migrateOrders(IncompleteUser oldClient, User newClient) {
        List<TransportOrder> orders = transportOrderRepository.findByClient(oldClient);

        if (!orders.isEmpty()) {
            logger.info("Migrating " + orders.size() + " orders for user: " + newClient.getEmail());

            // POPRAWKA: Zmień klienta wszystkich zamówień
            for (TransportOrder order : orders) {
                order.setClient(newClient);
                order.setLastModifiedBy("SYSTEM_MIGRATION");
                order.setLastModifiedDate(LocalDateTime.now());
            }

            transportOrderRepository.saveAll(orders);
            logger.info("Successfully migrated " + orders.size() + " orders");
        }
    }

    // === METODY POMOCNICZE ===

    /**
     * Sprawdza poprawność formatu email
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@(.+)$") &&
                email.length() <= 50;
    }

    /**
     * Zlicza rowery użytkownika
     */
    private int getBicycleCount(User user) {
        return incompleteBikeRepository.findByOwner(user).size();
    }

    /**
     * Zlicza zamówienia użytkownika
     */
    private int getOrderCount(User user) {
        return transportOrderRepository.findByClient(user).size();
    }

    // === METODY PUBLICZNE DO SPRAWDZANIA STANU ===

    /**
     * Sprawdza czy użytkownik ma dane do migracji
     */
    public boolean hasDataToMigrate(String email) {
        Optional<IncompleteUser> incompleteUser = incompleteUserRepository.findByEmail(email.toLowerCase().trim());

        if (incompleteUser.isEmpty()) {
            return false;
        }

        IncompleteUser user = incompleteUser.get();
        int bicycleCount = incompleteBikeRepository.findByOwner(user).size();
        int orderCount = transportOrderRepository.findByClient(user).size();

        return bicycleCount > 0 || orderCount > 0;
    }

    /**
     * Zwraca informacje o danych do migracji
     */
    public MigrationInfo getMigrationInfo(String email) {
        Optional<IncompleteUser> incompleteUser = incompleteUserRepository.findByEmail(email.toLowerCase().trim());

        if (incompleteUser.isEmpty()) {
            return new MigrationInfo(false, 0, 0, null);
        }

        IncompleteUser user = incompleteUser.get();
        int bicycleCount = incompleteBikeRepository.findByOwner(user).size();
        int orderCount = transportOrderRepository.findByClient(user).size();

        return new MigrationInfo(true, bicycleCount, orderCount, user.getCreatedAt());
    }

    /**
     * Klasa z informacjami o migracji
     */
    public static class MigrationInfo {
        private final boolean hasData;
        private final int bicycleCount;
        private final int orderCount;
        private final LocalDateTime userCreatedAt;

        public MigrationInfo(boolean hasData, int bicycleCount, int orderCount, LocalDateTime userCreatedAt) {
            this.hasData = hasData;
            this.bicycleCount = bicycleCount;
            this.orderCount = orderCount;
            this.userCreatedAt = userCreatedAt;
        }

        public boolean hasData() { return hasData; }
        public int getBicycleCount() { return bicycleCount; }
        public int getOrderCount() { return orderCount; }
        public LocalDateTime getUserCreatedAt() { return userCreatedAt; }

        @Override
        public String toString() {
            return String.format("MigrationInfo{hasData=%s, bicycles=%d, orders=%d}",
                    hasData, bicycleCount, orderCount);
        }
    }
}