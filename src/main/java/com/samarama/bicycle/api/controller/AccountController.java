package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.PasswordChangeDto;
import com.samarama.bicycle.api.dto.UserProfileUpdateDto;
import com.samarama.bicycle.api.model.IndividualUser;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.IndividualUserRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;
    private final IndividualUserRepository individualUserRepository;
    private final PasswordEncoder passwordEncoder;

    // Use @Lazy on the PasswordEncoder to prevent circular dependencies
    public AccountController(UserRepository userRepository, IndividualUserRepository individualUserRepository,
                             @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.individualUserRepository = individualUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        String email = getCurrentUserEmail();
        Optional<IndividualUser> indUserOpt = individualUserRepository.findByEmail(email);

        if (indUserOpt.isEmpty()) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail()
            ));
        } else {
            IndividualUser user = indUserOpt.get();
            String phoneNumber = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "phoneNumber", phoneNumber
            ));
        }





    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileUpdateDto updateDto) {
        String email = getCurrentUserEmail();
        Optional<IndividualUser> userOpt = individualUserRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IndividualUser user = userOpt.get();
        user.setFirstName(updateDto.firstName());
        user.setLastName(updateDto.lastName());
        user.setPhoneNumber(updateDto.phoneNumber());

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
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
        return ResponseEntity.notFound().build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}