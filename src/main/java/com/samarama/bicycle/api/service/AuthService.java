package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.LoginDto;
import com.samarama.bicycle.api.dto.ServiceUserDto;
import com.samarama.bicycle.api.dto.UserRegistrationDto;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface AuthService {
    /**
     * Authenticate client user
     * @param loginDto credentials for authentication
     * @return authentication response with token and user details
     */
    ResponseEntity<Map<String, Object>> authenticateClient(LoginDto loginDto);

    /**
     * Register a new client user
     * @param registrationDto user registration data
     * @return registration result
     */
    ResponseEntity<Map<String, String>> registerClient(UserRegistrationDto registrationDto);

    /**
     * Register a new serviceUser
     * @param serviceUserDto bike service registration data
     * @return registration result
     */
    ResponseEntity<Map<String, String>> registerServiceUser(ServiceUserDto serviceUserDto);
}