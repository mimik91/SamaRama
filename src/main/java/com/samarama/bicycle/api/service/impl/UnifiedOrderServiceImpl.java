package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.*;
import com.samarama.bicycle.api.service.helper.OrderValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class UnifiedOrderServiceImpl implements UnifiedOrderService {

    private static final Logger logger = Logger.getLogger(UnifiedOrderServiceImpl.class.getName());

    private final TransportOrderService transportOrderService;
    private final ServiceOrderService serviceOrderService;
    private final BikeServiceService bikeServiceService;
    private final ServicePackageService servicePackageService;
    private final AddressService addressService;
    private final UserRepository userRepository;
    private final OrderValidator orderValidator;

    public UnifiedOrderServiceImpl(
            TransportOrderService transportOrderService,
            ServiceOrderService serviceOrderService,
            BikeServiceService bikeServiceService,
            ServicePackageService servicePackageService,
            AddressService addressService,
            UserRepository userRepository,
            OrderValidator orderValidator) {
        this.transportOrderService = transportOrderService;
        this.serviceOrderService = serviceOrderService;
        this.bikeServiceService = bikeServiceService;
        this.servicePackageService = servicePackageService;
        this.addressService = addressService;
        this.userRepository = userRepository;
        this.orderValidator = orderValidator;
    }

    // === PUBLICZNE METODY ===

    @Override
    public ResponseEntity<List<BikeServicePinDto>> getAvailableServices() {
        List<BikeServicePinDto> services = bikeServiceService.getAllBikeServicePins();
        return ResponseEntity.ok(services);
    }

    @Override
    public ResponseEntity<List<ServicePackageDto>> getServicePackages() {
        List<ServicePackageDto> packages = servicePackageService.getActiveServicePackages().stream()
                .map(ServicePackageDto::fromEntity)
                .toList();
        return ResponseEntity.ok(packages);
    }

    @Override
    public ResponseEntity<BikeServiceDto> getServiceDetails(Long serviceId) {
        return bikeServiceService.getBikeServiceDetails(serviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<?> checkAvailability(String date, int bikesCount) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            boolean available = transportOrderService.areSlotsAvailable(localDate, bikesCount);
            int usedSlots = transportOrderService.countOrdersForDate(localDate);

            return ResponseEntity.ok(Map.of(
                    "date", date,
                    "available", available,
                    "usedSlots", usedSlots,
                    "requestedBikes", bikesCount,
                    "pickupTimeWindow", "18:00 - 22:00"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowa data: " + date));
        }
    }

    @Override
    public ResponseEntity<?> calculateTransportCost(Map<String, Object> request) {
        try {
            Integer bicycleCount = (Integer) request.get("bicycleCount");
            Long targetServiceId = request.get("targetServiceId") != null ?
                    ((Number) request.get("targetServiceId")).longValue() : null;

            if (bicycleCount == null || bicycleCount <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Liczba rowerów jest wymagana i musi być większa od 0"
                ));
            }

            // Kalkulacja kosztów transportu
            BigDecimal baseCost = new BigDecimal("30.00");
            BigDecimal perBikeCost = new BigDecimal("15.00");

            BigDecimal totalCost = baseCost;

            // Koszt za dodatkowe rowery
            if (bicycleCount > 1) {
                totalCost = totalCost.add(perBikeCost.multiply(new BigDecimal(bicycleCount - 1)));
            }

            // Rabat dla serwisu własnego
            String discountInfo = "0%";
            if (targetServiceId != null && targetServiceId.equals(1L)) {
                totalCost = totalCost.multiply(new BigDecimal("0.9")); // 10% rabatu
                discountInfo = "10%";
            }

            return ResponseEntity.ok(Map.of(
                    "transportCost", totalCost,
                    "breakdown", Map.of(
                            "baseCost", baseCost,
                            "additionalBikes", bicycleCount > 1 ? perBikeCost.multiply(new BigDecimal(bicycleCount - 1)) : BigDecimal.ZERO,
                            "discount", discountInfo
                    ),
                    "currency", "PLN",
                    "description", targetServiceId != null && targetServiceId.equals(1L) ?
                            "Koszt transportu do naszego serwisu (z rabatem)" :
                            "Koszt transportu do zewnętrznego serwisu"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Błąd podczas kalkulacji kosztów: " + e.getMessage()
            ));
        }
    }

    @Override
    public ResponseEntity<?> getOrderInfo() {
        return ResponseEntity.ok(Map.of(
                "orderTypes", Map.of(
                        "TRANSPORT", "Transport do zewnętrznego serwisu",
                        "SERVICE", "Transport + serwis w naszym serwisie"
                ),
                "statuses", List.of(
                        Map.of("value", "PENDING", "label", "Oczekujące"),
                        Map.of("value", "CONFIRMED", "label", "Potwierdzone"),
                        Map.of("value", "PICKED_UP", "label", "Odebrane"),
                        Map.of("value", "IN_SERVICE", "label", "W serwisie"),
                        Map.of("value", "ON_THE_WAY_BACK", "label", "W drodze powrotnej"),
                        Map.of("value", "CANCELLED", "label", "Anulowane")
                ),
                "pickupTime", Map.of(
                        "from", "18:00",
                        "to", "22:00",
                        "description", "Stały czas odbioru między 18:00 a 22:00"
                ),
                "modification", Map.of(
                        "canModifyStatuses", List.of("PENDING", "CONFIRMED"),
                        "cannotModifyStatuses", List.of("PICKED_UP", "IN_SERVICE", "ON_THE_WAY_BACK", "CANCELLED"),
                        "message", "Zamówienia można modyfikować tylko w statusie PENDING lub CONFIRMED"
                ),
                "serviceTypes", Map.of(
                        "transport", Map.of(
                                "description", "Tylko transport do zewnętrznego serwisu",
                                "targetService", "Nie może być serwis własny (ID=1)"
                        ),
                        "service", Map.of(
                                "description", "Transport + serwis w naszym serwisie",
                                "targetService", "Zawsze serwis własny (ID=1)",
                                "requiresPackage", true
                        )
                )
        ));
    }

    // === METODY DLA UŻYTKOWNIKÓW ===

    @Override
    public ResponseEntity<List<AddressDto>> getUserAddresses(String userEmail) {
        try {
            Long userId = getUserId(userEmail);
            List<AddressDto> addresses = addressService.getUserAddresses(userId);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            logger.severe("Error getting user addresses: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<UnifiedOrderResponseDto>> getUserOrders(String userEmail) {
        try {
            List<UnifiedOrderResponseDto> orders = transportOrderService.getAllUserOrders(userEmail);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.severe("Error getting user orders: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<UnifiedOrderResponseDto> getUserOrderDetails(Long orderId, String userEmail) {
        try {
            // Próbuj najpierw jako transport order
            ResponseEntity<UnifiedOrderResponseDto> transportResponse =
                    transportOrderService.getOrderDetails(orderId, userEmail);

            if (transportResponse.getStatusCode().is2xxSuccessful()) {
                return transportResponse;
            }

            // Jeśli nie znaleziono, spróbuj jako service order
            return serviceOrderService.getServiceOrderDetails(orderId, userEmail);
        } catch (Exception e) {
            logger.severe("Error getting order details: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<?> createUserOrder(ServiceOrTransportOrderDto dto, String userEmail) {
        try {
            Long userId = getUserId(userEmail);

            // Ustaw userId w DTO
            ServiceOrTransportOrderDto dtoWithUserId = new ServiceOrTransportOrderDto(
                    dto.getBicycleIds(), dto.getBicycles(), userId, dto.getEmail(), dto.getPhone(),
                    dto.getPickupAddressId(), dto.getPickupStreet(), dto.getPickupBuildingNumber(),
                    dto.getPickupApartmentNumber(), dto.getPickupCity(), dto.getPickupPostalCode(),
                    dto.getPickupLatitude(), dto.getPickupLongitude(), dto.getPickupDate(),
                    dto.getTransportPrice(), dto.getTransportNotes(), dto.getTargetServiceId(),
                    dto.getServicePackageId(), dto.getServiceNotes(), dto.getAdditionalNotes()
            );


            // Walidacja
            OrderValidator.ValidationResult validation = orderValidator.validateUserOrder(dtoWithUserId);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", validation.getFirstError(),
                        "errors", validation.getErrors()
                ));
            }

            // Przekieruj do odpowiedniego serwisu
            if (dtoWithUserId.isServiceOrder()) {
                return serviceOrderService.createServiceOrder(dtoWithUserId, userEmail);
            } else {
                // Konwertuj do TransportOrderDto dla backward compatibility
                return createTransportOrderFromDto(dtoWithUserId, userEmail);
            }

        } catch (Exception e) {
            logger.severe("Error creating user order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas tworzenia zamówienia"));
        }
    }

    @Override
    public ResponseEntity<?> cancelUserOrder(Long orderId, String userEmail) {
        try {
            return transportOrderService.cancelOrder(orderId, userEmail);
        } catch (Exception e) {
            logger.severe("Error cancelling order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas anulowania zamówienia"));
        }
    }

    @Override
    public ResponseEntity<?> getUserStatistics(String userEmail) {
        try {
            List<UnifiedOrderResponseDto> orders = transportOrderService.getAllUserOrders(userEmail);

            long totalOrders = orders.size();
            long transportOrders = orders.stream().filter(o -> "TRANSPORT".equals(o.orderType())).count();
            long serviceOrders = orders.stream().filter(o -> "SERVICE".equals(o.orderType())).count();
            long pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.status())).count();
            long completedOrders = orders.stream().filter(o -> "ON_THE_WAY_BACK".equals(o.status())).count();
            long cancelledOrders = orders.stream().filter(o -> "CANCELLED".equals(o.status())).count();

            return ResponseEntity.ok(Map.of(
                    "totalOrders", totalOrders,
                    "transportOrders", transportOrders,
                    "serviceOrders", serviceOrders,
                    "pendingOrders", pendingOrders,
                    "completedOrders", completedOrders,
                    "cancelledOrders", cancelledOrders,
                    "activeOrders", totalOrders - completedOrders - cancelledOrders
            ));
        } catch (Exception e) {
            logger.severe("Error getting user statistics: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // === METODY DLA GOŚCI ===

    @Override
    public ResponseEntity<?> createGuestOrder(ServiceOrTransportOrderDto dto) {
        try {
            // Walidacja
            OrderValidator.ValidationResult validation = orderValidator.validateGuestOrder(dto);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", validation.getFirstError(),
                        "errors", validation.getErrors()
                ));
            }

            // Goście mogą składać tylko zamówienia serwisowe
            return serviceOrderService.createGuestServiceOrder(dto);
        } catch (Exception e) {
            logger.severe("Error creating guest order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas tworzenia zamówienia gościa"));
        }
    }

    // === METODY ADMINISTRACYJNE ===

    @Override
    public ResponseEntity<List<UnifiedOrderResponseDto>> getAllOrdersForAdmin() {
        try {
            List<UnifiedOrderResponseDto> orders = transportOrderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.severe("Error getting all orders for admin: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<UnifiedOrderResponseDto>> searchOrders(String searchTerm) {
        try {
            List<UnifiedOrderResponseDto> orders = transportOrderService.searchOrders(searchTerm);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.severe("Error searching orders: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<?> updateOrderStatus(Long orderId, String newStatus, String adminEmail) {
        try {
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
            }

            return transportOrderService.updateOrderStatusByAdmin(orderId, newStatus, adminEmail);
        } catch (Exception e) {
            logger.severe("Error updating order status: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas aktualizacji statusu"));
        }
    }

    @Override
    public ResponseEntity<?> updateServiceNotes(Long orderId, String notes, String userEmail) {
        try {
            return serviceOrderService.updateServiceNotes(orderId, notes, userEmail);
        } catch (Exception e) {
            logger.severe("Error updating service notes: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas aktualizacji notatek"));
        }
    }

    // === METODY POMOCNICZE ===

    private Long getUserId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        return user.getId();
    }

    /**
     * Konwertuje ServiceOrTransportOrderDto do TransportOrderDto dla backward compatibility
     */
    private ResponseEntity<?> createTransportOrderFromDto(ServiceOrTransportOrderDto dto, String userEmail) {
        TransportOrderDto transportDto = new TransportOrderDto(
                dto.getBicycleIds(),
                dto.getBicycles(),
                dto.getPickupDate(),
                dto.getPickupAddressString(),
                dto.getPickupLatitude(),
                dto.getPickupLongitude(),
                null, // pickupTimeFrom - stałe 18:00
                null, // pickupTimeTo - stałe 22:00
                null, // deliveryAddress - będzie ustawiony automatycznie
                null, // deliveryLatitude
                null, // deliveryLongitude
                dto.getTargetServiceId(),
                dto.getTransportPrice(),
                60, // estimatedTime
                dto.getTransportNotes(),
                dto.getAdditionalNotes(),
                dto.getEmail(),
                dto.getPhone(),
                null, // clientName - nie używamy
                dto.getPickupCity()
        );

        return transportOrderService.createTransportOrder(transportDto, userEmail);
    }
}