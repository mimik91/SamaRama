package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.BicycleEnumerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminController {

    private final UserRepository userRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final BicycleEnumerationService enumerationService;

    public AdminController(UserRepository userRepository,
                           IncompleteBikeRepository incompleteBikeRepository,
                           ServiceOrderRepository serviceOrderRepository,
                           BicycleEnumerationService enumerationService) {
        this.userRepository = userRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.enumerationService = enumerationService;
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
        stats.put("pendingOrders", serviceOrderRepository.count());

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
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(serviceOrderRepository.findAll());
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
}