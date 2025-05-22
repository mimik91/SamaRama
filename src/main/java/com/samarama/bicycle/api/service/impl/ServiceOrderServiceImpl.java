package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private static final Logger logger = Logger.getLogger(ServiceOrderServiceImpl.class.getName());

    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final CityValidator cityValidator;
    private final ServicePackageRepository servicePackageRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServiceSlotService serviceSlotService;
    private final EmailService emailService;

    public ServiceOrderServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            UserRepository userRepository,
            CityValidator cityValidator,
            ServicePackageRepository servicePackageRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServiceSlotService serviceSlotService, EmailService emailService) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.cityValidator = cityValidator;
        this.servicePackageRepository = servicePackageRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceSlotService = serviceSlotService;
        this.emailService = emailService;
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

        for (ServiceOrder savedOrder : orders) {
            try {
                emailService.sendOrderNotificationEmail(savedOrder);
            } catch (Exception e) {
                logger.warning("Failed to send email notification for order ID: " + savedOrder.getId() + ", error: " + e.getMessage());

            }
        }

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
                .sorted(Comparator.comparing(ServiceOrder::getPickupDate))
                .map(ServiceOrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrderDto serviceOrderDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdź czy zamówienie należy do użytkownika (lub jest adminem/moderatorem)
        if (!order.getClient().getId().equals(user.getId()) &&
                !user.hasRole("ROLE_ADMIN") && !user.hasRole("ROLE_MODERATOR")) {
            return ResponseEntity.status(403).body(Map.of("message", "Nie masz uprawnień do aktualizacji tego zamówienia"));
        }

        // Walidacja daty - nie może być w przeszłości ani zbyt daleko w przyszłości
        LocalDate today = LocalDate.now();
        if (serviceOrderDto.pickupDate().isBefore(today)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
        }

        LocalDate maxDate = today.plusMonths(3);
        if (serviceOrderDto.pickupDate().isAfter(maxDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być odleglejsza niż 3 miesiące"));
        }

        // Walidacja miasta z adresu (jeśli adres jest aktualizowany)
        if (!serviceOrderDto.pickupAddress().equals(order.getPickupAddress())) {
            String city = cityValidator.extractCityFromAddress(serviceOrderDto.pickupAddress());
            if (city == null || !cityValidator.isValidCity(city)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto. Proszę wybrać miasto z listy."));
            }
        }

        // Sprawdzenie pakietu serwisowego (jeśli został zmieniony)
        ServicePackage servicePackage = order.getServicePackage();
        String packageCode = order.getServicePackageCode();

        if (serviceOrderDto.servicePackageId() != null &&
                (order.getServicePackage() == null || !order.getServicePackage().getId().equals(serviceOrderDto.servicePackageId()))) {

            servicePackage = servicePackageRepository.findById(serviceOrderDto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            packageCode = servicePackage.getCode();
        } else if (serviceOrderDto.servicePackageCode() != null && !serviceOrderDto.servicePackageCode().equals(order.getServicePackageCode())) {
            servicePackage = servicePackageRepository.findByCode(serviceOrderDto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found with code: " + serviceOrderDto.servicePackageCode()));
            packageCode = serviceOrderDto.servicePackageCode();
        }

        // Sprawdzanie dostępności slotów jeśli zmieniono datę
        if (!serviceOrderDto.pickupDate().equals(order.getPickupDate())) {
            // Sprawdź, czy są dostępne sloty serwisowe na wybrany dzień
            if (!serviceSlotService.areSlotsAvailable(serviceOrderDto.pickupDate(), 1)) {
                int maxPerDay = serviceSlotService.getMaxBikesPerDay(serviceOrderDto.pickupDate());
                int booked = serviceOrderRepository.countBikesScheduledForDate(serviceOrderDto.pickupDate());
                int available = Math.max(0, maxPerDay - booked);

                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Brak wolnych miejsc na wybrany dzień. Dostępne miejsca: " + available,
                        "availableBikes", available,
                        "maxBikesPerDay", maxPerDay
                ));
            }
        }

        // Aktualizacja zamówienia
        order.setPickupDate(serviceOrderDto.pickupDate());
        order.setPickupAddress(serviceOrderDto.pickupAddress());
        order.setPickupLatitude(serviceOrderDto.pickupLatitude());
        order.setPickupLongitude(serviceOrderDto.pickupLongitude());
        order.setServicePackage(servicePackage);
        order.setServicePackageCode(packageCode);

        if (serviceOrderDto.additionalNotes() != null) {
            order.setAdditionalNotes(serviceOrderDto.additionalNotes());
        }

        // Aktualizacja ceny (jeśli pakiet się zmienił)
        if (servicePackage != null && !servicePackage.getPrice().equals(order.getPrice())) {
            order.setPrice(servicePackage.getPrice());
        }

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane pomyślnie",
                "order", ServiceOrderResponseDto.fromEntity(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateOrderStatus(Long orderId, ServiceOrder.OrderStatus newStatus, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Klient może tylko anulować swoje zamówienie
        if (!user.hasRole("ROLE_ADMIN") && !user.hasRole("ROLE_MODERATOR")) {
            // Sprawdź czy zamówienie należy do użytkownika
            if (!order.getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Nie masz uprawnień do aktualizacji tego zamówienia"));
            }

            // Klient może tylko anulować zamówienie
            if (newStatus != ServiceOrder.OrderStatus.CANCELLED) {
                return ResponseEntity.status(403).body(Map.of("message", "Klient może tylko anulować zamówienie"));
            }

            // Sprawdź czy zamówienie można anulować (tylko w stanie PENDING lub CONFIRMED)
            if (order.getStatus() != ServiceOrder.OrderStatus.PENDING &&
                    order.getStatus() != ServiceOrder.OrderStatus.CONFIRMED) {
                return ResponseEntity.badRequest().body(Map.of("message", "Zamówienia nie można anulować w obecnym stanie: " + order.getStatus()));
            }
        }

        // Jeśli zamówienie było anulowane, tylko admin może je reaktywować
        if (order.getStatus() == ServiceOrder.OrderStatus.CANCELLED &&
                newStatus != ServiceOrder.OrderStatus.CANCELLED &&
                !user.hasRole("ROLE_ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("message", "Tylko administrator może reaktywować anulowane zamówienie"));
        }

        // Aktualizacja statusu
        order.setStatus(newStatus);
        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Status zamówienia został zaktualizowany pomyślnie",
                "order", ServiceOrderResponseDto.fromEntity(order)
        ));
    }

    @Override
    public ResponseEntity<ServiceOrderResponseDto> getServiceOrderByIdForAdmin(Long orderId) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();
        return ResponseEntity.ok(ServiceOrderResponseDto.fromEntity(order));
    }


    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrderByAdmin(Long orderId, ServiceOrderDto dto, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Aktualizacja pól z DTO - zwróć uwagę na użycie metod dostępowych rekordu
        if (dto.pickupDate() != null) {
            order.setPickupDate(dto.pickupDate());
        }

        if (dto.pickupAddress() != null) {
            order.setPickupAddress(dto.pickupAddress());
        }

        // Aktualizacja pakietu serwisowego, jeśli podano
        if (dto.servicePackageId() != null) {
            Optional<ServicePackage> packageOpt = servicePackageRepository.findById(dto.servicePackageId());

            if (packageOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Pakiet serwisowy o podanym ID nie istnieje"));
            }

            ServicePackage servicePackage = packageOpt.get();
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setPrice(servicePackage.getPrice());
        }

        // Aktualizacja notatek dodatkowych
        if (dto.additionalNotes() != null) {
            order.setAdditionalNotes(dto.additionalNotes());
        }

        // Zapisanie informacji o modyfikacji
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało zaktualizowane"));
    }

    /**
     * Anuluj zamówienie serwisowe (dla administratora)
     * @param orderId ID zamówienia
     * @param adminEmail email administratora
     * @return wynik operacji
     */
    @Override
    @Transactional
    public ResponseEntity<?> cancelServiceOrderByAdmin(Long orderId, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Sprawdzenie, czy zamówienie można anulować
        if (order.getStatus() == ServiceOrder.OrderStatus.DELIVERED ||
                order.getStatus() == ServiceOrder.OrderStatus.COMPLETED ||
                order.getStatus() == ServiceOrder.OrderStatus.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nie można anulować zamówienia w statusie " + order.getStatus()));
        }

        // Aktualizacja statusu na anulowany
        order.setStatus(ServiceOrder.OrderStatus.CANCELLED);

        // Zapisanie informacji o modyfikacji
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało anulowane"));
    }

    /**
     * Aktualizuj status zamówienia (dla administratora)
     * @param orderId ID zamówienia
     * @param newStatus nowy status zamówienia
     * @param adminEmail email administratora
     * @return wynik operacji
     */
    @Override
    @Transactional
    public ResponseEntity<?> updateOrderStatusByAdmin(Long orderId, ServiceOrder.OrderStatus newStatus, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Walidacja zmiany statusu
        if (!isValidStatusChange(order.getStatus(), newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nieprawidłowa zmiana statusu z " + order.getStatus() + " na " + newStatus));
        }

        // Aktualizacja statusu
        order.setStatus(newStatus);

        // Zapisanie informacji o modyfikacji
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Status zamówienia został zaktualizowany"));
    }

    /**
     * Sprawdź, czy zmiana statusu jest prawidłowa
     */
    private boolean isValidStatusChange(ServiceOrder.OrderStatus currentStatus, ServiceOrder.OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true; // Brak zmiany
        }

        if (currentStatus == ServiceOrder.OrderStatus.CANCELLED) {
            return false; // Nie można zmienić z anulowanego
        }

        if (newStatus == ServiceOrder.OrderStatus.CANCELLED) {
            return true; // Można anulować z dowolnego statusu
        }

        // Sprawdzenie prawidłowej progresji statusów
        switch (currentStatus) {
            case PENDING:
                return newStatus == ServiceOrder.OrderStatus.CONFIRMED;
            case CONFIRMED:
                return newStatus == ServiceOrder.OrderStatus.PICKED_UP;
            case PICKED_UP:
                return newStatus == ServiceOrder.OrderStatus.IN_SERVICE;
            case IN_SERVICE:
                return newStatus == ServiceOrder.OrderStatus.COMPLETED;
            case COMPLETED:
                return newStatus == ServiceOrder.OrderStatus.DELIVERED;
            case DELIVERED:
                return false; // Koniec przepływu
            default:
                return false;
        }
    }
}