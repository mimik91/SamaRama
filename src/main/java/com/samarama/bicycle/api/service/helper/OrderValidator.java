package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.ServiceOrTransportOrderDto;
import com.samarama.bicycle.api.repository.ServicePackageRepository;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.service.ServiceSlotService;
import com.samarama.bicycle.api.service.impl.CityValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ujednolicony walidator dla ServiceOrTransportOrderDto
 * Obsługuje walidację dla użytkowników zalogowanych i gości
 */
@Component
public class OrderValidator {

    private final ServicePackageRepository servicePackageRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServiceSlotService serviceSlotService;
    private final CityValidator cityValidator;

    @Value("${app.internal.service.id}")
    private String internalServiceIdString;

    public OrderValidator(
            ServicePackageRepository servicePackageRepository,
            BikeServiceRepository bikeServiceRepository,
            ServiceSlotService serviceSlotService,
            CityValidator cityValidator) {
        this.servicePackageRepository = servicePackageRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.serviceSlotService = serviceSlotService;
        this.cityValidator = cityValidator;
    }

    /**
     * Główna metoda walidacji - automatycznie rozpoznaje typ zamówienia
     */
    public ValidationResult validate(ServiceOrTransportOrderDto dto) {
        if (dto.isGuestOrder()) {
            return validateGuestOrder(dto);
        } else if (dto.isUserOrder()) {
            return validateUserOrder(dto);
        } else {
            return ValidationResult.invalid("Nie można określić typu zamówienia - brak userId i email");
        }
    }

    /**
     * Waliduj zamówienie użytkownika zalogowanego
     */
    public ValidationResult validateUserOrder(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // === PODSTAWOWE POLA ===
        errors.addAll(validateBasicFields(dto));

        // === UŻYTKOWNIK ===
        if (dto.getUserId() == null) {
            errors.add("ID użytkownika jest wymagane");
        }

        // === ROWERY ===
        errors.addAll(validateBicyclesForUser(dto));

        // === ADRES ===
        errors.addAll(validateAddressForUser(dto));

        // === TYP ZAMÓWIENIA ===
        errors.addAll(validateOrderType(dto));

        // === SLOTY ===
        if (dto.getPickupDate() != null) {
            errors.addAll(validateSlots(dto));
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Waliduj zamówienie gościa
     */
    public ValidationResult validateGuestOrder(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // === PODSTAWOWE POLA ===
        errors.addAll(validateBasicFields(dto));

        // === DANE GOŚCIA ===
        errors.addAll(validateGuestData(dto));

        // === OGRANICZENIA DLA GOŚCI ===
        errors.addAll(validateGuestRestrictions(dto));

        // === ROWERY ===
        errors.addAll(validateBicyclesForGuest(dto));

        // === ADRES ===
        errors.addAll(validateAddressForGuest(dto));

        // === PAKIET SERWISOWY (wymagany dla gości) ===
        errors.addAll(validateServicePackageForGuest(dto));

        // === SLOTY ===
        if (dto.getPickupDate() != null) {
            errors.addAll(validateSlots(dto));
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // === WALIDACJA PODSTAWOWYCH PÓL ===

    private List<String> validateBasicFields(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getPickupDate() == null) {
            errors.add("Data odbioru jest wymagana");
        } else if (dto.getPickupDate().isBefore(LocalDate.now())) {
            errors.add("Data odbioru nie może być w przeszłości");
        }

        if (dto.getTargetServiceId() == null) {
            dto.setTargetServiceId(Long.parseLong(internalServiceIdString));
        } else if (!bikeServiceRepository.existsById(dto.getTargetServiceId())) {
            errors.add("Nieprawidłowy serwis docelowy");
        }

        if (dto.getTransportPrice() != null && dto.getTransportPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Cena transportu nie może być ujemna");
        }

        return errors;
    }

    // === WALIDACJA DANYCH GOŚCIA ===

    private List<String> validateGuestData(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            errors.add("Email jest wymagany");
        } else if (!isValidEmail(dto.getEmail())) {
            errors.add("Nieprawidłowy format email");
        }

        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            errors.add("Numer telefonu jest wymagany");
        } else if (!isValidPhone(dto.getPhone())) {
            errors.add("Nieprawidłowy format numeru telefonu");
        }

        return errors;
    }

    private List<String> validateGuestRestrictions(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // Goście mogą składać tylko zamówienia serwisowe
        if (!dto.isServiceOrder()) {
            errors.add("Goście mogą składać tylko zamówienia serwisowe");
        }

        // Goście muszą używać nowych rowerów i adresów
        if (dto.usesExistingBicycles()) {
            errors.add("Gość nie może używać istniejących rowerów z systemu");
        }

        if (dto.usesExistingAddress()) {
            errors.add("Gość nie może używać istniejącego adresu z systemu");
        }

        return errors;
    }

    // === WALIDACJA ROWERÓW ===

    private List<String> validateBicyclesForUser(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (!dto.usesExistingBicycles() && !dto.usesNewBicycles()) {
            errors.add("Brak rowerów w zamówieniu");
        }

        if (dto.usesExistingBicycles() && dto.usesNewBicycles()) {
            errors.add("Nie można jednocześnie używać istniejących i nowych rowerów");
        }

        if (dto.usesExistingBicycles() && (dto.getBicycleIds() == null || dto.getBicycleIds().isEmpty())) {
            errors.add("Lista ID rowerów nie może być pusta");
        }

        if (dto.usesNewBicycles() && (dto.getBicycles() == null || dto.getBicycles().isEmpty())) {
            errors.add("Lista nowych rowerów nie może być pusta");
        }

        return errors;
    }

    private List<String> validateBicyclesForGuest(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (!dto.usesNewBicycles()) {
            errors.add("Gość musi podać dane rowerów");
        }

        if (dto.getBicycles() == null || dto.getBicycles().isEmpty()) {
            errors.add("Brak rowerów w zamówieniu");
        } else {
            // Walidacja każdego roweru
            for (int i = 0; i < dto.getBicycles().size(); i++) {
                var bike = dto.getBicycles().get(i);
                if (bike.brand() == null || bike.brand().trim().isEmpty()) {
                    errors.add("Marka roweru #" + (i + 1) + " jest wymagana");
                }
                if (bike.model() == null || bike.model().trim().isEmpty()) {
                    errors.add("Model roweru #" + (i + 1) + " jest wymagany");
                }
            }
        }

        return errors;
    }

    // === WALIDACJA ADRESU ===

    private List<String> validateAddressForUser(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (!dto.usesExistingAddress() && !dto.usesNewAddress()) {
            errors.add("Brak adresu odbioru");
        }

        if (dto.usesExistingAddress() && dto.usesNewAddress()) {
            errors.add("Nie można jednocześnie używać istniejącego i nowego adresu");
        }

        // Walidacja nowego adresu
        if (dto.usesNewAddress()) {
            errors.addAll(validateNewAddressFields(dto));
        }

        return errors;
    }

    private List<String> validateAddressForGuest(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (!dto.usesNewAddress()) {
            errors.add("Gość musi podać adres odbioru");
        }

        errors.addAll(validateNewAddressFields(dto));

        return errors;
    }

    private List<String> validateNewAddressFields(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getPickupStreet() == null || dto.getPickupStreet().trim().isEmpty()) {
            errors.add("Ulica jest wymagana");
        }

        if (dto.getPickupBuildingNumber() == null || dto.getPickupBuildingNumber().trim().isEmpty()) {
            errors.add("Numer budynku jest wymagany");
        }

        if (dto.getPickupCity() == null || dto.getPickupCity().trim().isEmpty()) {
            errors.add("Miasto jest wymagane");
        } else if (!cityValidator.isValidCity(dto.getPickupCity())) {
            errors.add("Nieprawidłowe miasto: " + dto.getPickupCity());
        }

        return errors;
    }

