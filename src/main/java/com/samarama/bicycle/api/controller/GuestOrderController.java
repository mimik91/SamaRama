package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.UnifiedOrderService;
import com.samarama.bicycle.api.service.TransportOrderService;
import com.samarama.bicycle.api.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/guest-orders")
public class GuestOrderController {

    private static final Logger logger = Logger.getLogger(GuestOrderController.class.getName());

    private final UnifiedOrderService unifiedOrderService;
    private final TransportOrderService transportOrderService;
    private final EmailService emailService;

    public GuestOrderController(
            UnifiedOrderService unifiedOrderService,
            TransportOrderService transportOrderService,
            EmailService emailService) {
        this.unifiedOrderService = unifiedOrderService;
        this.transportOrderService = transportOrderService;
        this.emailService = emailService;
    }

    /**
     * Tworzy zamówienie serwisowe dla gości (transport + serwis)
     * DELEGUJE DO UnifiedOrderService
     */
    @PostMapping("/service")
    public ResponseEntity<?> createGuestServiceOrder(@Valid @RequestBody ServiceOrTransportOrderDto dto) {
        logger.info("Received guest service order for email: " + dto.getEmail());

        // Walidacja - musi być zamówienie serwisowe
        if (!dto.isServiceOrder()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Endpoint /service wymaga servicePackageId"
            ));
        }

        // Deleguj do UnifiedOrderService
        return unifiedOrderService.createGuestOrder(dto);
    }

    /**
     * Tworzy zamówienie transportowe dla gości (TYLKO transport)
     * POZOSTAJE JAK BYŁO
     */
    @PostMapping("/transport")
    public ResponseEntity<?> createGuestTransportOrder(@Valid @RequestBody ServiceOrTransportOrderDto dto) {


        return transportOrderService.createGuestTransportOrder(dto);
    }

    /**
     * Oblicza koszt transportu
     * POZOSTAJE JAK BYŁO
     */
    @PostMapping("/calculate-transport-cost")
    public ResponseEntity<?> calculateTransportCost(@RequestBody Map<String, Object> request) {
        return unifiedOrderService.calculateTransportCost(request);
    }


    /**
     * Rejestracja serwisu (legacy endpoint)
     * POZOSTAJE JAK BYŁO
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

    /**
     * Konwertuje TransportOrderDto do ServiceOrTransportOrderDto
     */
    private ServiceOrTransportOrderDto convertToUnifiedDto(TransportOrderDto dto) {
        return new ServiceOrTransportOrderDto(
                dto.bicycleIds(),           // bicycleIds
                dto.bicycles(),             // bicycles (guest bikes)
                null,                       // userId
                dto.clientEmail(),          // email
                dto.clientPhone(),          // phone
                null,                       // pickupAddressId
                null,                       // pickupStreet - będzie wyciągnięte z address
                null,                       // pickupBuildingNumber
                null,                       // pickupApartmentNumber
                dto.city(),                 // pickupCity
                null,                       // pickupPostalCode
                dto.pickupLatitude(),       // pickupLatitude
                dto.pickupLongitude(),      // pickupLongitude
                dto.pickupDate(),           // pickupDate
                dto.transportPrice(),       // transportPrice
                dto.transportNotes(),       // transportNotes
                dto.targetServiceId(),      // targetServiceId
                null,                       // servicePackageId - brak dla transportu
                null,                       // serviceNotes
                dto.additionalNotes()       // additionalNotes
        );
    }

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