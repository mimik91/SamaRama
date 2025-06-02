package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class TransportOrderServiceImpl implements TransportOrderService {

    private static final Logger logger = Logger.getLogger(TransportOrderServiceImpl.class.getName());

    private final TransportOrderRepository transportOrderRepository;
    private final UserRepository userRepository;
    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServiceSlotService serviceSlotService;
    private final EmailService emailService;
    private final CityValidator cityValidator;

    public TransportOrderServiceImpl(
            TransportOrderRepository transportOrderRepository,
            UserRepository userRepository,
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            BikeServiceRepository bikeServiceRepository,
            ServiceSlotService serviceSlotService,
            EmailService emailService,
            CityValidator cityValidator) {
        this.transportOrderRepository = transportOrderRepository;
        this.userRepository = userRepository;
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.serviceSlotService = serviceSlotService;
        this.emailService = emailService;
        this.cityValidator = cityValidator;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createTransportOrder(TransportOrderDto dto, String userEmail) {
        // Walidacja danych wejściowych
        if (!dto.isValidForLoggedUser()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe dane zamówienia"));
        }

        // Walidacja daty
        if (dto.pickupDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
        }

        // Walidacja miasta
        String city = cityValidator.extractCityFromAddress(dto.pickupAddress());
        if (city == null || !cityValidator.isValidCity(city)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto"));
        }

        // Walidacja dostępności slotów
        int ordersCount = dto.bicycleIds().size();
        if (!serviceSlotService.areSlotsAvailable(dto.pickupDate(), ordersCount)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Brak dostępnych miejsc na wybrany dzień",
                    "availableSlots", getAvailableSlots(dto.pickupDate())
            ));
        }

        // Pobierz użytkownika
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pobierz serwis docelowy
        BikeService targetService = bikeServiceRepository.findById(dto.targetServiceId())
                .orElseThrow(() -> new RuntimeException("Target service not found"));

        // Walidacja - dla czystego transportu nie może być serwis własny
        if (targetService.getId().equals(1L)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Dla transportu do serwisu własnego użyj endpointu /api/service-orders"
            ));
        }

        // Walidacja rowerów
        List<IncompleteBike> bikes = validateAndGetBikes(dto.bicycleIds(), user.getId());

        // Utwórz zamówienia transportowe (jedno na rower)
        List<TransportOrder> orders = new ArrayList<>();
        for (IncompleteBike bike : bikes) {
            TransportOrder order = new TransportOrder();
            order.setBicycle(bike);
            order.setClient(user);
            order.setPickupDate(dto.pickupDate());
            order.setPickupAddress(dto.pickupAddress());
            order.setPickupLatitude(dto.pickupLatitude());
            order.setPickupLongitude(dto.pickupLongitude());
            order.setPickupTimeFrom(dto.pickupTimeFrom());
            order.setPickupTimeTo(dto.pickupTimeTo());

            order.setTargetService(targetService);
            order.setDeliveryAddress(dto.deliveryAddress() != null ? dto.deliveryAddress() : targetService.getFullAddress());
            order.setDeliveryLatitude(dto.deliveryLatitude() != null ? dto.deliveryLatitude() : targetService.getLatitude());
            order.setDeliveryLongitude(dto.deliveryLongitude() != null ? dto.deliveryLongitude() : targetService.getLongitude());

            order.setTransportPrice(dto.transportPrice());
            order.setEstimatedTime(dto.estimatedTime());
            order.setTransportNotes(dto.transportNotes());
            order.setAdditionalNotes(dto.additionalNotes());
            order.setStatus(TransportOrder.OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            orders.add(order);
        }

        // Zapisz zamówienia
        List<TransportOrder> savedOrders = transportOrderRepository.saveAll(orders);

        // Wyślij powiadomienia email
        for (TransportOrder order : savedOrders) {
            try {
                // emailService.sendTransportOrderNotification(order);
                logger.info("Transport order created: " + order.getId());
            } catch (Exception e) {
                logger.warning("Failed to send email notification for transport order: " + order.getId());
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienia transportowe zostały utworzone pomyślnie",
                "orderIds", savedOrders.stream().map(TransportOrder::getId).collect(Collectors.toList()),
                "orderCount", savedOrders.size()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createGuestTransportOrder(TransportOrderDto dto) {
        // Walidacja danych gościa
        if (!dto.isValidForGuest()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe dane zamówienia gościa"));
        }

        // Walidacja daty
        if (dto.pickupDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data odbioru nie może być w przeszłości"));
        }

        // Walidacja miasta
        if (dto.city() == null || !cityValidator.isValidCity(dto.city())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowe miasto"));
        }

        // Walidacja dostępności slotów
        int ordersCount = dto.bicycles().size();
        if (!serviceSlotService.areSlotsAvailable(dto.pickupDate(), ordersCount)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Brak dostępnych miejsc na wybrany dzień",
                    "availableSlots", getAvailableSlots(dto.pickupDate())
            ));
        }

        // Pobierz serwis docelowy
        BikeService targetService = bikeServiceRepository.findById(dto.targetServiceId())
                .orElseThrow(() -> new RuntimeException("Target service not found"));

        // Walidacja - dla czystego transportu nie może być serwis własny
        if (targetService.getId().equals(1L)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Dla transportu do serwisu własnego użyj endpointu /api/service-orders/guest"
            ));
        }

        // Utwórz lub znajdź użytkownika tymczasowego
        IncompleteUser guestUser = createOrFindIncompleteUser(dto.clientEmail(), dto.clientPhone());

        // Utwórz rowery
        List<IncompleteBike> bikes = createIncompleteBikes(dto.bicycles(), guestUser);

        // Utwórz zamówienia transportowe
        List<TransportOrder> orders = new ArrayList<>();
        for (IncompleteBike bike : bikes) {
            TransportOrder order = new TransportOrder();
            order.setBicycle(bike);
            order.setClient(guestUser);
            order.setPickupDate(dto.pickupDate());
            order.setPickupAddress(dto.pickupAddress() + ", " + dto.city());
            order.setPickupLatitude(dto.pickupLatitude());
            order.setPickupLongitude(dto.pickupLongitude());
            order.setPickupTimeFrom(dto.pickupTimeFrom());
            order.setPickupTimeTo(dto.pickupTimeTo());

            order.setTargetService(targetService);
            order.setDeliveryAddress(dto.deliveryAddress() != null ? dto.deliveryAddress() : targetService.getFullAddress());
            order.setDeliveryLatitude(dto.deliveryLatitude() != null ? dto.deliveryLatitude() : targetService.getLatitude());
            order.setDeliveryLongitude(dto.deliveryLongitude() != null ? dto.deliveryLongitude() : targetService.getLongitude());

            order.setTransportPrice(dto.transportPrice());
            order.setEstimatedTime(dto.estimatedTime());
            order.setTransportNotes(dto.transportNotes());
            order.setAdditionalNotes(dto.additionalNotes());
            order.setStatus(TransportOrder.OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            orders.add(order);
        }

        // Zapisz zamówienia
        List<TransportOrder> savedOrders = transportOrderRepository.saveAll(orders);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienia transportowe zostały utworzone pomyślnie",
                "orderIds", savedOrders.stream().map(TransportOrder::getId).collect(Collectors.toList()),
                "guestUserId", guestUser.getId()
        ));
    }

    @Override
    public List<UnifiedOrderResponseDto> getUserTransportOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<TransportOrder> orders = transportOrderRepository.findPureTransportOrders();

        return orders.stream()
                .filter(order -> order.getClient().getId().equals(user.getId()))
                .map(UnifiedOrderResponseDto::fromTransportOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedOrderResponseDto> getAllUserOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<TransportOrder> orders = transportOrderRepository.findByClient(user);

        return orders.stream()
                .map(order -> order instanceof ServiceOrder ?
                        UnifiedOrderResponseDto.fromServiceOrder((ServiceOrder) order) :
                        UnifiedOrderResponseDto.fromTransportOrder(order))
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<UnifiedOrderResponseDto> getOrderDetails(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        UnifiedOrderResponseDto dto = order instanceof ServiceOrder ?
                UnifiedOrderResponseDto.fromServiceOrder((ServiceOrder) order) :
                UnifiedOrderResponseDto.fromTransportOrder(order);

        return ResponseEntity.ok(dto);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateTransportOrder(Long orderId, TransportOrderDto dto, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        // Sprawdź czy można modyfikować
        if (!order.canBeCancelled()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        // Sprawdź czy to nie jest ServiceOrder (nie można konwertować)
        if (order instanceof ServiceOrder) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Użyj endpointu /api/service-orders/{id} dla zamówień serwisowych"
            ));
        }

        // Aktualizuj pola
        updateTransportOrderFields(order, dto);
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        transportOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane",
                "order", UnifiedOrderResponseDto.fromTransportOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> cancelOrder(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        // Sprawdź czy można anulować
        if (!order.canBeCancelled()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można anulować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        order.setStatus(TransportOrder.OrderStatus.CANCELLED);
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        transportOrderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało anulowane"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateOrderStatus(Long orderId, String newStatus, String userEmail) {
        User user = getUserByEmail(userEmail);

        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        // Klient może tylko anulować
        if (!"CANCELLED".equals(newStatus)) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Klient może tylko anulować zamówienie"
            ));
        }

        return cancelOrder(orderId, userEmail);
    }

    // === ADMIN METHODS ===

    @Override
    public List<UnifiedOrderResponseDto> getAllTransportOrders() {
        List<TransportOrder> orders = transportOrderRepository.findPureTransportOrders();
        return orders.stream()
                .map(UnifiedOrderResponseDto::fromTransportOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedOrderResponseDto> getAllOrders() {
        List<TransportOrder> orders = transportOrderRepository.findAllOrders();
        return orders.stream()
                .map(order -> order instanceof ServiceOrder ?
                        UnifiedOrderResponseDto.fromServiceOrder((ServiceOrder) order) :
                        UnifiedOrderResponseDto.fromTransportOrder(order))
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedOrderResponseDto> searchOrders(String searchTerm) {
        List<TransportOrder> orders = transportOrderRepository.searchByClientInfo(searchTerm);
        return orders.stream()
                .map(order -> order instanceof ServiceOrder ?
                        UnifiedOrderResponseDto.fromServiceOrder((ServiceOrder) order) :
                        UnifiedOrderResponseDto.fromTransportOrder(order))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateTransportOrderByAdmin(Long orderId, TransportOrderDto dto, String adminEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Admin może modyfikować wszystko, ale nie konwertować ServiceOrder
        if (order instanceof ServiceOrder) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Użyj endpointu /api/service-orders/admin/{id} dla zamówień serwisowych"
            ));
        }

        updateTransportOrderFields(order, dto);
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        transportOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane",
                "order", UnifiedOrderResponseDto.fromTransportOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteTransportOrder(Long orderId, String adminEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Nie można usunąć ServiceOrder przez ten endpoint
        if (order instanceof ServiceOrder) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Użyj endpointu /api/service-orders/admin/{id} dla zamówień serwisowych"
            ));
        }

        transportOrderRepository.deleteById(orderId);
        logger.info("Admin " + adminEmail + " deleted transport order " + orderId);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało usunięte"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateOrderStatusByAdmin(Long orderId, String newStatus, String adminEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            TransportOrder.OrderStatus status = TransportOrder.OrderStatus.valueOf(newStatus);
            TransportOrder order = orderOpt.get();

            order.setStatus(status);
            order.setLastModifiedBy(adminEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            // Dodatkowe akcje przy zmianie statusu
            if (status == TransportOrder.OrderStatus.PICKED_UP) {
                order.setActualPickupTime(LocalDateTime.now());
            } else if (status == TransportOrder.OrderStatus.ON_THE_WAY_BACK) {
                order.setActualDeliveryTime(LocalDateTime.now());
            }

            transportOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "message", "Status zamówienia został zaktualizowany",
                    "newStatus", status.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy status: " + newStatus));
        }
    }

    // === HELPER METHODS ===

    @Override
    public int countOrdersForDate(LocalDate date) {
        return transportOrderRepository.countByPickupDate(date);
    }

    @Override
    public boolean areSlotsAvailable(LocalDate date, int ordersCount) {
        return serviceSlotService.areSlotsAvailable(date, ordersCount);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private List<IncompleteBike> validateAndGetBikes(List<Long> bicycleIds, Long userId) {
        List<IncompleteBike> bikes = new ArrayList<>();
        for (Long bicycleId : bicycleIds) {
            IncompleteBike bike = incompleteBikeRepository.findById(bicycleId)
                    .orElseThrow(() -> new RuntimeException("Bike not found: " + bicycleId));

            if (bike.getOwner() == null || !bike.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Brak uprawnień do roweru: " + bicycleId);
            }

            bikes.add(bike);
        }
        return bikes;
    }

    private IncompleteUser createOrFindIncompleteUser(String email, String phone) {
        Optional<IncompleteUser> existingUser = incompleteUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            IncompleteUser user = existingUser.get();
            if (phone != null) {
                user.setPhoneNumber(phone);
            }
            return incompleteUserRepository.save(user);
        } else {
            IncompleteUser newUser = new IncompleteUser();
            newUser.setEmail(email);
            newUser.setPhoneNumber(phone);
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
            if (bikeDto.additionalInfo() != null && !bikeDto.additionalInfo().isEmpty()) {
                bike.setType(bikeDto.additionalInfo());
            }
            bike.setOwner(owner);
            bike.setCreatedAt(LocalDateTime.now());

            bikes.add(incompleteBikeRepository.save(bike));
        }

        return bikes;
    }

    private void updateTransportOrderFields(TransportOrder order, TransportOrderDto dto) {
        if (dto.pickupDate() != null) {
            order.setPickupDate(dto.pickupDate());
        }
        if (dto.pickupAddress() != null) {
            order.setPickupAddress(dto.pickupAddress());
        }
        if (dto.pickupLatitude() != null) {
            order.setPickupLatitude(dto.pickupLatitude());
        }
        if (dto.pickupLongitude() != null) {
            order.setPickupLongitude(dto.pickupLongitude());
        }
        if (dto.pickupTimeFrom() != null) {
            order.setPickupTimeFrom(dto.pickupTimeFrom());
        }
        if (dto.pickupTimeTo() != null) {
            order.setPickupTimeTo(dto.pickupTimeTo());
        }
        if (dto.deliveryAddress() != null) {
            order.setDeliveryAddress(dto.deliveryAddress());
        }
        if (dto.deliveryLatitude() != null) {
            order.setDeliveryLatitude(dto.deliveryLatitude());
        }
        if (dto.deliveryLongitude() != null) {
            order.setDeliveryLongitude(dto.deliveryLongitude());
        }
        if (dto.transportPrice() != null) {
            order.setTransportPrice(dto.transportPrice());
        }
        if (dto.estimatedTime() != null) {
            order.setEstimatedTime(dto.estimatedTime());
        }
        if (dto.transportNotes() != null) {
            order.setTransportNotes(dto.transportNotes());
        }
        if (dto.additionalNotes() != null) {
            order.setAdditionalNotes(dto.additionalNotes());
        }
        if (dto.targetServiceId() != null) {
            BikeService targetService = bikeServiceRepository.findById(dto.targetServiceId())
                    .orElseThrow(() -> new RuntimeException("Target service not found"));

            // Walidacja - nie można zmienić na serwis własny w czystym transporcie
            if (targetService.getId().equals(1L)) {
                throw new RuntimeException("Nie można zmienić na serwis własny dla czystego transportu");
            }

            order.setTargetService(targetService);

            // Aktualizuj adres dostawy jeśli nie został podany
            if (dto.deliveryAddress() == null) {
                order.setDeliveryAddress(targetService.getFullAddress());
                order.setDeliveryLatitude(targetService.getLatitude());
                order.setDeliveryLongitude(targetService.getLongitude());
            }
        }

        // Aktualizuj rower - sprawdź czy należy do klienta
        if (dto.bicycleIds() != null && !dto.bicycleIds().isEmpty()) {
            Long bicycleId = dto.bicycleIds().get(0); // Weź pierwszy rower
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
            if (bikeOpt.isPresent()) {
                IncompleteBike bike = bikeOpt.get();
                if (bike.getOwner().getId().equals(order.getClient().getId())) {
                    order.setBicycle(bike);
                }
            }
        }
    }

    private int getAvailableSlots(LocalDate date) {
        int maxSlots = serviceSlotService.getMaxBikesPerDay(date);
        int usedSlots = countOrdersForDate(date);
        return Math.max(0, maxSlots - usedSlots);
    }

    /**
     * Waliduje czy można utworzyć zamówienie transportowe
     */
    private void validateTransportOrderCreation(TransportOrderDto dto) {
        // Dodatkowe walidacje specyficzne dla transportu
        if (dto.targetServiceId() == null) {
            throw new IllegalArgumentException("Target service ID jest wymagany");
        }

        // Sprawdź czy target service istnieje
        if (!bikeServiceRepository.existsById(dto.targetServiceId())) {
            throw new IllegalArgumentException("Serwis docelowy nie istnieje");
        }

        // Sprawdź czy to nie jest serwis własny
        if (dto.targetServiceId().equals(1L)) {
            throw new IllegalArgumentException("Dla serwisu własnego użyj endpoint /api/service-orders");
        }

        // Walidacja ceny transportu
        if (dto.transportPrice() == null || dto.transportPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cena transportu musi być większa lub równa 0");
        }
    }

    /**
     * Loguje utworzenie zamówienia transportowego
     */
    private void logTransportOrderCreation(TransportOrder order, String userEmail) {
        logger.info(String.format(
                "Transport order created: ID=%d, User=%s, Target=%s, Date=%s, Price=%s",
                order.getId(),
                userEmail,
                order.getTargetService().getName(),
                order.getPickupDate(),
                order.getTransportPrice()
        ));
    }

    /**
     * Waliduje przejście statusu dla zamówienia transportowego
     */
    private boolean isValidStatusTransition(TransportOrder.OrderStatus current, TransportOrder.OrderStatus target) {
        return switch (current) {
            case PENDING -> target == TransportOrder.OrderStatus.CONFIRMED || target == TransportOrder.OrderStatus.CANCELLED;
            case CONFIRMED -> target == TransportOrder.OrderStatus.PICKED_UP || target == TransportOrder.OrderStatus.CANCELLED;
            case PICKED_UP -> target == TransportOrder.OrderStatus.ON_THE_WAY_BACK || target == TransportOrder.OrderStatus.CANCELLED;
            case ON_THE_WAY_BACK -> target == TransportOrder.OrderStatus.CANCELLED; // Tylko anulowanie możliwe
            case CANCELLED -> false; // Nie można zmienić z anulowanego
            default -> false;
        };
    }

    /**
     * Sprawdza czy zamówienie może być modyfikowane przez użytkownika
     */
    private boolean canUserModifyOrder(TransportOrder order, String userEmail) {
        // Sprawdź własność
        if (!order.getClient().getEmail().equals(userEmail)) {
            return false;
        }

        // Sprawdź status
        return order.canBeCancelled();
    }

    /**
     * Pobiera szczegółowe informacje o dostępności slotów
     */
    public Map<String, Object> getDetailedSlotAvailability(LocalDate date) {
        int maxSlots = serviceSlotService.getMaxBikesPerDay(date);
        int usedSlots = countOrdersForDate(date);
        int availableSlots = Math.max(0, maxSlots - usedSlots);

        return Map.of(
                "date", date,
                "maxSlots", maxSlots,
                "usedSlots", usedSlots,
                "availableSlots", availableSlots,
                "isAvailable", availableSlots > 0,
                "utilizationPercentage", maxSlots > 0 ? (usedSlots * 100.0 / maxSlots) : 0
        );
    }

    // Dodaj te metody do TransportOrderServiceImpl

    @Override
    public List<UnifiedOrderResponseDto> getAllTransportOrdersAsUnified() {
        List<TransportOrder> orders = transportOrderRepository.findPureTransportOrders();
        return orders.stream()
                .map(UnifiedOrderResponseDto::fromTransportOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UnifiedOrderResponseDto> getOrderAsUnified(Long orderId) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }

        TransportOrder order = orderOpt.get();

        // Sprawdź czy to nie jest ServiceOrder - jeśli tak, zwróć empty
        if (order instanceof ServiceOrder) {
            return Optional.empty();
        }

        return Optional.of(UnifiedOrderResponseDto.fromTransportOrder(order));
    }
}