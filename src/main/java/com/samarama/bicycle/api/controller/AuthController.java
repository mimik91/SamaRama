package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.exceptions.BikeServiceNotFoundException;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
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
    private final BikeServiceRepository bikeServiceRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          BikeServiceRepository bikeServiceRepository,
                          PasswordEncoder encoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signin/client")
    public ResponseEntity<?> authenticateClient(@Valid @RequestBody LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signin/service")
    public ResponseEntity<?> authenticateService(@Valid @RequestBody LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        BikeService service = bikeServiceRepository.findByEmail(loginDto.email());
        if(service == null){
            throw new BikeServiceNotFoundException(loginDto.email());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", service.getId());
        response.put("email", service.getEmail());
        response.put("name", service.getName());
        response.put("role", "SERVICE");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup/client")
    public ResponseEntity<?> registerClient(@Valid @RequestBody UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.email()) ||
                bikeServiceRepository.existsByEmail(registrationDto.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use!"));
        }

        User user = new User();
        user.setEmail(registrationDto.email());
        user.setFirstName(registrationDto.firstName());
        user.setLastName(registrationDto.lastName());
        user.setPhoneNumber(registrationDto.phoneNumber());
        user.setPassword(encoder.encode(registrationDto.password()));
        // Role CLIENT jest już domyślne w klasie User

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    @PostMapping("/signup/service")
    public ResponseEntity<?> registerService(@Valid @RequestBody BikeServiceDto bikeServiceDto) {
        if (userRepository.existsByEmail(bikeServiceDto.email()) ||
                bikeServiceRepository.existsByEmail(bikeServiceDto.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use!"));
        }

        BikeService bikeService = new BikeService();
        bikeService.setName(bikeServiceDto.name());
        bikeService.setAddress(bikeServiceDto.address());
        bikeService.setPostalCode(bikeServiceDto.postalCode());
        bikeService.setCity(bikeServiceDto.city());
        bikeService.setPhoneNumber(bikeServiceDto.phoneNumber());
        bikeService.setEmail(bikeServiceDto.email());
        bikeService.setDescription(bikeServiceDto.description());
        bikeService.setPassword(encoder.encode(bikeServiceDto.password()));

        // Konwersja openingHours do JSON zostanie obsłużona w kontrolerze BikeServiceController

        bikeServiceRepository.save(bikeService);

        return ResponseEntity.ok(Map.of("message", "Bike service registered successfully!"));
    }
}