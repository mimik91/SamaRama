package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.dto.ServiceRegisterDto;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.BicycleEnumerationService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminController {

    private final UserRepository userRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServiceOrderService serviceOrderService;
    private final BicycleEnumerationService enumerationService;
    private final EmailService emailService;
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());



    public AdminController(UserRepository userRepository,
                           IncompleteBikeRepository incompleteBikeRepository,
                           ServiceOrderService serviceOrderService,
                           BicycleEnumerationService enumerationService, EmailService emailService) {
        this.userRepository = userRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceOrderService = serviceOrderService;
        this.enumerationService = enumerationService;
        this.emailService = emailService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Map<String, Object> stats = new HashMap<>();

        // Get user info
        Optional<User> currentUser = userRepository.findByEmail(email);
        if (currentUser.isPresent()) {
            User user = currentUser.get();
            stats.put("user", Map.of(
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "roles", user.getRoles()
            ));
        }

        // Get roles from authentication
        stats.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        // Count entities
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBicycles", incompleteBikeRepository.count());
        stats.put("pendingOrders", serviceOrderService.countServiceOrders());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/bicycles")
    public ResponseEntity<?> getAllBicycles() {
        return ResponseEntity.ok(incompleteBikeRepository.findAll());
    }

    @GetMapping("/orders")
    public List<ServiceOrderResponseDto> getAllOrders() {
        return serviceOrderService.getAllServiceOrders();
    }

    @GetMapping("/enumerations")
    public ResponseEntity<?> getAllEnumerations() {
        return ResponseEntity.ok(enumerationService.getAllEnumerations());
    }

    // This endpoint is just for testing admin access
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        return ResponseEntity.ok(Map.of(
                "message", "Admin access successful",
                "user", auth.getName(),
                "roles", roles
        ));
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

    /**
     * Parsuje dane serwisu z formatowanego stringa
     */
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