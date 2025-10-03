package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.mapDto.BikeServiceDto;
import com.samarama.bicycle.api.dto.BikeServiceRegisteredDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.BikeServiceRegistered;
import com.samarama.bicycle.api.repository.BikeServiceRegisteredRepository;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Komponent odpowiedzialny za walidację danych serwisów rowerowych
 */
@Component
public class BikeServiceValidator {

    private final BikeServiceRepository bikeServiceRepository;
    private final BikeServiceRegisteredRepository bikeServiceRegisteredRepository;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z]{2,})+(/.*)?$"
    );

    @Autowired
    public BikeServiceValidator(BikeServiceRepository bikeServiceRepository, BikeServiceRegisteredRepository bikeServiceRegisteredRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.bikeServiceRegisteredRepository = bikeServiceRegisteredRepository;
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

    // === BIKE_SERVICE_REGISTERED ===


    /**
     * Waliduje dane dla tworzenia nowego zarejestrowanego serwisu
     */
    public ResponseEntity<?> validateForCreation(BikeServiceRegisteredDto bikeServiceRegisteredDto) {
        // Walidacja podstawowych danych z BikeService
        ResponseEntity<?> basicValidation = validateBasicRegisteredData(bikeServiceRegisteredDto);
        if (basicValidation != null) {
            return basicValidation;
        }

        // Sprawdź unikalność nazwy serwisu
        if (bikeServiceRegisteredRepository.existsByNameIgnoreCase(bikeServiceRegisteredDto.name())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Serwis o podanej nazwie już istnieje"));
        }

        // Sprawdź unikalność emaila jeśli został podany
        if (bikeServiceRegisteredDto.email() != null &&
                !bikeServiceRegisteredDto.email().trim().isEmpty() &&
                bikeServiceRegisteredRepository.existsByEmailIgnoreCase(bikeServiceRegisteredDto.email())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Serwis z podanym adresem email już istnieje"));
        }

        return null; // Wszystko OK
    }

    /**
     * Waliduje dane dla aktualizacji istniejącego zarejestrowanego serwisu
     */
    public ResponseEntity<?> validateForUpdate(BikeServiceRegisteredDto bikeServiceRegisteredDto,
                                               BikeServiceRegistered existingService) {
        // Walidacja podstawowych danych
        ResponseEntity<?> basicValidation = validateBasicRegisteredData(bikeServiceRegisteredDto);
        if (basicValidation != null) {
            return basicValidation;
        }

        // Sprawdź unikalność nazwy (z wyłączeniem aktualnego serwisu)
        if (!bikeServiceRegisteredDto.name().equalsIgnoreCase(existingService.getName()) &&
                bikeServiceRepository.existsByNameIgnoreCase(bikeServiceRegisteredDto.name())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Podana nazwa jest już używana przez inny serwis"));
        }

        // Sprawdź unikalność emaila (z wyłączeniem aktualnego serwisu)
        if (bikeServiceRegisteredDto.email() != null &&
                !bikeServiceRegisteredDto.email().trim().isEmpty() &&
                !bikeServiceRegisteredDto.email().equalsIgnoreCase(existingService.getEmail()) &&
                bikeServiceRepository.existsByEmailIgnoreCase(bikeServiceRegisteredDto.email())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Serwis z podanym adresem email już istnieje"));
        }

        return null; // Wszystko OK
    }

    /**
     * Podstawowa walidacja danych dla zarejestrowanego serwisu
     */
    private ResponseEntity<?> validateBasicRegisteredData(BikeServiceRegisteredDto dto) {
        // Walidacja nazwy - obowiązkowe pole
        ResponseEntity<?> nameValidation = validateName(dto.name());
        if (nameValidation != null) {
            return nameValidation;
        }

        // Walidacja emaila
        if (dto.email() != null && !dto.email().trim().isEmpty()) {
            if (!isValidEmail(dto.email())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nieprawidłowy format adresu email"));
            }
            if (dto.email().length() > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Adres email nie może być dłuższy niż 100 znaków"));
            }
        }

        // Walidacja strony internetowej
        if (dto.website() != null && !dto.website().trim().isEmpty()) {
            if (!isValidUrl(dto.website())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nieprawidłowy format strony internetowej"));
            }
            if (dto.website().length() > 255) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "URL strony internetowej nie może być dłuższy niż 255 znaków"));
            }
        }

        // Walidacja opisu
        if (dto.description() != null && dto.description().length() > 1500) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Opis nie może być dłuższy niż 1500 znaków"));
        }

        // Walidacja suffiksu
        if (dto.suffix() != null && dto.suffix().length() > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Sufiks nie może być dłuższy niż 100 znaków"));
        }

        // Walidacja osoby kontaktowej
        if (dto.contactPerson() != null && dto.contactPerson().length() > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Nazwa osoby kontaktowej nie może być dłuższa niż 100 znaków"));
        }

        // Walidacja danych adresowych
        ResponseEntity<?> addressValidation = validateAddressData(dto);
        if (addressValidation != null) {
            return addressValidation;
        }

        // Walidacja numeru telefonu
        if (dto.phoneNumber() != null && !dto.phoneNumber().trim().isEmpty()) {
            if (!isValidPhoneNumber(dto.phoneNumber())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nieprawidłowy format numeru telefonu"));
            }
            if (dto.phoneNumber().length() > 15) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Numer telefonu nie może być dłuższy niż 15 znaków"));
            }
        }

        // Walidacja kosztów transportu
        if (dto.transportCost() != null && dto.transportCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Koszt transportu nie może być ujemny"));
        }

        // Walidacja współrzędnych geograficznych
        if (dto.latitude() != null || dto.longitude() != null) {
            if (!areValidCoordinates(dto.latitude(), dto.longitude())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nieprawidłowe współrzędne geograficzne"));
            }
        }

        return null; // Brak błędów walidacji
    }

    /**
     * Walidacja danych adresowych
     */
    private ResponseEntity<?> validateAddressData(BikeServiceRegisteredDto dto) {
        if (dto.street() != null && dto.street().length() > 255) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Nazwa ulicy nie może być dłuższa niż 255 znaków"));
        }

        if (dto.building() != null && dto.building().length() > 20) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Numer budynku nie może być dłuższy niż 20 znaków"));
        }

        if (dto.flat() != null && dto.flat().length() > 20) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Numer mieszkania nie może być dłuższy niż 20 znaków"));
        }

        if (dto.postalCode() != null && dto.postalCode().length() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Kod pocztowy nie może być dłuższy niż 10 znaków"));
        }

        if (dto.city() != null && dto.city().length() > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Nazwa miasta nie może być dłuższa niż 100 znaków"));
        }

        return null;
    }

    /**
     * Walidacja formatu URL
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // Pusty URL jest dozwolony
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }


}