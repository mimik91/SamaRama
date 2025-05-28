package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.dto.ServiceRegisterDto;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.BicycleEnumerationService;
import com.samarama.bicycle.api.service.BikeServiceService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final BikeServiceService bikeServiceService;


    public AdminController(UserRepository userRepository,
                           IncompleteBikeRepository incompleteBikeRepository,
                           ServiceOrderService serviceOrderService,
                           BicycleEnumerationService enumerationService, EmailService emailService, BikeServiceService bikeServiceService) {
        this.userRepository = userRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceOrderService = serviceOrderService;
        this.enumerationService = enumerationService;
        this.bikeServiceService = bikeServiceService;
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

    @GetMapping("/bike-services")
    public ResponseEntity<List<BikeServiceDto>> getAllBikeServices() {
        List<BikeServiceDto> services = bikeServiceService.getAllBikeServicesForAdmin();
        return ResponseEntity.ok(services);
    }

    /**
     * Pobierz szczegóły serwisu rowerowego
     */
    @GetMapping("/bike-services/{id}")
    public ResponseEntity<BikeServiceDto> getBikeServiceById(@PathVariable Long id) {
        Optional<BikeServiceDto> service = bikeServiceService.getBikeServiceDetails(id);
        return service.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Utwórz nowy serwis rowerowy
     */
    @PostMapping("/bike-services")
    public ResponseEntity<?> createBikeService(@Valid @RequestBody BikeServiceDto bikeServiceDto) {
        return bikeServiceService.createBikeService(bikeServiceDto);
    }

    /**
     * Zaktualizuj serwis rowerowy
     */
    @PutMapping("/bike-services/{id}")
    public ResponseEntity<?> updateBikeService(
            @PathVariable Long id,
            @Valid @RequestBody BikeServiceDto bikeServiceDto) {
        return bikeServiceService.updateBikeService(id, bikeServiceDto);
    }

    /**
     * Usuń serwis rowerowy
     */
    @DeleteMapping("/bike-services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBikeService(@PathVariable Long id) {
        return bikeServiceService.deleteBikeService(id);
    }

    /**
     * Import serwisów z pliku CSV
     */
    @PostMapping("/bike-services/import")
    public ResponseEntity<?> importBikeServicesFromCsv(@RequestParam("file") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();

        return bikeServiceService.importBikeServicesFromCsv(file, adminEmail);
    }

    /**
     * Pobierz statystyki serwisów rowerowych
     */
    @GetMapping("/bike-services/statistics")
    public ResponseEntity<?> getBikeServiceStatistics() {
        return bikeServiceService.getBikeServiceStatistics();
    }
}