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

            dto.setUserId(userId);

            // Walidacja
            OrderValidator.ValidationResult validation = orderValidator.validateUserOrder(dto);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", validation.getFirstError(),
                        "errors", validation.getErrors()
                ));
            }

            // Przekieruj do odpowiedniego serwisu
            if (dto.isServiceOrder()) {
                return serviceOrderService.createServiceOrder(dto, userEmail);
            } else {
                // Konwertuj do TransportOrderDto dla backward compatibility
                return transportOrderService.createTransportOrder(dto, userEmail);
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

    // === METODY DLA GOŚCI ===

    @Override
    public ResponseEntity<?> createGuestOrder(ServiceOrTransportOrderDto dto) {

        // Ustaw serwis własny jeśli nie podano
        if (dto.getTargetServiceId() == null) {
            dto.setTargetServiceId(2137L); // ID serwisu własnego z application.properties
        }

        // Dla zamówień serwisowych transport jest wliczony w cenę pakietu
        if (dto.getTransportPrice() == null) {
            dto.setTransportPrice(BigDecimal.ZERO);
        }
        try {
            // Sprawdź typ zamówienia na podstawie servicePackageId
            boolean isServiceOrder = dto.getServicePackageId() != null;
            String orderType = isServiceOrder ? "SERVICE" : "TRANSPORT";

            logger.info("Creating guest " + orderType + " order for email: " + dto.getEmail());

            OrderValidator.ValidationResult validation = orderValidator.validateGuestOrder(dto);
            if (!validation.isValid()) {
                logger.warning("Guest order validation failed: " + validation.getAllErrors());
                return ResponseEntity.badRequest().body(Map.of(
                        "message", validation.getFirstError(),
                        "errors", validation.getErrors()
                ));
            }

            // DELEGACJA do odpowiedniego serwisu
            if (isServiceOrder) {
                logger.info("Delegating to ServiceOrderService for guest service order");
                return serviceOrderService.createGuestServiceOrder(dto);
            } else {
                logger.info("Delegating to TransportOrderService for guest transport order");
                return transportOrderService.createGuestTransportOrder(dto);
            }

        } catch (Exception e) {
            logger.severe("Error creating guest order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Błąd podczas tworzenia zamówienia: " + e.getMessage()
            ));
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
}