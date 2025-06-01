package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.ServiceOrderService;
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

    private final TransportOrderService transportOrderService;
    private final ServiceOrderService serviceOrderService;
    private final EmailService emailService;

    public GuestOrderController(
            TransportOrderService transportOrderService,
            ServiceOrderService serviceOrderService,
            EmailService emailService) {
        this.transportOrderService = transportOrderService;
        this.serviceOrderService = serviceOrderService;
        this.emailService = emailService;
    }

    /**
     * Tworzy zamówienie transportowe dla gości (TYLKO transport)
     */
    @PostMapping("/transport")
    public ResponseEntity<?> createGuestTransportOrder(@Valid @RequestBody TransportOrderDto dto) {
        return transportOrderService.createGuestTransportOrder(dto);
    }

    /**
     * Tworzy zamówienie serwisowe dla gości (transport + serwis)
     */
    @PostMapping("/service")
    public ResponseEntity<?> createGuestServiceOrder(@Valid @RequestBody ServiceOrderDto dto) {
        return serviceOrderService.createGuestServiceOrder(dto);
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
     * Informacje o zasadach dla gości
     */
    @GetMapping("/info")
    public ResponseEntity<?> getGuestOrderInfo() {
        return ResponseEntity.ok(Map.of(
                "transportOrders", Map.of(
                        "description", "Transport roweru do zewnętrznego serwisu",
                        "pricing", "30 zł base + 15 zł za każdy dodatkowy rower + 2.50 zł/km",
                        "endpoint", "/api/guest-orders/transport"
                ),
                "serviceOrders", Map.of(
                        "description", "Transport + serwis w naszym własnym serwisie",
                        "pricing", "Transport (10% rabatu) + cena pakietu serwisowego",
                        "endpoint", "/api/guest-orders/service",
                        "availablePackages", "/api/service-packages/active"
                ),
                "limitations", List.of(
                        "Goście nie mogą modyfikować zamówień po utworzeniu",
                        "Powiadomienia tylko na podany email",
                        "Brak dostępu do panelu klienta"
                ),
                "advantages", List.of(
                        "Szybkie złożenie zamówienia bez rejestracji",
                        "Wszystkie funkcje dostępne od razu",
                        "Automatyczne powiadomienia email"
                ),
                "requiredFields", Map.of(
                        "transport", List.of("clientEmail", "clientPhone", "bicycles", "pickupDate", "pickupAddress", "city", "targetServiceId", "transportPrice"),
                        "service", List.of("clientEmail", "clientPhone", "bicycles", "pickupDate", "pickupAddress", "city", "servicePackageId", "transportPrice")
                )
        ));
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

    /**
     * Walidacja danych zamówienia gościa (publiczny endpoint)
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateGuestOrder(@RequestBody Map<String, Object> orderData) {
        try {
            String orderType = (String) orderData.get("orderType");
            List<String> errors = new java.util.ArrayList<>();

            // Podstawowa walidacja
            if (orderData.get("clientEmail") == null || ((String) orderData.get("clientEmail")).trim().isEmpty()) {
                errors.add("Email klienta jest wymagany");
            }

            if (orderData.get("clientPhone") == null || ((String) orderData.get("clientPhone")).trim().isEmpty()) {
                errors.add("Telefon klienta jest wymagany");
            }

            if (orderData.get("bicycles") == null || ((List<?>) orderData.get("bicycles")).isEmpty()) {
                errors.add("Lista rowerów jest wymagana");
            }

            if (orderData.get("pickupDate") == null) {
                errors.add("Data odbioru jest wymagana");
            }

            if (orderData.get("pickupAddress") == null || ((String) orderData.get("pickupAddress")).trim().isEmpty()) {
                errors.add("Adres odbioru jest wymagany");
            }

            if (orderData.get("city") == null || ((String) orderData.get("city")).trim().isEmpty()) {
                errors.add("Miasto jest wymagane");
            }

            // Walidacja specyficzna dla typu
            if ("transport".equals(orderType)) {
                if (orderData.get("targetServiceId") == null) {
                    errors.add("ID serwisu docelowego jest wymagane dla transportu");
                }
                if (orderData.get("transportPrice") == null) {
                    errors.add("Cena transportu jest wymagana");
                }
            } else if ("service".equals(orderType)) {
                if (orderData.get("servicePackageId") == null && orderData.get("servicePackageCode") == null) {
                    errors.add("Pakiet serwisowy jest wymagany");
                }
                if (orderData.get("transportPrice") == null) {
                    errors.add("Cena transportu jest wymagana");
                }
            }

            if (errors.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "message", "Dane zamówienia są prawidłowe"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "errors", errors
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Błąd podczas walidacji: " + e.getMessage()
            ));
        }
    }

    /**
     * Pobiera przykładowe dane zamówienia (dla testów/dokumentacji)
     */
    @GetMapping("/example")
    public ResponseEntity<?> getExampleOrderData() {
        return ResponseEntity.ok(Map.of(
                "transportOrderExample", Map.of(
                        "clientEmail", "guest@example.com",
                        "clientPhone", "123456789",
                        "clientName", "Jan Kowalski",
                        "city", "Kraków",
                        "bicycles", List.of(
                                Map.of("brand", "Trek", "model", "Marlin 7", "additionalInfo", "Górski")
                        ),
                        "pickupDate", "2024-12-25",
                        "pickupAddress", "ul. Przykładowa 123",
                        "targetServiceId", 2,
                        "transportPrice", 45.0,
                        "additionalNotes", "Proszę o kontakt przed odbiorem"
                ),
                "serviceOrderExample", Map.of(
                        "clientEmail", "guest@example.com",
                        "clientPhone", "123456789",
                        "clientName", "Anna Nowak",
                        "city", "Kraków",
                        "bicycles", List.of(
                                Map.of("brand", "Specialized", "model", "Rockhopper", "additionalInfo", "Górski")
                        ),
                        "pickupDate", "2024-12-25",
                        "pickupAddress", "ul. Serwisowa 456",
                        "servicePackageId", 1,
                        "transportPrice", 27.0,
                        "additionalNotes", "Rower wymaga przeglądu podstawowego"
                )
        ));
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