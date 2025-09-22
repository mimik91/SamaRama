package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.ServiceUserDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import com.samarama.bicycle.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signin/client")
    public ResponseEntity<Map<String, Object>> authenticateClient(@Valid @RequestBody LoginDto loginDto) {
        return authService.authenticateClient(loginDto);
    }


    @PostMapping("/signup/client")
    public ResponseEntity<Map<String, String>> registerClient(@Valid @RequestBody UserRegistrationDto registrationDto) {
        return authService.registerClient(registrationDto);
    }

    @PostMapping("/signup/service")
    public ResponseEntity<Map<String, String>> registerServiceUser(@Valid @RequestBody ServiceUserDto serviceUserDto) {
        return authService.registerServiceUser(serviceUserDto);
    }
}