package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.GuestServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceRegisterDto;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.GuestOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/guest-orders")
public class GuestOrderController {

    private final GuestOrderService guestOrderService;
    private final EmailService emailService;
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());


    public GuestOrderController(GuestOrderService guestOrderService, EmailService emailService) {
        this.guestOrderService = guestOrderService;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<?> createGuestOrder(@Valid @RequestBody GuestServiceOrderDto orderDto) {
        return guestOrderService.processGuestOrder(orderDto);
    }

    @PostMapping("/service-registration")
    public ResponseEntity<?> serviceRegistration(@RequestBody List<String> serviceData) {
        logger.info("Received service registration: " + serviceData);

        if (serviceData == null || serviceData.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Brak danych rejestracyjnych"));
        }

        // Pobierz pierwszy element z listy - dokładnie tak jak widać na zrzucie ekranu
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