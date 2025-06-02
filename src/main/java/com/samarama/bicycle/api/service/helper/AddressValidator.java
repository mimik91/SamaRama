package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.AddressDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper do walidacji adresów
 */
@Component
public class AddressValidator {

    // Wzorce do walidacji
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^\\d{2}-\\d{3}$");
    private static final Pattern BUILDING_NUMBER_PATTERN = Pattern.compile("^[0-9]+[A-Za-z]?$");
    private static final Pattern APARTMENT_NUMBER_PATTERN = Pattern.compile("^[0-9]+[A-Za-z]?$");

    /**
     * Waliduje kompletność danych adresowych
     */
    public ValidationResult validateAddressData(AddressDto addressDto) {
        List<String> errors = new ArrayList<>();

        // Pola wymagane
        if (isBlank(addressDto.street())) {
            errors.add("Ulica jest wymagana");
        }

        if (isBlank(addressDto.buildingNumber())) {
            errors.add("Numer budynku jest wymagany");
        }

        if (isBlank(addressDto.city())) {
            errors.add("Miasto jest wymagane");
        }

        // Walidacja formatów
        if (!isBlank(addressDto.buildingNumber()) && !BUILDING_NUMBER_PATTERN.matcher(addressDto.buildingNumber()).matches()) {
            errors.add("Nieprawidłowy format numeru budynku");
        }

        if (!isBlank(addressDto.apartmentNumber()) && !APARTMENT_NUMBER_PATTERN.matcher(addressDto.apartmentNumber()).matches()) {
            errors.add("Nieprawidłowy format numeru mieszkania");
        }

        if (!isBlank(addressDto.postalCode()) && !POSTAL_CODE_PATTERN.matcher(addressDto.postalCode()).matches()) {
            errors.add("Nieprawidłowy format kodu pocztowego (wymagany format: XX-XXX)");
        }

        // Walidacja długości
        errors.addAll(validateFieldLengths(addressDto));

        // Walidacja współrzędnych geograficznych
        errors.addAll(validateCoordinates(addressDto));

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Waliduje czy adres ma wymagane pola dla zamówień
     */
    public ValidationResult validateForOrder(AddressDto addressDto) {
        List<String> errors = new ArrayList<>();

        if (isBlank(addressDto.street())) {
            errors.add("Ulica jest wymagana dla zamówienia");
        }

        if (isBlank(addressDto.buildingNumber())) {
            errors.add("Numer budynku jest wymagany dla zamówienia");
        }

        if (isBlank(addressDto.city())) {
            errors.add("Miasto jest wymagane dla zamówienia");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Sprawdza czy adres jest kompletny (ma wszystkie podstawowe dane)
     */
    public boolean isComplete(AddressDto addressDto) {
        return !isBlank(addressDto.street()) &&
                !isBlank(addressDto.buildingNumber()) &&
                !isBlank(addressDto.city());
    }

    /**
     * Sprawdza czy dwa adresy są podobne (ta sama ulica, numer, miasto)
     */
    public boolean areSimilar(AddressDto address1, AddressDto address2) {
        return normalizeString(address1.street()).equals(normalizeString(address2.street())) &&
                normalizeString(address1.buildingNumber()).equals(normalizeString(address2.buildingNumber())) &&
                normalizeString(address1.city()).equals(normalizeString(address2.city()));
    }

    /**
     * Tworzy pełny adres jako string
     */
    public String formatFullAddress(AddressDto addressDto) {
        StringBuilder sb = new StringBuilder();

        if (!isBlank(addressDto.street())) {
            sb.append(addressDto.street());
        }

        if (!isBlank(addressDto.buildingNumber())) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(addressDto.buildingNumber());
        }

        if (!isBlank(addressDto.apartmentNumber())) {
            sb.append("/").append(addressDto.apartmentNumber());
        }

        if (!isBlank(addressDto.city())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(addressDto.city());
        }

        if (!isBlank(addressDto.postalCode())) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(addressDto.postalCode());
        }

        return sb.toString();
    }

    // === METODY POMOCNICZE ===

    private List<String> validateFieldLengths(AddressDto addressDto) {
        List<String> errors = new ArrayList<>();

        if (addressDto.street() != null && addressDto.street().length() > 255) {
            errors.add("Nazwa ulicy jest zbyt długa (maksymalnie 255 znaków)");
        }

        if (addressDto.buildingNumber() != null && addressDto.buildingNumber().length() > 20) {
            errors.add("Numer budynku jest zbyt długi (maksymalnie 20 znaków)");
        }

        if (addressDto.apartmentNumber() != null && addressDto.apartmentNumber().length() > 20) {
            errors.add("Numer mieszkania jest zbyt długi (maksymalnie 20 znaków)");
        }

        if (addressDto.city() != null && addressDto.city().length() > 100) {
            errors.add("Nazwa miasta jest zbyt długa (maksymalnie 100 znaków)");
        }

        if (addressDto.postalCode() != null && addressDto.postalCode().length() > 10) {
            errors.add("Kod pocztowy jest zbyt długi (maksymalnie 10 znaków)");
        }

        if (addressDto.name() != null && addressDto.name().length() > 100) {
            errors.add("Nazwa adresu jest zbyt długa (maksymalnie 100 znaków)");
        }

        if (addressDto.transportNotes() != null && addressDto.transportNotes().length() > 500) {
            errors.add("Notatki transportowe są zbyt długie (maksymalnie 500 znaków)");
        }

        return errors;
    }

    private List<String> validateCoordinates(AddressDto addressDto) {
        List<String> errors = new ArrayList<>();

        if (addressDto.latitude() != null) {
            if (addressDto.latitude() < -90 || addressDto.latitude() > 90) {
                errors.add("Szerokość geograficzna musi być między -90 a 90");
            }
        }

        if (addressDto.longitude() != null) {
            if (addressDto.longitude() < -180 || addressDto.longitude() > 180) {
                errors.add("Długość geograficzna musi być między -180 a 180");
            }
        }

        // Jeśli podano jedną współrzędną, druga też powinna być podana
        if ((addressDto.latitude() != null && addressDto.longitude() == null) ||
                (addressDto.latitude() == null && addressDto.longitude() != null)) {
            errors.add("Należy podać obie współrzędne geograficzne lub żadną");
        }

        return errors;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String normalizeString(String str) {
        return str != null ? str.trim().toLowerCase() : "";
    }

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

        public static ValidationResult valid() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, List.of(error));
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }
}