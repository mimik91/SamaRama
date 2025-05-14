package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.IncompleteUser;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.IncompleteUserRepository;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    public UserDataMigrationService(
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServiceOrderRepository serviceOrderRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Przeprowadza pełny proces migracji:
     * 1. Sprawdza czy istnieje IncompleteUser
     * 2. Jeśli tak - tworzy nowego User z tymczasowym emailem, przepisuje dane,
     *    usuwa IncompleteUser i aktualizuje email
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
     * Konwertuje IncompleteUser na User:
     * 1. Tworzy nowego User z tymczasowym emailem
     * 2. Przepisuje referencje rowerów i zamówień
     * 3. Usuwa IncompleteUser
     * 4. Aktualizuje email nowego User
     *
     * @param incompleteUser IncompleteUser do konwersji
     * @param registrationDto Dane rejestracyjne
     * @return Zapisany obiekt użytkownika
     */
    private User convertIncompleteUserToUser(IncompleteUser incompleteUser, UserRegistrationDto registrationDto) {
        try {
            String originalEmail = registrationDto.email();
            String tempEmail = "tmp_" + System.currentTimeMillis() % 10000000 + "@tmp.co";

            // 1. Utwórz nowego User z tymczasowym emailem
            User newUser = new User();
            newUser.setEmail(tempEmail); // Tymczasowy email
            newUser.setFirstName(registrationDto.firstName());
            newUser.setLastName(registrationDto.lastName());
            newUser.setPhoneNumber(registrationDto.phoneNumber());
            newUser.setPassword(passwordEncoder.encode(registrationDto.password()));
            newUser.addRole("ROLE_CLIENT");
            newUser.setVerified(false);
            newUser.setCreatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(newUser);

            // 2. Przepisz referencje rowerów - pobieramy kopię kolekcji
            Set<IncompleteBike> bikes = new HashSet<>(incompleteUser.getBicycles());
            for (IncompleteBike bike : bikes) {
                bike.setOwner(savedUser); // Zmień właściciela na nowego usera
            }
            incompleteBikeRepository.saveAll(bikes);

            // 3. Przepisz referencje zamówień - pobieramy kopię kolekcji
            List<ServiceOrder> orders = new ArrayList<>(incompleteUser.getServiceOrders());
            for (ServiceOrder order : orders) {
                order.setClient(savedUser); // Zmień klienta na nowego usera
            }
            serviceOrderRepository.saveAll(orders);

            // 4. Usuń starego IncompleteUser
            incompleteUserRepository.delete(incompleteUser);

            // 5. Aktualizuj email nowego User na właściwy
            savedUser.setEmail(originalEmail);
            return userRepository.save(savedUser);

        } catch (Exception e) {
            logger.severe("Błąd podczas konwersji użytkownika: " + e.getMessage());
            throw new RuntimeException("Wystąpił błąd podczas konwersji użytkownika. Spróbuj ponownie.", e);
        }
    }
}