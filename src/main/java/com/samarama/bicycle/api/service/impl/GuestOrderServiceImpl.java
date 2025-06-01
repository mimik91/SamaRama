package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.GuestBicycleDto;
import com.samarama.bicycle.api.dto.GuestServiceOrderDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.GuestOrderService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuestOrderServiceImpl implements GuestOrderService {

    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final CityValidator cityValidator;
    private final ServiceSlotService serviceSlotService;
    private final EmailService emailService;

    public GuestOrderServiceImpl(
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServicePackageRepository servicePackageRepository,
            ServiceOrderRepository serviceOrderRepository,
            CityValidator cityValidator,
            ServiceSlotService serviceSlotService, EmailService emailService) {
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.cityValidator = cityValidator;
        this.serviceSlotService = serviceSlotService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public ResponseEntity<?> processGuestOrder(GuestServiceOrderDto orderDto) {
        // Walidacja danych wejściowych
        if (orderDto.bicycles() == null || orderDto.bicycles().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Brak rowerów w zamówieniu"));
        }

        // Walidacja pakietu serwisowego
        Optional<ServicePackage> packageOpt = servicePackageRepository.findById(orderDto.servicePackageId());
        if (packageOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy pakiet serwisowy"));
        }
        ServicePackage servicePackage = packageOpt.get();

        // Walidacja miasta
        String city = orderDto.city();
        if (city == null || !cityValidator.isValidCity(city)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto"));
        }

        // Walidacja daty odbioru
        if (orderDto.pickupDate() == null || orderDto.pickupDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowa data odbioru"));
        }

        // Walidacja dostępnych slotów serwisowych
        int bikesCount = orderDto.bicycles().size();

        // Sprawdź, czy liczba rowerów nie przekracza limitu na jedno zamówienie
        if (!serviceSlotService.isWithinMaxBikesPerOrder(orderDto.pickupDate(), bikesCount)) {
            int maxPerOrder = serviceSlotService.getMaxBikesPerOrder(orderDto.pickupDate());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Przekroczono maksymalną liczbę rowerów na jedno zamówienie (" + maxPerOrder + "). Proszę rozłożyć zamówienie na kilka dni.",
                    "maxBikesPerOrder", maxPerOrder
            ));
        }

        // Sprawdź, czy są dostępne sloty serwisowe na wybrany dzień
        if (!serviceSlotService.areSlotsAvailable(orderDto.pickupDate(), bikesCount)) {
            int maxPerDay = serviceSlotService.getMaxBikesPerDay(orderDto.pickupDate());
            int booked = serviceOrderRepository.countByPickupDate(orderDto.pickupDate());
            int available = Math.max(0, maxPerDay - booked);

            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Brak wystarczającej liczby wolnych miejsc na wybrany dzień. Dostępne miejsca: " + available + ". Proszę wybrać inny termin lub zmniejszyć liczbę rowerów.",
                    "availableBikes", available,
                    "maxBikesPerDay", maxPerDay
            ));
        }

        // 1. Tworzenie użytkownika tymczasowego (IncompleteUser)
        IncompleteUser incompleteUser = createOrFindIncompleteUser(orderDto);

        // 2. Tworzenie rowerów
        List<IncompleteBike> bikes = createIncompleteBikes(orderDto.bicycles(), incompleteUser);

        // 3. Tworzenie zamówień serwisowych
        List<ServiceOrder> orders = createServiceOrders(bikes, servicePackage, orderDto, incompleteUser);

        // 4. Zapisywanie zamówień
        List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

        for (ServiceOrder savedOrder : savedOrders) {
            try {
                emailService.sendOrderNotificationEmail(savedOrder);
            } catch (Exception e) {
                // Log błędu ale nie przerywaj procesu
                System.err.println("Failed to send email notification for guest order ID: " + savedOrder.getId() + ", error: " + e.getMessage());
            }
        }

        // Pobranie ID zamówień
        List<Long> orderIds = savedOrders.stream()
                .map(ServiceOrder::getId)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                "orderIds", orderIds,
                "userId", incompleteUser.getId()
        ));
    }

    private IncompleteUser createOrFindIncompleteUser(GuestServiceOrderDto orderDto) {
        // Szukamy użytkownika po emailu
        Optional<IncompleteUser> existingUser = incompleteUserRepository.findByEmail(orderDto.email());

        if (existingUser.isPresent()) {
            // Jeśli istnieje, aktualizujemy dane
            IncompleteUser user = existingUser.get();
            user.setPhoneNumber(orderDto.phone());
            return incompleteUserRepository.save(user);
        } else {
            // Jeśli nie istnieje, tworzymy nowego
            IncompleteUser newUser = new IncompleteUser();
            newUser.setEmail(orderDto.email());
            newUser.setPhoneNumber(orderDto.phone());
            newUser.setCreatedAt(LocalDateTime.now());
            return incompleteUserRepository.save(newUser);
        }
    }

    private List<IncompleteBike> createIncompleteBikes(List<GuestBicycleDto> bicycleDtos, IncompleteUser owner) {
        List<IncompleteBike> bikes = new ArrayList<>();

        for (GuestBicycleDto bikeDto : bicycleDtos) {
            IncompleteBike bike = new IncompleteBike();
            bike.setBrand(bikeDto.brand());
            bike.setModel(bikeDto.model());
            // Możemy dodać dodatkowe informacje do pola notatek
            if (bikeDto.additionalInfo() != null && !bikeDto.additionalInfo().isEmpty()) {
                bike.setType(bikeDto.additionalInfo());
            }
            bike.setOwner(owner);
            bike.setCreatedAt(LocalDateTime.now());

            bikes.add(incompleteBikeRepository.save(bike));
        }

        return bikes;
    }

    private List<ServiceOrder> createServiceOrders(
            List<IncompleteBike> bikes,
            ServicePackage servicePackage,
            GuestServiceOrderDto orderDto,
            IncompleteUser user) {

        List<ServiceOrder> orders = new ArrayList<>();

        // Dla każdego roweru tworzymy nowe zamówienie
        for (IncompleteBike bike : bikes) {
            ServiceOrder order = new ServiceOrder();
            order.setBicycle(bike);
            order.setClient(user);
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setPickupDate(orderDto.pickupDate());
            order.setPickupAddress(orderDto.address() + ", " + orderDto.city());
            order.setServicePrice(servicePackage.getPrice());
            order.setAdditionalNotes(orderDto.notes());
            order.setStatus(ServiceOrder.OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            orders.add(order);
        }

        return orders;
    }
}