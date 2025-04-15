package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.exceptions.BikeServiceNotFoundException;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.OpeningHours;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.security.JwtUtils;
import com.samarama.bicycle.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
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

    @Override
    public ResponseEntity<Map<String, Object>> authenticateClient(LoginDto loginDto) {
        try {
            // Create authentication token with additional context info
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password());

            // Add context information
            Map<String, String> details = new HashMap<>();
            details.put("loginContext", "client");
            authToken.setDetails(details);

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            User user = userRepository.findByEmail(loginDto.email())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            if (user.hasRole("ROLE_ADMIN")) {
                response.put("role", "ADMIN");
            } else if (user.hasRole("ROLE_MODERATOR")) {
                response.put("role", "MODERATOR");
            } else {
                response.put("role", "CLIENT");
            }

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Authentication error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> authenticateService(LoginDto loginDto) {
        try {
            // Create authentication token with additional context info
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password());

            // Add context information
            Map<String, String> details = new HashMap<>();
            details.put("loginContext", "service");
            authToken.setDetails(details);

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            BikeService service = bikeServiceRepository.findByEmail(loginDto.email())
                    .orElseThrow(() -> new BikeServiceNotFoundException(loginDto.email()));

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", service.getId());
            response.put("email", service.getEmail());
            response.put("name", service.getName());
            response.put("role", "SERVICE");

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        } catch (BikeServiceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Authentication error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Map<String, String>> registerClient(UserRegistrationDto registrationDto) {
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
        user.addRole("ROLE_CLIENT"); // Ensure role is set

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    @Override
    public ResponseEntity<Map<String, String>> registerService(BikeServiceDto bikeServiceDto) {
        if (userRepository.existsByEmail(bikeServiceDto.email()) ||
                bikeServiceRepository.existsByEmail(bikeServiceDto.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use!"));
        }

        try {
            BikeService bikeService = new BikeService();
            bikeService.setName(bikeServiceDto.name());
            bikeService.setStreet(bikeServiceDto.street());
            bikeService.setBuilding(bikeServiceDto.building());
            bikeService.setFlat(bikeServiceDto.flat());
            bikeService.setPostalCode(bikeServiceDto.postalCode());
            bikeService.setCity(bikeServiceDto.city());
            bikeService.setPhoneNumber(bikeServiceDto.phoneNumber());
            bikeService.setBusinessPhone(bikeServiceDto.businessPhone());
            bikeService.setEmail(bikeServiceDto.email());
            bikeService.setDescription(bikeServiceDto.description());
            bikeService.setPassword(encoder.encode(bikeServiceDto.password()));
            bikeService.setLatitude(bikeServiceDto.latitude());
            bikeService.setLongitude(bikeServiceDto.longitude());

            // Create default empty opening hours
            OpeningHours openingHours = new OpeningHours();
            openingHours.setBikeService(bikeService);
            bikeService.setOpeningHours(openingHours);

            bikeServiceRepository.save(bikeService);
            return ResponseEntity.ok(Map.of("message", "Bike service registered successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Registration error: " + e.getMessage()));
        }
    }
}