package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.ServiceOrTransportOrderDto;
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

    private final BikeServiceRepository bikeServiceRepository;
    private final ServiceSlotService serviceSlotService;
    private final CityValidator cityValidator;

    @Value("${app.internal.service.id}")
    public static String internalServiceIdString = "2137";

    public OrderValidator(
            BikeServiceRepository bikeServiceRepository,
            ServiceSlotService serviceSlotService,
            CityValidator cityValidator) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.serviceSlotService = serviceSlotService;
        this.cityValidator = cityValidator;
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

        return new ArrayList<>(validateTransportOrder(dto));
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

    private List<String> validateGuestTransportOrderSpecific(ServiceOrTransportOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // 1. TargetServiceId jest wymagany
        if (dto.getTargetServiceId() == null) {
            errors.add("Serwis docelowy jest wymagany dla zamówień transportowych");
            return errors; // Nie ma sensu sprawdzać dalej
        }

        // 2. Serwis musi istnieć
        if (!bikeServiceRepository.existsById(dto.getTargetServiceId())) {
            errors.add("Serwis docelowy nie istnieje (ID: " + dto.getTargetServiceId() + ")");
        }

        // 3. Nie może być serwis własny
        if (dto.getTargetServiceId().equals(Long.parseLong(internalServiceIdString))) {
            errors.add("Dla transportu do serwisu własnego użyj zamówienia serwisowego (dodaj servicePackageId)");
        }

        // 4. Cena transportu jest wymagana i musi być > 0
        if (dto.getTransportPrice() == null) {
            errors.add("Cena transportu jest wymagana dla zamówień transportowych");
        } else if (dto.getTransportPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Cena transportu musi być większa od 0 dla zamówień transportowych");
        }

        return errors;
    }






}