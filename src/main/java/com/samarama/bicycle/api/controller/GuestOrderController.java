package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.GuestOrderService;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/guest-orders")
public class GuestOrderController {

    private static final Logger logger = Logger.getLogger(GuestOrderController.class.getName());

    private final GuestOrderService guestOrderService;
    private final TransportOrderService transportOrderService;
    private final EmailService emailService;

    public GuestOrderController(
            GuestOrderService guestOrderService,
            TransportOrderService transportOrderService,
            EmailService emailService) {
        this.guestOrderService = guestOrderService;
        this.transportOrderService = transportOrderService;
        this.emailService = emailService;
    }

    /**
     * Tworzy zamówienie serwisowe dla gości (transport + serwis)
     */
    @PostMapping("/service")
    public ResponseEntity<?> createGuestServiceOrder(@Valid @RequestBody GuestServiceOrderDto dto) {
        logger.info("Received guest service order for email: " + dto.email());
        try {
            return guestOrderService.processGuestOrder(dto);
        } catch (Exception e) {
            logger.severe("Error creating guest service order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Wystąpił błąd podczas przetwarzania zamówienia: " + e.getMessage()
            ));
        }
    }

    /**
     * Tworzy zamówienie transportowe dla gości (TYLKO transport)
     */
    @PostMapping("/transport")
    public ResponseEntity<?> createGuestTransportOrder(@Valid @RequestBody TransportOrderDto dto) {
        return transportOrderService.createGuestTransportOrder(dto);
    }

    /**
     * Oblicza koszt transportu
     */
    @PostMapping("/calculate-transport-cost")
    public ResponseEntity<?> calculateTransportCost(@RequestBody Map<String, Object> request) {
        try {
            Integer bicycleCount = (Integer) request.get("bicycleCount");
            Double distance = request.get("distance") != null ?
                    ((Number) request.get("distance")).doubleValue() : null;
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
            BigDecimal perKmCost = new BigDecimal("2.50");

            BigDecimal totalCost = baseCost;

            // Koszt za dodatkowe rowery
            if (bicycleCount > 1) {
                totalCost = totalCost.add(perBikeCost.multiply(new BigDecimal(bicycleCount - 1)));
            }

            // Koszt za dystans
            if (distance != null && distance > 0) {
                totalCost = totalCost.add(perKmCost.multiply(new BigDecimal(distance)));
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
                            "distanceCost", distance != null ? perKmCost.multiply(new BigDecimal(distance)) : BigDecimal.ZERO,
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

    /**
     * Sprawdza dostępność slotów dla gości
     */
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam String date,
            @RequestParam(defaultValue = "1") int ordersCount) {
        try {
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            boolean available = transportOrderService.areSlotsAvailable(localDate, ordersCount);
            int usedSlots = transportOrderService.countOrdersForDate(localDate);

            return ResponseEntity.ok(Map.of(
                    "date", date,
                    "available", available,
                    "usedSlots", usedSlots,
                    "requestedOrders", ordersCount,
                    "message", available ? "Sloty dostępne" : "Brak dostępnych slotów"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowa data: " + date));
        }
    }

    /**
     * Rejestracja serwisu (legacy endpoint)
     */
    @PostMapping("/service-registration")
    public ResponseEntity<?> serviceRegistration(@RequestBody List<String> serviceData) {
        logger.info("Received service registration: " + serviceData);

        if (serviceData == null || serviceData.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Brak danych rejestracyjnych"));
        }

        // Pobierz pierwszy element z listy
        String data = serviceData.get(0);

        // Parsowanie danych w formacie K:[contactPerson]|T:[phoneNumber]|E:[email]|S:[serviceName]
        ServiceRegisterDto dto = parseServiceData(data);

        // Wysłanie emaila z danymi rejestracyjnymi
        try {
            emailService.sendServiceRegistrationNotification(dto);

            return ResponseEntity.ok(Map.of("message", "Zgłoszenie zostało przyjęte pomyślnie"));
        } catch (Exception e) {
            logger.severe("Error processing service registration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Wystąpił błąd podczas przetwarzania zgłoszenia"));
        }
    }

    // === HELPER METHODS ===

    private ServiceRegisterDto parseServiceData(String data) {
        ServiceRegisterDto dto = new ServiceRegisterDto();

        // Format: K:[contactPerson]|T:[phoneNumber]|E:[email]|S:[serviceName]
        String[] parts = data.split("\\|");

        for (String part : parts) {
            if (part.startsWith("K:")) {
                dto.setName(part.substring(2));
            } else if (part.startsWith("T:")) {
                dto.setPhoneNumber(part.substring(2));
            } else if (part.startsWith("E:")) {
                dto.setEmail(part.substring(2));
            } else if (part.startsWith("S:")) {
                dto.setServiceName(part.substring(2));
            }
        }

        return dto;
    }
}