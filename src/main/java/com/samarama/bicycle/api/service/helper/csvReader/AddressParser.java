package com.samarama.bicycle.api.service.helper.csvReader;

import com.samarama.bicycle.api.model.BikeService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Komponent odpowiedzialny za parsowanie adresów
 */
@Component
public class AddressParser {

    private static final Logger logger = Logger.getLogger(AddressParser.class.getName());

    /**
     * Parsuje adres w różnych formatach
     */
    public void parseAddress(BikeService service, String address) {
        try {
            logger.info("Parsowanie adresu: " + address);

            // Wzorce dla różnych formatów adresów
            String[] addressParts = address.split(",");

            if (addressParts.length >= 2) {
                // Format: "Ulica Numer, Miasto"
                String streetPart = addressParts[0].trim();
                String cityPart = addressParts[addressParts.length - 1].trim();

                parseStreetAndNumber(service, streetPart);
                service.setCity(extractCityName(cityPart));

            } else {
                // Brak przecinka - spróbuj wykryć miasto
                String detectedCity = detectCityFromAddress(address);
                if (detectedCity != null) {
                    service.setCity(detectedCity);
                    String streetPart = address.replace(detectedCity, "").trim();
                    parseStreetAndNumber(service, streetPart);
                } else {
                    // Nie udało się wykryć miasta
                    parseStreetAndNumber(service, address);
                    service.setCity("Kraków"); // Domyślnie
                }
            }

            logger.info("Sparsowany adres - Ulica: " + service.getStreet() +
                    ", Numer: " + service.getBuilding() +
                    ", Miasto: " + service.getCity());

        } catch (Exception e) {
            logger.warning("Błąd parsowania adresu '" + address + "': " + e.getMessage());
            // W przypadku błędu, ustaw przynajmniej podstawowe dane
            service.setStreet(address);
            service.setCity("Kraków");
        }
    }

    /**
     * Parsuje ulicę i numer budynku
     */
    private void parseStreetAndNumber(BikeService service, String streetPart) {
        if (streetPart == null || streetPart.trim().isEmpty()) {
            return;
        }

        streetPart = streetPart.trim();

        // Wzorzec: ulica + numer (może zawierać literę)
        Pattern pattern = Pattern.compile("^(.+?)\\s+([0-9]+[A-Za-z]?(?:/[0-9]+[A-Za-z]?)?)$");
        Matcher matcher = pattern.matcher(streetPart);

        if (matcher.matches()) {
            String street = matcher.group(1).trim();
            String buildingNumber = matcher.group(2).trim();

            // Usuń przedrostki jak "ul.", "al."
            street = street.replaceAll("^(ul\\.|ulica|al\\.|aleja)\\s+", "");

            service.setStreet(street);

            // Obsługa numeru z mieszkaniem (12/4)
            if (buildingNumber.contains("/")) {
                String[] parts = buildingNumber.split("/");
                service.setBuilding(parts[0]);
                if (parts.length > 1) {
                    service.setFlat(parts[1]);
                }
            } else {
                service.setBuilding(buildingNumber);
            }
        } else {
            // Nie udało się wyodrębnić numeru
            service.setStreet(streetPart);
        }
    }

    /**
     * Wyciąga nazwę miasta (usuwa kod pocztowy)
     */
    private String extractCityName(String cityPart) {
        if (cityPart == null || cityPart.trim().isEmpty()) {
            return null;
        }

        // Usuń kod pocztowy (XX-XXX)
        String cleaned = cityPart.replaceAll("\\d{2}-\\d{3}", "").trim();

        if (!cleaned.isEmpty()) {
            return cleaned;
        }

        return cityPart.trim();
    }

    /**
     * Próbuje wykryć miasto na końcu adresu
     */
    private String detectCityFromAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        String lowerAddress = address.toLowerCase();

        // Lista znanych miast w województwie małopolskim i okolicznych
        String[] cities = {
                "kraków", "krakow", "wieliczka", "skawina", "niepołomice", "niepolomice",
                "myślenice", "myslenice", "chrzastowice", "zabierzów", "zabierzow",
                "zielonki", "michałowice", "michalowice", "liszki", "czernichów", "czernichow",
                "mogilany", "świątniki górne", "swiatniki gorne", "krzeszowice",
                "warszawa", "gdańsk", "gdansk", "wrocław", "wroclaw", "poznań", "poznan",
                "łódź", "lodz", "katowice", "lublin", "białystok", "bialystok",
                "bydgoszcz", "toruń", "torun", "rzeszów", "rzeszow"
        };

        for (String city : cities) {
            if (lowerAddress.endsWith(" " + city) || lowerAddress.equals(city)) {
                // Zwróć z poprawną wielkością liter (pierwsza wielka)
                return city.substring(0, 1).toUpperCase() + city.substring(1);
            }
        }

        return null;
    }
}