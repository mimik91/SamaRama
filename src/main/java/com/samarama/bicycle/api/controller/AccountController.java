package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.PasswordChangeDto;
import com.samarama.bicycle.api.dto.ServiceProfileUpdateDto;
import com.samarama.bicycle.api.dto.UserProfileUpdateDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserRepository userRepository,
                             BikeServiceRepository bikeServiceRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        String email = getCurrentUserEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "phoneNumber", user.getPhoneNumber()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileUpdateDto updateDto) {
        String email = getCurrentUserEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setFirstName(updateDto.firstName());
        user.setLastName(updateDto.lastName());
        user.setPhoneNumber(updateDto.phoneNumber());

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @GetMapping("/service-profile")
    public ResponseEntity<?> getServiceProfile() {
        String email = getCurrentUserEmail();
        Optional<BikeService> serviceOpt = bikeServiceRepository.findByEmail(email);

        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BikeService service = serviceOpt.get();

        // Create map using a map builder pattern instead of Map.of
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("id", service.getId());
        serviceData.put("email", service.getEmail());
        serviceData.put("name", service.getName());
        serviceData.put("phoneNumber", service.getPhoneNumber());
        serviceData.put("businessPhone", service.getBusinessPhone());
        serviceData.put("street", service.getStreet());
        serviceData.put("building", service.getBuilding());
        serviceData.put("flat", service.getFlat());
        serviceData.put("city", service.getCity());
        serviceData.put("postalCode", service.getPostalCode());
        serviceData.put("description", service.getDescription());
        serviceData.put("latitude", service.getLatitude());
        serviceData.put("longitude", service.getLongitude());

        return ResponseEntity.ok(serviceData);
    }

    @PutMapping("/service-profile")
    public ResponseEntity<?> updateServiceProfile(@RequestBody ServiceProfileUpdateDto updateDto) {
        String email = getCurrentUserEmail();
        Optional<BikeService> serviceOpt = bikeServiceRepository.findByEmail(email);

        if (serviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BikeService service = serviceOpt.get();
        service.setName(updateDto.name());
        service.setPhoneNumber(updateDto.phoneNumber());
        service.setBusinessPhone(updateDto.businessPhone());
        service.setStreet(updateDto.street());
        service.setBuilding(updateDto.building());
        service.setCity(updateDto.city());
        service.setPostalCode(updateDto.postalCode());

        bikeServiceRepository.save(service);

        return ResponseEntity.ok(Map.of("message", "Service profile updated successfully"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeDto passwordChangeDto) {
        String email = getCurrentUserEmail();

        // Check if user exists in the User table
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify current password
            if (!passwordEncoder.matches(passwordChangeDto.currentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(passwordChangeDto.newPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        }

        // Check if user exists in the BikeService table
        Optional<BikeService> serviceOpt = bikeServiceRepository.findByEmail(email);
        if (serviceOpt.isPresent()) {
            BikeService service = serviceOpt.get();

            // Verify current password
            if (!passwordEncoder.matches(passwordChangeDto.currentPassword(), service.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
            }

            // Update password
            service.setPassword(passwordEncoder.encode(passwordChangeDto.newPassword()));
            bikeServiceRepository.save(service);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        }

        return ResponseEntity.notFound().build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}