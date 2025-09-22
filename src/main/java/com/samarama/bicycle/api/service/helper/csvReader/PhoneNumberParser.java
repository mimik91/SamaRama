package com.samarama.bicycle.api.service.helper.csvReader;

import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Komponent odpowiedzialny za parsowanie numerów telefonów
 */
@Component
public class PhoneNumberParser {

    private static final Logger logger = Logger.getLogger(PhoneNumberParser.class.getName());

    /**
     * Czyści numer telefonu z różnych formatów
     */
    public String parsePhoneNumber(String phoneStr) {
        if (phoneStr == null || phoneStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Sprawdź czy to liczba (może być w formacie naukowym)
            if (phoneStr.matches("^[0-9]+$")) {
                // Zwykła liczba
                return phoneStr;
            } else if (phoneStr.matches("^[0-9.]+E[0-9]+$")) {
                // Format naukowy (np. 1.22961885E8)
                double phoneNum = Double.parseDouble(phoneStr);
                return String.format("%.0f", phoneNum);
            } else {
                return cleanPhoneNumber(phoneStr);
            }
        } catch (NumberFormatException e) {
            logger.warning("Nie udało się sparsować telefonu: " + phoneStr);
            return phoneStr; // Zapisz oryginalną wartość
        }
    }

    /**
     * Czyści numer telefonu ze znaków specjalnych
     */
    private String cleanPhoneNumber(String phoneStr) {
        if (phoneStr == null || phoneStr.trim().isEmpty()) {
            return null;
        }

        // Usuń białe znaki i znaki specjalne oprócz cyfr i +
        String cleaned = phoneStr.replaceAll("[\\s\\-\\(\\)]", "");

        // Sprawdź czy to sensowny numer telefonu
        if (cleaned.matches("^\\+?[0-9]{9,15}$")) {
            return cleaned;
        }

        return phoneStr; // Zwróć oryginalną wartość jeśli nie pasuje do wzorca
    }
}