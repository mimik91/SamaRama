package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.service.ServiceSlotService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validator for ServiceOrder operations
 * Handles all validation logic for service orders including slots, dates, and business rules
 */
@Component
public class ServiceOrderValidator {

    private final ServiceSlotService serviceSlotService;
    private final CityValidator cityValidator;

    public ServiceOrderValidator(ServiceSlotService serviceSlotService, CityValidator cityValidator) {
        this.serviceSlotService = serviceSlotService;
        this.cityValidator = cityValidator;
    }

    // === MAIN VALIDATION METHODS ===

    /**
     * Validates service order data for logged users
     */
    public ValidationResult validateUserServiceOrder(ServiceOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // Basic data validation
        if (!dto.isValidForLoggedUser()) {
            errors.add("Nieprawidłowe dane zamówienia");
        }

        // Date validation
        validatePickupDate(dto.pickupDate(), errors);

        // City validation
        validateCity(dto.pickupAddress(), null, errors);

        // Service package validation
        validateServicePackage(dto, errors);

        // Bicycles validation
        validateBicycles(dto.bicycleIds(), null, errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates service order data for guest users
     */
    public ValidationResult validateGuestServiceOrder(ServiceOrderDto dto) {
        List<String> errors = new ArrayList<>();

        // Basic data validation
        if (!dto.isValidForGuest()) {
            errors.add("Nieprawidłowe dane zamówienia gościa");
        }

        // Date validation
        validatePickupDate(dto.pickupDate(), errors);

        // City validation
        validateCity(dto.pickupAddress(), dto.city(), errors);

        // Service package validation
        validateServicePackage(dto, errors);

        // Guest bicycles validation
        validateGuestBicycles(dto.bicycles(), errors);

        // Guest contact validation
        validateGuestContact(dto, errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates slot availability for service orders
     */
    public SlotValidationResult validateSlotAvailability(LocalDate pickupDate, int bikesCount) {
        // Check max bikes per order
        if (!serviceSlotService.isWithinMaxBikesPerOrder(pickupDate, bikesCount)) {
            int maxPerOrder = serviceSlotService.getMaxBikesPerOrder(pickupDate);
            return SlotValidationResult.failed(
                    "Przekroczono maksymalną liczbę rowerów na jedno zamówienie (" + maxPerOrder + ")",
                    Map.of("maxBikesPerOrder", maxPerOrder)
            );
        }

        // Check available slots
        if (!serviceSlotService.areSlotsAvailable(pickupDate, bikesCount)) {
            int maxPerDay = serviceSlotService.getMaxBikesPerDay(pickupDate);
            // Note: We would need access to repository to get booked count
            // For now, we'll use service slot service method
            return SlotValidationResult.failed(
                    "Brak wystarczającej liczby wolnych miejsc na wybrany dzień",
                    Map.of(
                            "maxBikesPerDay", maxPerDay,
                            "requestedBikes", bikesCount
                    )
            );
        }

        return SlotValidationResult.success();
    }

    // === SPECIFIC VALIDATION METHODS ===

    private void validatePickupDate(LocalDate pickupDate, List<String> errors) {
        if (pickupDate == null) {
            errors.add("Data odbioru jest wymagana");
            return;
        }

        if (pickupDate.isBefore(LocalDate.now())) {
            errors.add("Data odbioru nie może być w przeszłości");
        }

        LocalDate maxDate = LocalDate.now().plusMonths(1);
        if (pickupDate.isAfter(maxDate)) {
            errors.add("Data odbioru nie może być odleglejsza niż miesiąc");
        }
    }

    private void validateCity(String address, String city, List<String> errors) {
        if (address == null || address.trim().isEmpty()) {
            errors.add("Adres odbioru jest wymagany");
            return;
        }

        String cityToValidate = city != null ? city : cityValidator.extractCityFromAddress(address);

        if (cityToValidate == null || !cityValidator.isValidCity(cityToValidate)) {
            errors.add("Nieprawidłowe miasto");
        }
    }

    private void validateServicePackage(ServiceOrderDto dto, List<String> errors) {
        boolean hasPackageId = dto.servicePackageId() != null;
        boolean hasPackageCode = dto.servicePackageCode() != null && !dto.servicePackageCode().trim().isEmpty();

        if (!hasPackageId && !hasPackageCode) {
            errors.add("Pakiet serwisowy jest wymagany");
        }
    }

    private void validateBicycles(List<Long> bicycleIds, List<String> bicycles, List<String> errors) {
        if (bicycleIds == null || bicycleIds.isEmpty()) {
            errors.add("Lista rowerów jest wymagana");
            return;
        }

        if (bicycleIds.size() > 10) { // Business rule: max 10 bikes per order
            errors.add("Maksymalnie 10 rowerów na zamówienie");
        }
    }

    private void validateGuestBicycles(List<com.samarama.bicycle.api.dto.GuestBicycleDto> bicycles, List<String> errors) {
        if (bicycles == null || bicycles.isEmpty()) {
            errors.add("Lista rowerów jest wymagana");
            return;
        }

        if (bicycles.size() > 10) { // Business rule: max 10 bikes per order
            errors.add("Maksymalnie 10 rowerów na zamówienie");
        }

        // Validate each bicycle
        for (int i = 0; i < bicycles.size(); i++) {
            var bike = bicycles.get(i);
            if (bike.brand() == null || bike.brand().trim().isEmpty()) {
                errors.add("Marka roweru " + (i + 1) + " jest wymagana");
            }
        }
    }

    private void validateGuestContact(ServiceOrderDto dto, List<String> errors) {
        if (dto.clientEmail() == null || dto.clientEmail().trim().isEmpty()) {
            errors.add("Email klienta jest wymagany");
        } else if (!isValidEmail(dto.clientEmail())) {
            errors.add("Nieprawidłowy format email");
        }

        if (dto.clientPhone() == null || dto.clientPhone().trim().isEmpty()) {
            errors.add("Telefon klienta jest wymagany");
        } else if (!isValidPhone(dto.clientPhone())) {
            errors.add("Nieprawidłowy format telefonu");
        }
    }

    // === BUSINESS RULES VALIDATION ===

    /**
     * Validates if user can modify the service order
     */
    public boolean canUserModifyOrder(com.samarama.bicycle.api.model.ServiceOrder order, String userEmail) {
        // Check ownership
        if (!order.getClient().getEmail().equals(userEmail)) {
            return false;
        }

        // Check if order can be cancelled (our business rule for modification)
        return order.canBeCancelled();
    }

    /**
     * Validates if service can be started
     */
    public ValidationResult validateServiceStart(com.samarama.bicycle.api.model.ServiceOrder order) {
        List<String> errors = new ArrayList<>();

        if (!order.canStartService()) {
            errors.add("Serwis można rozpocząć tylko w statusie CONFIRMED lub PICKED_UP");
        }

        if (order.getServiceStartDate() != null) {
            errors.add("Serwis już został rozpoczęty");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates if service can be completed
     */
    public ValidationResult validateServiceCompletion(com.samarama.bicycle.api.model.ServiceOrder order) {
        List<String> errors = new ArrayList<>();

        if (!order.isServiceInProgress()) {
            errors.add("Serwis można zakończyć tylko w statusie IN_SERVICE");
        }

        if (order.getServiceCompletionDate() != null) {
            errors.add("Serwis już został zakończony");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // === UTILITY METHODS ===

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) return false;

        // Remove all non-digits
        String digitsOnly = phone.replaceAll("\\D", "");

        // Check if it's a valid Polish phone number (9 digits) or international format
        return digitsOnly.length() >= 9 && digitsOnly.length() <= 15;
    }

    // === RESULT CLASSES ===

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

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult failed(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failed(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    public static class SlotValidationResult {
        private final boolean available;
        private final String message;
        private final Map<String, Object> additionalInfo;

        public SlotValidationResult(boolean available, String message, Map<String, Object> additionalInfo) {
            this.available = available;
            this.message = message;
            this.additionalInfo = additionalInfo != null ? additionalInfo : Map.of();
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }

        public static SlotValidationResult success() {
            return new SlotValidationResult(true, "Sloty dostępne", Map.of());
        }

        public static SlotValidationResult failed(String message, Map<String, Object> additionalInfo) {
            return new SlotValidationResult(false, message, additionalInfo);
        }
    }
}