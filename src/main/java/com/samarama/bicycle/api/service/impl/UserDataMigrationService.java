package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.IncompleteUser;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.IncompleteUserRepository;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Klasa odpowiedzialna za migrację danych użytkownika podczas rejestracji.
 * Obsługuje konwersję IncompleteUser na User oraz przenoszenie powiązanych danych.
 */
@Service
public class UserDataMigrationService {

    private static final Logger logger = Logger.getLogger(UserDataMigrationService.class.getName());

    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public UserDataMigrationService(
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServiceOrderRepository serviceOrderRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Przeprowadza pełny proces migracji:
     * 1. Sprawdza czy istnieje IncompleteUser
     * 2. Jeśli tak - tworzy nowego User, przepisuje dane i usuwa IncompleteUser
     * 3. Jeśli nie - po prostu tworzy nowego User
     *
     * @param registrationDto Dane rejestracyjne
     * @return Zapisany obiekt użytkownika
     */
    @Transactional
    public User registerAndMigrateData(UserRegistrationDto registrationDto) {
        String email = registrationDto.email();

        // Sprawdź czy istnieje niekompletny użytkownik
        Optional<IncompleteUser> incompleteUserOpt = incompleteUserRepository.findByEmail(email);

        if (incompleteUserOpt.isPresent()) {
            // Znaleziono IncompleteUser - przeprowadź proces konwersji
            IncompleteUser incompleteUser = incompleteUserOpt.get();
            return convertIncompleteUserToUser(incompleteUser, registrationDto);
        } else {
            // Brak IncompleteUser - tworzenie nowego User normalną ścieżką
            return createNewUser(registrationDto);
        }
    }

    /**
     * Tworzy nowego użytkownika bez migracji danych
     *
     * @param registrationDto Dane rejestracyjne
     * @return Zapisany obiekt użytkownika
     */
    private User createNewUser(UserRegistrationDto registrationDto) {
        User user = new User();
        user.setEmail(registrationDto.email());
        user.setFirstName(registrationDto.firstName());
        user.setLastName(registrationDto.lastName());
        user.setPhoneNumber(registrationDto.phoneNumber());
        user.setPassword(passwordEncoder.encode(registrationDto.password()));
        user.addRole("ROLE_CLIENT");
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Konwertuje IncompleteUser na User i przenosi powiązane dane w następującej kolejności:
     * 1. Zmiana emaila u incomplete_user
     * 2. Utworzenie nowego użytkownika z poprawnym emailem
     * 3. Aktualizacja referencji w tabelach incomplete_bikes i service_orders
     */
    @Transactional
    private User convertIncompleteUserToUser(IncompleteUser incompleteUser, UserRegistrationDto registrationDto) {
        String email = registrationDto.email();
        Long incompleteUserId = incompleteUser.getId();

        try {
            logger.info("Starting user migration for incomplete user ID: " + incompleteUserId + " with email: " + email);

            // 1. Najpierw zmień email dla istniejącego incomplete_user
            String tempEmail = "old_" + System.currentTimeMillis() + "@temp.com";
            logger.info("Changing email of incomplete user from " + incompleteUser.getEmail() + " to " + tempEmail);

            // Użyj bezpośredniego zapytania SQL do zmiany emaila, pomijając Hibernate
            int updatedIncompleteUser = jdbcTemplate.update(
                    "UPDATE incomplete_users SET email = ? WHERE id = ?",
                    tempEmail, incompleteUserId
            );
            logger.info("Updated incomplete user email, result: " + updatedIncompleteUser);

            // 2. Utwórz nowego użytkownika z poprawnym emailem
            User newUser = new User();
            newUser.setEmail(email); // Już możemy użyć oryginalnego adresu email
            newUser.setFirstName(registrationDto.firstName());
            newUser.setLastName(registrationDto.lastName());
            newUser.setPhoneNumber(registrationDto.phoneNumber());
            newUser.setPassword(passwordEncoder.encode(registrationDto.password()));
            newUser.addRole("ROLE_CLIENT");
            newUser.setVerified(false);
            newUser.setCreatedAt(LocalDateTime.now());

            User savedUser = userRepository.saveAndFlush(newUser);
            Long newUserId = savedUser.getId();
            logger.info("Created new user with ID: " + newUserId + " and email: " + email);

            // 3. Aktualizuj referencje do rowerów
            int updatedBikes = jdbcTemplate.update(
                    "UPDATE incomplete_bikes SET owner_id = ? WHERE owner_id = ?",
                    newUserId, incompleteUserId
            );
            logger.info("Updated " + updatedBikes + " bikes to new user ID: " + newUserId);

            // 4. Aktualizuj referencje do zamówień
            int updatedOrders = jdbcTemplate.update(
                    "UPDATE service_orders SET user_id = ? WHERE user_id = ?",
                    newUserId, incompleteUserId
            );
            logger.info("Updated " + updatedOrders + " orders to new user ID: " + newUserId);

            // 5. Teraz możemy usunąć starego użytkownika (powinno zadziałać, bo już usunęliśmy wszystkie referencje)
            int deletedUsers = jdbcTemplate.update(
                    "DELETE FROM incomplete_users WHERE id = ?",
                    incompleteUserId
            );
            logger.info("Deleted incomplete user, result: " + deletedUsers);

            // 6. Sprawdź, czy wszystko się udało
            if (deletedUsers == 0) {
                logger.warning("Failed to delete incomplete user. Checking for remaining references...");

                // Diagnostyka pozostałych referencji
                List<Object[]> remainingReferences = jdbcTemplate.query(
                        "SELECT 'incomplete_bikes' AS table_name, COUNT(*) FROM incomplete_bikes WHERE owner_id = ? " +
                                "UNION ALL " +
                                "SELECT 'service_orders' AS table_name, COUNT(*) FROM service_orders WHERE user_id = ?",
                        (rs, rowNum) -> new Object[] { rs.getString(1), rs.getLong(2) },
                        incompleteUserId, incompleteUserId
                );

                for (Object[] ref : remainingReferences) {
                    logger.warning("Remaining " + ref[0] + " references: " + ref[1]);
                }

                // Próba wymuszenia usunięcia - tylko jeśli nie ma referencji
                boolean hasReferences = remainingReferences.stream()
                        .anyMatch(ref -> ((Number)ref[1]).longValue() > 0);

                if (!hasReferences) {
                    logger.info("No references found, trying forced delete...");
                    try {
                        jdbcTemplate.execute("SET session_replication_role = 'replica'");
                        int forcedDelete = jdbcTemplate.update(
                                "DELETE FROM incomplete_users WHERE id = ?",
                                incompleteUserId
                        );
                        logger.info("Forced delete result: " + forcedDelete);
                    } finally {
                        jdbcTemplate.execute("SET session_replication_role = 'origin'");
                    }
                }
            }

            logger.info("Migration completed successfully for user ID: " + newUserId);
            return savedUser;

        } catch (Exception e) {
            logger.severe("Error during user migration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Wystąpił błąd podczas migracji danych. Spróbuj ponownie.", e);
        }
    }
}