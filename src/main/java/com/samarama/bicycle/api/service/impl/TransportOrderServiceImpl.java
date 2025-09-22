package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final IndividualUserRepository individualUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServiceSlotService serviceSlotService;
    private final CouponRepository couponRepository;
    private final BicycleEnumerationRepository bicycleEnumerationRepository;

    public TransportOrderServiceImpl(
            TransportOrderRepository transportOrderRepository,
            UserRepository userRepository,
            IndividualUserRepository individualUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            BikeServiceRepository bikeServiceRepository,
            ServiceSlotService serviceSlotService, CouponRepository couponRepository, BicycleEnumerationRepository bicycleEnumerationRepository){
        this.transportOrderRepository = transportOrderRepository;
        this.userRepository = userRepository;
        this.individualUserRepository = individualUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.serviceSlotService = serviceSlotService;
        this.couponRepository = couponRepository;
        this.bicycleEnumerationRepository = bicycleEnumerationRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createTransportOrder(ServiceOrTransportOrderDto dto, String userEmail) {
        // Pobierz użytkownika
        IndividualUser user = individualUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pobierz serwis docelowy
        BikeService targetService = bikeServiceRepository.findById(dto.getTargetServiceId())
                .orElseThrow(() -> new RuntimeException("Target service not found"));

        // Walidacja rowerów
        List<IncompleteBike> bikes = validateAndGetBikes(dto.getBicycleIds(), user.getId());

        // Utwórz zamówienia transportowe (jedno na rower)
        List<TransportOrder> orders = createTransportOrderFromDtos(bikes, user, dto, targetService);

        // Zapisz zamówienia
        List<TransportOrder> savedOrders = transportOrderRepository.saveAll(orders);

        // Wyślij powiadomienia email
        for (TransportOrder order : savedOrders) {
            try {
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
    public ResponseEntity<?> createGuestTransportOrder(ServiceOrTransportOrderDto dto) {
        try {
            logger.info("Creating guest transport order for email: " + dto.getEmail());

            // Sprawdź dostępność slotów
            int bikesCount = dto.getBicycles().size();
            if (!serviceSlotService.areSlotsAvailable(dto.getPickupDate(), bikesCount)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Brak dostępnych miejsc na wybrany dzień",
                        "availableSlots", getAvailableSlots(dto.getPickupDate())
                ));
            }

            // Pobierz serwis docelowy
            BikeService targetService = bikeServiceRepository.findById(dto.getTargetServiceId())
                    .orElseThrow(() -> new RuntimeException("Target service not found: " + dto.getTargetServiceId()));

            // Utwórz lub znajdź użytkownika tymczasowego
            IndividualUser guestUser = createOrFindIncompleteUser(dto.getEmail(), dto.getPhone());

            // Utwórz rowery gościa
            List<IncompleteBike> bikes = createIncompleteBikes(dto.getBicycles(), guestUser);

            // Utwórz zamówienia transportowe
            List<TransportOrder> orders = createTransportOrderFromDtos(bikes, guestUser, dto, targetService);

            // Zapisz zamówienia
            List<TransportOrder> savedOrders = transportOrderRepository.saveAll(orders);

            // Log sukcesu
            savedOrders.forEach(order -> logTransportOrderCreation(order, dto.getEmail()));

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienia transportowe zostały utworzone pomyślnie",
                    "orderIds", savedOrders.stream().map(TransportOrder::getId).collect(Collectors.toList()),
                    "orderCount", savedOrders.size(),
                    "guestUserId", guestUser.getId(),
                    "targetService", targetService.getName(),
                    "orderType", "TRANSPORT"
            ));

        } catch (Exception e) {
            logger.severe("Error creating guest transport order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    public List<ServiceOrderResponseDto> getAllUserOrders(String userEmail) {
        IndividualUser user = getUserByEmail(userEmail);
        List<TransportOrder> orders = transportOrderRepository.findByClient(user);

        return orders.stream()
                .map(order -> {
                    if (order instanceof ServiceOrder) {
                        return ServiceOrderResponseDto.fromServiceOrder((ServiceOrder) order);
                    } else {
                        return ServiceOrderResponseDto.fromTransportOrder(order);
                    }
                })
                .sorted(Comparator.comparing(ServiceOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<ServiceOrderDetailsResponseDto> getOrderDetails(Long orderId, String userEmail) {
        IndividualUser user = getUserByEmail(userEmail);

        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        // Sprawdź własność
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        ServiceOrderDetailsResponseDto dto = order instanceof ServiceOrder ?
                ServiceOrderDetailsResponseDto.fromServiceOrder((ServiceOrder) order) :
                ServiceOrderDetailsResponseDto.fromTransportOrder(order);

        return ResponseEntity.ok(dto);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateTransportOrder(Long orderId, ServiceOrTransportOrderDto dto) {
        IndividualUser user = getUserByEmail(dto.getEmail());

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

        // Sprawdź czy to nie jest ServiceOrder
        if (order instanceof ServiceOrder) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Użyj endpointu /api/service-orders/{id} dla zamówień serwisowych"
            ));
        }

        // Aktualizuj pola
        updateTransportOrderFields(order, dto);
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
        IndividualUser user = getUserByEmail(userEmail);

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

    // W TransportOrderServiceImpl sprawdź czy metoda updateOrderStatus wygląda tak:

    @Override
    @Transactional
    public ResponseEntity<?> updateOrderStatus(Long orderId, String newStatus, String userEmail) {
        try {
            // WAŻNE: Sprawdź najpierw w ServiceOrder (dziedziczą po TransportOrder)
            Optional<TransportOrder> transportOrderOpt = transportOrderRepository.findById(orderId);
            if (transportOrderOpt.isPresent()) {
                TransportOrder transportOrder = transportOrderOpt.get();

                // Walidacja statusu
                TransportOrder.OrderStatus targetStatus;
                try {
                    targetStatus = TransportOrder.OrderStatus.valueOf(newStatus);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "message", "Nieprawidłowy status: " + newStatus
                    ));
                }

                // Aktualizuj status
                transportOrder.setStatus(targetStatus);
                transportOrder.setLastModifiedBy(userEmail);
                transportOrder.setLastModifiedDate(LocalDateTime.now());

                transportOrderRepository.save(transportOrder);

                return ResponseEntity.ok(Map.of(
                        "message", "Status zamówienia serwisowego został zaktualizowany",
                        "orderId", orderId,
                        "newStatus", newStatus
                ));
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.severe("Error updating order status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Błąd podczas aktualizacji statusu: " + e.getMessage()
            ));
        }
    }

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
    public ResponseEntity<?> updateTransportOrderByAdmin(Long orderId, ServiceOrTransportOrderDto dto, String adminEmail) {
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

    @Override
    public int countOrdersForDate(LocalDate date) {
        return transportOrderRepository.countByPickupDate(date);
    }

    @Override
    public boolean areSlotsAvailable(LocalDate date, int ordersCount) {
        return serviceSlotService.areSlotsAvailable(date, ordersCount);
    }

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

    @Override
    public List<CourierOrderDto> getCourierOrders() {
        logger.info("Fetching courier orders for today and ON_THE_WAY_BACK status");

        try {
            LocalDate today = LocalDate.now();

            // Pobierz zamówienia CONFIRMED z dzisiejszą datą odbioru
            List<TransportOrder> confirmedToday = transportOrderRepository
                    .findByStatusAndPickupDate(TransportOrder.OrderStatus.CONFIRMED, today);

            // Pobierz wszystkie zamówienia ON_THE_WAY_BACK
            List<TransportOrder> onTheWayBack = transportOrderRepository
                    .findByStatus(TransportOrder.OrderStatus.ON_THE_WAY_BACK);

            // Połącz listy i przekształć na DTO
            List<CourierOrderDto> result = confirmedToday.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            result.addAll(onTheWayBack.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList()));

            logger.info("Found " + result.size() + " orders for courier (" +
                    confirmedToday.size() + " confirmed today, " +
                    onTheWayBack.size() + " on the way back)");

            return result;

        } catch (Exception e) {
            logger.severe("Error fetching courier orders: " + e.getMessage());
            throw new RuntimeException("Błąd podczas pobierania zamówień kuriera", e);
        }
    }

    @Transactional
    @Override
    public BigDecimal checkDiscount(String couponCode, BigDecimal currentPrice, LocalDate orderDate) {

        // Krok 1: Oczyszczenie kodu z białych znaków na początku i końcu, aby być bardziej "łaskawym".
        String sanitizedCode = couponCode.trim();

        // Krok 2: Wyszukanie kuponu w bazie danych z ignorowaniem wielkości liter.
        Optional<Coupon> couponOptional = couponRepository.findByCouponCodeIgnoreCase(sanitizedCode);

        // Jeśli kupon o podanym kodzie nie istnieje, zwróć oryginalną cenę.
        if (couponOptional.isEmpty()) {
            return currentPrice;
        }



        Coupon coupon = couponOptional.get();

        // Krok 3: Sprawdzenie daty ważności.
        boolean isDateValid = !orderDate.isAfter(coupon.getExpirationDate());

        if (isDateValid) {
            coupon.setUsageCount(coupon.getUsageCount() + 1);
            // Kupon jest poprawny i aktywny, zwróć cenę zniżkową przypisaną do kuponu.
            return currentPrice
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP) // precyzyjne dzielenie
                    .multiply(BigDecimal.valueOf(100).subtract(coupon.getPriceAfterDiscount()))       // mnożenie
                    .setScale(0, RoundingMode.HALF_UP);             // zaokrąglenie do pełnej liczby
        } else {
            // Kupon istnieje, ale jest przeterminowany, zwróć oryginalną cenę.
            return currentPrice;
        }
    }

    @Override
    public List<TransportOrderDto> getOrdersByIds(List<Long> orderIds) {
        return transportOrderRepository.findAllById(orderIds)
                .stream()
                .map(this::mapToTransportDto)
                .toList();
    }

    // === PRIVATE HELPER METHODS ===

    private IndividualUser getUserByEmail(String email) {
        return individualUserRepository.findByEmail(email)
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

    private IndividualUser createOrFindIncompleteUser(String email, String phone) {
        Optional<IndividualUser> existingUser = individualUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            IndividualUser user = existingUser.get();
            if (phone != null) {
                user.setPhoneNumber(phone);
            }
            return individualUserRepository.save(user);
        } else {
            IndividualUser newUser = new IndividualUser();
            newUser.setEmail(email);
            newUser.setPhoneNumber(phone);
            newUser.setCreatedAt(LocalDateTime.now());
            return individualUserRepository.save(newUser);
        }
    }

    private List<IncompleteBike> createIncompleteBikes(List<GuestBicycleDto> bicycleDtos, IndividualUser owner) {
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

    private List<TransportOrder> createTransportOrderFromDtos(
            List<IncompleteBike> bikes, IndividualUser user, ServiceOrTransportOrderDto dto,
            BikeService targetService) {

        List<TransportOrder> orders = new ArrayList<>();

        for (IncompleteBike bike : bikes) {
            TransportOrder order = new TransportOrder();

            order.setBicycle(bike);
            order.setClient(user);
            order.setPickupDate(dto.getPickupDate());

            // Ustaw rozbity adres odbioru
            order.setPickupStreet(dto.getPickupStreet());
            order.setPickupBuilding(dto.getPickupBuildingNumber());
            order.setPickupApartment(dto.getPickupApartmentNumber());
            order.setPickupCity(dto.getPickupCity());
            order.setPickupPostalCode(dto.getPickupPostalCode());
            order.setPickupLatitude(dto.getPickupLatitude());
            order.setPickupLongitude(dto.getPickupLongitude());

            order.setTargetService(targetService);

            // Ustaw rozbity adres dostawy z serwisu
            order.setDeliveryStreet(targetService.getStreet());
            order.setDeliveryBuilding(targetService.getBuilding());
            order.setDeliveryApartment(targetService.getFlat());
            order.setDeliveryCity(targetService.getCity());
            order.setDeliveryPostalCode(targetService.getPostalCode());
            order.setDeliveryLatitude(targetService.getLatitude());
            order.setDeliveryLongitude(targetService.getLongitude());

            order.setTransportPrice(calculateTransportPrice(dto.getBicycles().size(), dto.getTransportPrice()));
            order.setTransportNotes(dto.getTransportNotes());
            order.setAdditionalNotes(dto.getAdditionalNotes());
            order.setStatus(TransportOrder.OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            orders.add(order);
        }

        return orders;
    }

    private BigDecimal calculateTransportPrice(int numberOfBicycles, BigDecimal transportPrice) {
        if (numberOfBicycles <= 0) {
            throw new IllegalArgumentException("Liczba rowerów musi być większa niż 0.");
        }
        return transportPrice
                .multiply(BigDecimal.valueOf(2))
                .divide(BigDecimal.valueOf(numberOfBicycles), 0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2));
    }



    private void updateTransportOrderFields(TransportOrder order, ServiceOrTransportOrderDto dto) {
        if (dto.getPickupDate() != null) {
            order.setPickupDate(dto.getPickupDate());
        }

        // Aktualizuj rozbity adres odbioru
        if (dto.getPickupStreet() != null) {
            order.setPickupStreet(dto.getPickupStreet());
        }
        if (dto.getPickupBuildingNumber() != null) {
            order.setPickupBuilding(dto.getPickupBuildingNumber());
        }
        if (dto.getPickupApartmentNumber() != null) {
            order.setPickupApartment(dto.getPickupApartmentNumber());
        }
        if (dto.getBicycles() != null){
            order.getBicycle().setBrand(dto.getBicycles().getFirst().brand());
            order.getBicycle().setModel(dto.getBicycles().getFirst().model());
        }
        if (dto.getPickupCity() != null) {
            order.setPickupCity(dto.getPickupCity());
        }
        if (dto.getPickupPostalCode() != null) {
            order.setPickupPostalCode(dto.getPickupPostalCode());
        }
        if (dto.getPickupLatitude() != null) {
            order.setPickupLatitude(dto.getPickupLatitude());
        }
        if (dto.getPickupLongitude() != null) {
            order.setPickupLongitude(dto.getPickupLongitude());
        }

        if (dto.getTransportPrice() != null) {
            order.setTransportPrice(dto.getTransportPrice());
        }
        if (dto.getTransportNotes() != null) {
            order.setTransportNotes(dto.getTransportNotes());
        }
        if (dto.getAdditionalNotes() != null) {
            order.setAdditionalNotes(dto.getAdditionalNotes());
        }

        if (dto.getTargetServiceId() != null) {
            BikeService targetService = bikeServiceRepository.findById(dto.getTargetServiceId())
                    .orElseThrow(() -> new RuntimeException("Target service not found"));

            // Walidacja - nie można zmienić na serwis własny w czystym transporcie
            if (targetService.getId().equals(1L)) {
                throw new RuntimeException("Nie można zmienić na serwis własny dla czystego transportu");
            }

            order.setTargetService(targetService);

            // Aktualizuj rozbity adres dostawy z serwisu
            order.setDeliveryStreet(targetService.getStreet());
            order.setDeliveryBuilding(targetService.getBuilding());
            order.setDeliveryApartment(targetService.getFlat());
            order.setDeliveryCity(targetService.getCity());
            order.setDeliveryPostalCode(targetService.getPostalCode());
            order.setDeliveryLatitude(targetService.getLatitude());
            order.setDeliveryLongitude(targetService.getLongitude());
        }

        // Aktualizuj rower - sprawdź czy należy do klienta
        if (dto.getBicycleIds() != null && !dto.getBicycleIds().isEmpty()) {
            Long bicycleId = dto.getBicycleIds().get(0); // Weź pierwszy rower
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

    private CourierOrderDto mapToDto(TransportOrder order) {
        CourierOrderDto dto = new CourierOrderDto();

        dto.setId(order.getId());
        dto.setStatus(order.getStatus().toString());
        dto.setOrderDate(order.getOrderDate().toString());
        dto.setPickupDate(order.getPickupDate().toString());

        // Ustaw okno czasowe odbioru jeśli istnieje
        if (order.hasPickupTimeWindow()) {
            dto.setPickupTimeWindow(order.getPickupTimeWindow());
        }

        // Dla zamówień ON_THE_WAY_BACK zamień miejscami adresy
        if (order.getStatus() == TransportOrder.OrderStatus.ON_THE_WAY_BACK) {
            // W drodze powrotnej: deliveryAddress to teraz pickup, pickupAddress to delivery
            dto.setPickupAddress(order.getFullDeliveryAddress());
            dto.setDeliveryAddress(order.getFullPickupAddress());
        } else {
            // Normalnie: pickup to pickup, delivery to delivery
            dto.setPickupAddress(order.getFullPickupAddress());
            dto.setDeliveryAddress(order.getFullDeliveryAddress());
        }

        // Informacje o rowerze
        if (order.getBicycle() != null) {
            dto.setBikeBrand(order.getBicycle().getBrand());
            dto.setBikeModel(order.getBicycle().getModel());
        }

        // Informacje o kliencie
        if (order.getClient() != null) {
            dto.setClientEmail(order.getClient().getEmail());
            dto.setClientPhone(order.getClient().getPhoneNumber());
        }

        return dto;
    }

    private TransportOrderDto mapToTransportDto(TransportOrder entity) {
        // ... (przykładowa implementacja mapowania)
        return new TransportOrderDto(
                entity.getId(),
                entity.getBicycle(),
                entity.getPickupDate(),
                entity.getFullPickupAddress(),
                entity.getPickupLatitude(),
                entity.getPickupLongitude(),
                entity.getPickupTimeFrom(),
                entity.getPickupTimeTo(),
                entity.getFullDeliveryAddress(),
                entity.getDeliveryLatitude(),
                entity.getDeliveryLongitude(),
                entity.getTargetService().getId(),
                entity.getTransportPrice(),
                entity.getEstimatedTime(),
                entity.getTransportNotes(),
                entity.getAdditionalNotes(),
                entity.getClient().getEmail(),
                entity.getClient().getPhoneNumber()
        );
    }
}