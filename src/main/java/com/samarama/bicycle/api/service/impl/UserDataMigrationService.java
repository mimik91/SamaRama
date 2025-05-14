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
     * 1. Przygotowuje IncompleteUser (jeśli istnieje)
     * 2. Tworzy nowego User
     * 3. Migruje dane powiązane
     *
     * @param registrationDto Dane rejestracyjne
     * @return Zapisany obiekt użytkownika
     */
    @Transactional
    public User registerAndMigrateData(UserRegistrationDto registrationDto) {
        String email = registrationDto.email();

        // Sprawdź czy istnieje niekompletny użytkownik
        Optional<IncompleteUser> incompleteUserOpt = incompleteUserRepository.findByEmail(email);

        // Jeśli istnieje, anonimizuj go zanim utworzymy nowego użytkownika
        if (incompleteUserOpt.isPresent()) {
            IncompleteUser incompleteUser = incompleteUserOpt.get();
            anonymizeIncompleteUser(incompleteUser);
        }

        // Teraz możemy utworzyć nowego użytkownika
        User newUser = new User();
        newUser.setEmail(email); // teraz bezpieczne, bo już zmieniliśmy email IncompleteUser
        newUser.setFirstName(registrationDto.firstName());
        newUser.setLastName(registrationDto.lastName());
        newUser.setPhoneNumber(registrationDto.phoneNumber());
        newUser.setPassword(passwordEncoder.encode(registrationDto.password()));
        newUser.addRole("ROLE_CLIENT");
        newUser.setVerified(false);
        newUser.setCreatedAt(LocalDateTime.now());

        Optional<User> inncompleteUserOpt = userRepository.findByEmail(email);
        System.out.println("AAAA");
        User savedUser = userRepository.save(newUser);



        // Jeśli istniał niekompletny użytkownik, migruj jego dane
        if (incompleteUserOpt.isPresent()) {
            IncompleteUser incompleteUser = incompleteUserOpt.get();

            try {
                // Migruj rowery
                migrateUserBicycles(incompleteUser, savedUser);

                // Migruj zamówienia serwisowe
                migrateServiceOrders(incompleteUser, savedUser);

                // Usuń stary IncompleteUser
                incompleteUserRepository.delete(incompleteUser);

            } catch (Exception e) {
                logger.severe("Błąd podczas migracji danych użytkownika: " + e.getMessage());
                throw new RuntimeException("Wystąpił błąd podczas migracji danych użytkownika", e);
            }
        }

        return savedUser;
    }

    /**
     * Anonimizuje dane IncompleteUser, aby uniknąć konfliktów.
     *
     * @param incompleteUser IncompleteUser do anonimizacji
     */
    private void anonymizeIncompleteUser(IncompleteUser incompleteUser) {
        String tempEmail = UUID.randomUUID().toString() + "@gmail.com";
        incompleteUser.setEmail(tempEmail);
        incompleteUserRepository.save(incompleteUser);
    }

    /**
     * Migruje rowery z IncompleteUser do nowego User.
     *
     * @param incompleteUser Stary incompleteUser
     * @param newUser Nowy użytkownik
     * @return Mapa mapująca stare ID na nowe obiekty rowerów
     */
    private void migrateUserBicycles(IncompleteUser incompleteUser, User newUser) {
        Map<Long, IncompleteBike> oldToNewBikeMap = new HashMap<>();

        if (incompleteUser.getBicycles() == null || incompleteUser.getBicycles().isEmpty()) {
            return;
        }

        List<IncompleteBike> newBikes = new ArrayList<>();
        List<IncompleteBike> oldBikes = new ArrayList<>(incompleteUser.getBicycles());

        for (IncompleteBike oldBike : oldBikes) {
            IncompleteBike newBike = copyBikeData(oldBike, newUser);
            newBikes.add(newBike);
        }

        // Zapisz wszystkie nowe rowery
        List<IncompleteBike> savedBikes = incompleteBikeRepository.saveAll(newBikes);

        // Utwórz mapowanie ze starych ID na nowe obiekty
        for (int i = 0; i < oldBikes.size(); i++) {
            oldToNewBikeMap.put(oldBikes.get(i).getId(), savedBikes.get(i));
        }
    }


    private IncompleteBike copyBikeData(IncompleteBike oldBike, User newOwner) {
        IncompleteBike newBike = new IncompleteBike();

        newBike.setBrand(oldBike.getBrand());
        newBike.setModel(oldBike.getModel());
        newBike.setType(oldBike.getType());
        newBike.setFrameMaterial(oldBike.getFrameMaterial());
        newBike.setOwner(newOwner);

        return newBike;
    }

    private void migrateServiceOrders(IncompleteUser incompleteUser, User newUser) {
        if (incompleteUser.getServiceOrders() == null || incompleteUser.getServiceOrders().isEmpty()) {
            return;
        }

        List<ServiceOrder> newOrders = new ArrayList<>();
        List<ServiceOrder> oldOrders = new ArrayList<>(incompleteUser.getServiceOrders());

        for (ServiceOrder oldOrder : oldOrders) {
            ServiceOrder newOrder = copyOrderData(oldOrder, newUser);
            newOrders.add(newOrder);
        }
        System.out.println("BBBB");
        // Zapisz wszystkie nowe zamówienia
        serviceOrderRepository.saveAll(newOrders);
    }

    /**
     * Kopiuje dane zamówienia serwisowego do nowego obiektu.
     *
     * @param oldOrder Stare zamówienie
     * @param newClient Nowy klient
     * @return Nowy obiekt zamówienia z skopiowanymi danymi
     */
    private ServiceOrder copyOrderData(ServiceOrder oldOrder, User newClient) {
        ServiceOrder newOrder = new ServiceOrder();

        // Bezpieczne kopiowanie pól - tylko jeśli są dostępne

        // OrderDate
        if (oldOrder.getOrderDate() != null) {
            newOrder.setOrderDate(oldOrder.getOrderDate());
        }

        // PickupDate
        if (oldOrder.getPickupDate() != null) {
            newOrder.setPickupDate(oldOrder.getPickupDate());
        }

        // PickupAddress
        if (oldOrder.getPickupAddress() != null) {
            newOrder.setPickupAddress(oldOrder.getPickupAddress());
        }

        // Price
        if (oldOrder.getPrice() != null) {
            newOrder.setPrice(oldOrder.getPrice());
        }

        // Status
        if (oldOrder.getStatus() != null) {
            newOrder.setStatus(oldOrder.getStatus());
        }

        // AdditionalNotes
        if (oldOrder.getAdditionalNotes() != null) {
            newOrder.setAdditionalNotes(oldOrder.getAdditionalNotes());
        }

        // ServiceNotes
        if (oldOrder.getServiceNotes() != null) {
            newOrder.setServiceNotes(oldOrder.getServiceNotes());
        }

        // ServicePackage - bezpieczniejsze podejście
        if (oldOrder.getServicePackage() != null) {
            newOrder.setServicePackage(oldOrder.getServicePackage());
        }

        // Pominięte setServicePackageId, ponieważ może nie istnieć

        // Ustaw nowego klienta
        newOrder.setClient(newClient);

        return newOrder;
    }
}