    // === WALIDACJA TYPU ZAMÓWIENIA ===

    private List<String> validateOrderType(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.isServiceOrder()) {
            errors.addAll(validateServiceOrder(dto));
        } else {
            errors.addAll(validateTransportOrder(dto));
        }

        return errors;
    }

    private List<String> validateServiceOrder(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getServicePackageId() == null) {
            errors.add("Pakiet serwisowy jest wymagany dla zamówienia serwisowego");
        } else if (!servicePackageRepository.existsById(dto.getServicePackageId())) {
            errors.add("Nieprawidłowy pakiet serwisowy");
        }

        // Zamówienia serwisowe muszą być kierowane do serwisu własnego
        if (dto.getTargetServiceId() != null && !dto.getTargetServiceId().equals(1L)) {
            errors.add("Zamówienia serwisowe muszą być kierowane do serwisu własnego (ID=1)");
        }

        return errors;
    }

    private List<String> validateTransportOrder(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // Zamówienia transportowe nie mogą być kierowane do serwisu własnego
        if (dto.getTargetServiceId() != null && dto.getTargetServiceId().equals(1L)) {
            errors.add("Zamówienia transportowe nie mogą być kierowane do serwisu własnego");
        }

        // Nie powinno mieć pakietu serwisowego
        if (dto.getServicePackageId() != null) {
            errors.add("Zamówienia transportowe nie powinny mieć pakietu serwisowego");
        }

        return errors;
    }

    private List<String> validateServicePackageForGuest(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getServicePackageId() == null) {
            errors.add("Pakiet serwisowy jest wymagany");
        } else if (!servicePackageRepository.existsById(dto.getServicePackageId())) {
            errors.add("Nieprawidłowy pakiet serwisowy");
        }

        return errors;
    }

    // === WALIDACJA SLOTÓW ===

    private List<String> validateSlots(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        int bikesCount = dto.getBicycleCount();

        if (bikesCount <= 0) {
            errors.add("Liczba rowerów musi być większa od 0");
            return errors; // Nie ma sensu sprawdzać slotów bez rowerów
        }

        if (!serviceSlotService.isWithinMaxBikesPerOrder(dto.getPickupDate(), bikesCount)) {
            int maxPerOrder = serviceSlotService.getMaxBikesPerOrder(dto.getPickupDate());
            errors.add("Przekroczono maksymalną liczbę rowerów na zamówienie (" + maxPerOrder + ")");
        }

        if (!serviceSlotService.areSlotsAvailable(dto.getPickupDate(), bikesCount)) {
            errors.add("Brak wystarczającej liczby wolnych miejsc na wybrany dzień");
        }

        return errors;
    }

    // === METODY POMOCNICZE ===

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[0-9+\\-\\s()]{9,15}$");
    }

    // === KLASA WYNIKU WALIDACJI ===

    /**
     * Klasa reprezentująca wynik walidacji
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        public String getAllErrors() {
            return String.join("; ", errors);
        }

        public int getErrorCount() {
            return errors.size();
        }

        // Factory methods
        public static ValidationResult valid() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, List.of(error));
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, errors=%d}", valid, errors.size());
        }
    }
}