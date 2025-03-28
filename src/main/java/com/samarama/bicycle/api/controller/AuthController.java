package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder encoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signin/client")
    public ResponseEntity<?> authenticateClient(@Valid @RequestBody LoginDto loginDto) {
        return authenticate(loginDto, User.UserRole.CLIENT);
    }

    @PostMapping("/signin/serviceman")
    public ResponseEntity<?> authenticateServiceman(@Valid @RequestBody LoginDto loginDto) {
        return authenticate(loginDto);
    }

    private ResponseEntity<?> authenticate(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != expectedRole) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid user role"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup/client")
    public ResponseEntity<?> registerClient(@Valid @RequestBody UserRegistrationDto registrationDto) {
        return registerUser(registrationDto, User.UserRole.CLIENT);
    }

    @PostMapping("/signup/serviceman")
    public ResponseEntity<?> registerServiceman(@Valid @RequestBody UserRegistrationDto registrationDto) {
        return registerUser(registrationDto, User.UserRole.SERVICEMAN);
    }

    private ResponseEntity<?> registerUser(UserRegistrationDto registrationDto, User.UserRole role) {
        if (userRepository.existsByEmail(registrationDto.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use!"));
        }

        User user = new User();
        user.setEmail(registrationDto.email());
        user.setFirstName(registrationDto.firstName());
        user.setLastName(registrationDto.lastName());
        user.setPhoneNumber(registrationDto.phoneNumber());
        user.setPassword(encoder.encode(registrationDto.password()));
        user.setRole(role);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }
}
