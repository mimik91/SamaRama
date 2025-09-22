package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Komponent odpowiedzialny za walidację danych serwisów rowerowych
 */
@Component
public class BikeServiceValidator {

    private final BikeServiceRepository bikeServiceRepository;

    @Autowired
    public BikeServiceValidator(BikeServiceRepository bikeServiceRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
    }

    /**
     * Waliduje dane dla tworzenia nowego serwisu
     */
    public ResponseEntity<?> validateForCreation(BikeServiceDto bikeServiceDto) {
        // Walidacja nazwy - obowiązkowe pole
        ResponseEntity<?> nameValidation = validateName(bikeServiceDto.name());
        if (nameValidation != null) {
            return nameValidation;
        }

        // Walidacja - sprawdź czy nazwa nie jest już używana
        if (bikeServiceRepository.existsByNameIgnoreCase(bikeServiceDto.name())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Serwis o podanej nazwie już istnieje"));
        }

        return null; // Wszystko OK
    }

    /**
     * Waliduje dane dla aktualizacji istniejącego serwisu
     */
    public ResponseEntity<?> validateForUpdate(BikeServiceDto bikeServiceDto, BikeService existingService) {
        // Walidacja nazwy - obowiązkowe pole
        ResponseEntity<?> nameValidation = validateName(bikeServiceDto.name());
        if (nameValidation != null) {
            return nameValidation;
        }

        // Walidacja nazwa - sprawdź czy nowa nazwa nie koliduje z innym serwisem
        if (!bikeServiceDto.name().equalsIgnoreCase(existingService.getName()) &&
                bikeServiceRepository.existsByNameIgnoreCase(bikeServiceDto.name())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Podana nazwa jest już używana przez inny serwis"));
        }

        return null; // Wszystko OK
    }

    /**
     * Waliduje nazwę serwisu
     */
    private ResponseEntity<?> validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Nazwa serwisu jest wymagana"));
        }
        return null;
    }

    /**
     * Waliduje współrzędne geograficzne
     */
    public boolean areValidCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    /**
     * Waliduje format adresu email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Email nie jest obowiązkowy
        }

        // Prosty regex dla email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Waliduje format numeru telefonu
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true; // Telefon nie jest obowiązkowy
        }

        // Usuń białe znaki i znaki specjalne
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // Sprawdź czy to sensowny numer telefonu
        return cleaned.matches("^\\+?[0-9]{9,15}$");
    }
}