package com.samarama.bicycle.api.controller;


import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bicycles")
public class BicycleController {
    private final BicycleRepository bicycleRepository;
    private final UserRepository userRepository;

    public BicycleController(BicycleRepository bicycleRepository, UserRepository userRepository) {
        this.bicycleRepository = bicycleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Bicycle>> getUserBicycles() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bicycle> bicycles = bicycleRepository.findByOwner(user);
        return ResponseEntity.ok(bicycles);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'SERVICEMAN')")
    public ResponseEntity<?> addBicycle(@Valid @RequestBody BicycleDto bicycleDto) {
        if (bicycleRepository.existsByFrameNumber(bicycleDto.frameNumber())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bicycle with this frame number already exists"));
        }

        Bicycle bicycle = new Bicycle();
        bicycle.setFrameNumber(bicycleDto.frameNumber());
        bicycle.setBrand(bicycleDto.brand());
        bicycle.setModel(bicycleDto.model());
        bicycle.setType(bicycleDto.type());

        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only set owner if the user is a client
        if (user.getRole() == User.UserRole.CLIENT) {
            bicycle.setOwner(user);
        }

        bicycleRepository.save(bicycle);
        return ResponseEntity.ok(Map.of("message", "Bicycle added successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SERVICEMAN')")
    public ResponseEntity<?> searchBicycleByFrameNumber(@RequestParam String frameNumber) {
        Optional<Bicycle> bicycle = bicycleRepository.findByFrameNumber(frameNumber);
        return bicycle.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}