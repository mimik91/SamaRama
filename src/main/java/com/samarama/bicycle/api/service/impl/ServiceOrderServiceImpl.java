package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {
    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final CityValidator cityValidator;
    private final ServicePackageRepository servicePackageRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServiceSlotService serviceSlotService;

    public ServiceOrderServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            UserRepository userRepository,
            CityValidator cityValidator,
            ServicePackageRepository servicePackageRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServiceSlotService serviceSlotService) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.cityValidator = cityValidator;
        this.servicePackageRepository = servicePackageRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceSlotService = serviceSlotService;
    }

    @Override
    public List<ServiceOrderResponseDto> getUserServiceOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        List<ServiceOrder> orders = serviceOrderRepository.findByClient(user);

        // Konwersja encji na DTO
        return orders.stream()
                .map(ServiceOrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceOrderResponseDto> getBicycleServiceOrders(Long bicycleId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
        if (bikeOpt.isEmpty()) {
            return List.of();
        }

        IncompleteBike bike = bikeOpt.get();

        // Sprawdź czy rower należy do użytkownika
        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return List.of();
        }

        List<ServiceOrder> orders = serviceOrderRepository.findByBicycle(bike);

        // Konwersja encji na DTO
        return orders.stream()
                .map(ServiceOrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<ServiceOrderResponseDto> getServiceOrderById(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Konwersja encji na DTO
        return ResponseEntity.ok(ServiceOrderResponseDto.fromEntity(order));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createServiceOrder(ServiceOrderDto serviceOrderDto, String userEmail) {
        // Walidacja daty - nie może być w przeszłości ani zbyt daleko w przyszłości
        LocalDate today = LocalDate.now();
        if (serviceOrderDto.pickupDate().isBefore(today)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
        }

        LocalDate maxDate = today.plusMonths(1);
        if (serviceOrderDto.pickupDate().isAfter(maxDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być odleglejsza niż miesiąc"));
        }

        // Walidacja miasta z adresu
        String city = cityValidator.extractCityFromAddress(serviceOrderDto.pickupAddress());
        if (city == null || !cityValidator.isValidCity(city)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto. Proszę wybrać miasto z listy."));
        }

        // Walidacja dostępnych slotów serwisowych
        int bikesCount = serviceOrderDto.bicycleIds().size();

        // Sprawdź, czy liczba rowerów nie przekracza limitu na jedno zamówienie
        if (!serviceSlotService.isWithinMaxBikesPerOrder(serviceOrderDto.pickupDate(), bikesCount)) {
            int maxPerOrder = serviceSlotService.getMaxBikesPerOrder(serviceOrderDto.pickupDate());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Przekroczono maksymalną liczbę rowerów na jedno zamówienie (" + maxPerOrder + "). Proszę rozłożyć zamówienie na kilka dni.",
                    "maxBikesPerOrder", maxPerOrder
            ));
        }

        // Sprawdź, czy są dostępne sloty serwisowe na wybrany dzień
        if (!serviceSlotService.areSlotsAvailable(serviceOrderDto.pickupDate(), bikesCount)) {
            int maxPerDay = serviceSlotService.getMaxBikesPerDay(serviceOrderDto.pickupDate());
            int booked = serviceOrderRepository.countBikesScheduledForDate(serviceOrderDto.pickupDate());
            int available = Math.max(0, maxPerDay - booked);

            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Brak wystarczającej liczby wolnych miejsc na wybrany dzień. Dostępne miejsca: " + available + ". Proszę wybrać inny termin lub zmniejszyć liczbę rowerów.",
                    "availableBikes", available,
                    "maxBikesPerDay", maxPerDay
            ));
        }

        // Pobierz użytkownika
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        // Pobierz pakiet serwisowy
        ServicePackage servicePackage = null;
        String packageCode = null;

        if (serviceOrderDto.servicePackageId() != null) {
            servicePackage = servicePackageRepository.findById(serviceOrderDto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            packageCode = servicePackage.getCode();
        } else if (serviceOrderDto.servicePackageCode() != null) {
            // Dla kompatybilności wstecznej
            servicePackage = servicePackageRepository.findByCode(serviceOrderDto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found with code: " + serviceOrderDto.servicePackageCode()));
            packageCode = serviceOrderDto.servicePackageCode();
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Pakiet serwisowy jest wymagany"));
        }

        // Walidacja rowerów
        List<Long> bicycleIds = serviceOrderDto.bicycleIds();
        if (bicycleIds == null || bicycleIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lista rowerów jest wymagana"));
        }

        // Dla każdego roweru utwórz osobne zamówienie
        List<ServiceOrder> orders = new ArrayList<>();
        for (Long bicycleId : bicycleIds) {
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
            if (bikeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Rower o ID " + bicycleId + " nie został znaleziony"));
            }

            IncompleteBike bike = bikeOpt.get();

            // Sprawdź, czy rower należy do użytkownika
            if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień do wybranego roweru"));
            }

            // Utwórz zamówienie
            ServiceOrder order = new ServiceOrder();
            order.setBicycle(bike);
            order.setClient(user);
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(packageCode);
            order.setPickupDate(serviceOrderDto.pickupDate());
            order.setPickupAddress(serviceOrderDto.pickupAddress());
            order.setPickupLatitude(serviceOrderDto.pickupLatitude());
            order.setPickupLongitude(serviceOrderDto.pickupLongitude());
            order.setPrice(servicePackage.getPrice());
            order.setAdditionalNotes(serviceOrderDto.additionalNotes());
            order.setStatus(ServiceOrder.OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            orders.add(order);
        }

        // Zapisz wszystkie zamówienia
        serviceOrderRepository.saveAll(orders);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie serwisowe zostało utworzone pomyślnie",
                "orderCount", orders.size(),
                "orderIds", orders.stream().map(ServiceOrder::getId).collect(Collectors.toList())
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> cancelServiceOrder(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Nie masz uprawnień do anulowania tego zamówienia"));
        }

        // Sprawdź czy zamówienie można anulować (tylko w stanie PENDING lub CONFIRMED)
        if (order.getStatus() != ServiceOrder.OrderStatus.PENDING &&
                order.getStatus() != ServiceOrder.OrderStatus.CONFIRMED) {
            return ResponseEntity.badRequest().body(Map.of("message", "Zamówienia nie można anulować w obecnym stanie: " + order.getStatus()));
        }

        // Anuluj zamówienie
        order.setStatus(ServiceOrder.OrderStatus.CANCELLED);
        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało pomyślnie anulowane"));
    }

    @Override
    public ResponseEntity<?> getServicePackagePrice(String servicePackageCode) {
        Optional<ServicePackage> packageEntity = servicePackageRepository.findByCode(servicePackageCode);
        if (packageEntity.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy pakiet serwisowy"));
        }

        return ResponseEntity.ok(Map.of(
                "servicePackage", servicePackageCode,
                "price", packageEntity.get().getPrice()
        ));
    }

    @Override
    public long countServiceOrders() {
        return serviceOrderRepository.findAllActiveOrders().size();
    }

    @Override
    public List<ServiceOrderResponseDto> getAllServiceOrders() {
        return serviceOrderRepository.findAllActiveOrders().stream()
                .map(ServiceOrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}