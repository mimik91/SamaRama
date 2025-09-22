package com.samarama.bicycle.api.service.helper.csvReader;

import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Komponent odpowiedzialny za parsowanie danych z CSV do obiektów BikeService
 */
@Component
public class BikeServiceCsvParser {

    private static final Logger logger = Logger.getLogger(BikeServiceCsvParser.class.getName());

    private final BikeServiceRepository bikeServiceRepository;
    private final AddressParser addressParser;
    private final CoordinateParser coordinateParser;
    private final PhoneNumberParser phoneNumberParser;

    @Autowired
    public BikeServiceCsvParser(BikeServiceRepository bikeServiceRepository,
                                AddressParser addressParser,
                                CoordinateParser coordinateParser,
                                PhoneNumberParser phoneNumberParser) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.addressParser = addressParser;
        this.coordinateParser = coordinateParser;
        this.phoneNumberParser = phoneNumberParser;
    }

    /**
     * Parsuje wiersz CSV do obiektu BikeService
     * Oczekiwany format: nazwa,adres,telefon,latitude,longitude,cena
     */
    public BikeService parseCsvRow(String[] fields, int lineNumber) throws Exception {
        // Loguj zawartość wiersza dla debugowania
        logger.info("Parsowanie wiersza " + lineNumber + ": " + Arrays.toString(fields));

        if (fields.length < 6) {
            throw new Exception("Nieprawidłowa liczba kolumn (oczekiwano 6: nazwa, adres, numer telefonu, latitude, longitude, cena), otrzymano: " + fields.length);
        }

        String name = fields[0].trim();
        String address = fields[1].trim();
        String phoneStr = fields[2].trim();
        String latStr = fields[3].trim();
        String lngStr = fields[4].trim();
        String cenaStr = fields[5].trim();

        // Walidacja obowiązkowych pól
        validateRequiredFields(name, address);

        // Sprawdź czy nazwa już istnieje
        if (bikeServiceRepository.existsByNameIgnoreCase(name)) {
            throw new Exception("Serwis o nazwie '" + name + "' już istnieje");
        }

        BikeService service = new BikeService();
        service.setName(name);

        // Parsowanie adresu
        addressParser.parseAddress(service, address);

        // Parsowanie telefonu
        if (!phoneStr.isEmpty()) {
            service.setPhoneNumber(phoneNumberParser.parsePhoneNumber(phoneStr));
        }

        // Parsowanie współrzędnych
        if (!latStr.isEmpty() && !lngStr.isEmpty()) {
            try {
                coordinateParser.parseCoordinates(service, latStr, lngStr);
            } catch (Exception e) {
                logger.warning("Błąd parsowania współrzędnych: " + e.getMessage());
                // Nie przerywaj parsowania z powodu błędnych współrzędnych
            }
        }

        // Parsowanie ceny transportu
        if (!cenaStr.isEmpty()) {
            service.setTransportCost(parseTransportCost(cenaStr));
        } else {
            service.setTransportCost(BigDecimal.ZERO);
        }

        logger.info("Pomyślnie sparsowano serwis: " + service.getName() +
                " - tel: " + service.getPhoneNumber() +
                " - cena: " + service.getTransportCost() +
                " - koordynaty: " + service.getLatitude() + "," + service.getLongitude());

        return service;
    }

    /**
     * Walidacja obowiązkowych pól
     */
    private void validateRequiredFields(String name, String address) throws Exception {
        if (name.isEmpty()) {
            throw new Exception("Nazwa serwisu jest wymagana");
        }

        if (address.isEmpty()) {
            throw new Exception("Adres serwisu jest wymagany");
        }
    }

    /**
     * Parsuje cenę transportu z różnych formatów
     */
    private BigDecimal parseTransportCost(String cenaStr) {
        try {
            // Obsługa różnych formatów liczb
            if (cenaStr.matches("^[0-9]+$")) {
                // Zwykła liczba całkowita
                return new BigDecimal(cenaStr);
            } else if (cenaStr.matches("^[0-9]+\\.[0-9]+$")) {
                // Liczba dziesiętna
                return new BigDecimal(cenaStr);
            } else if (cenaStr.matches("^[0-9]+,[0-9]+$")) {
                // Liczba z przecinkiem jako separatorem dziesiętnym
                String normalizedPrice = cenaStr.replace(",", ".");
                return new BigDecimal(normalizedPrice);
            } else {
                // Spróbuj usunąć znaki niebędące cyframi
                String cleanPrice = cenaStr.replaceAll("[^0-9.,]", "").replace(",", ".");
                if (!cleanPrice.isEmpty()) {
                    return new BigDecimal(cleanPrice);
                } else {
                    return BigDecimal.ZERO;
                }
            }
        } catch (NumberFormatException e) {
            logger.warning("Nie udało się sparsować ceny: " + cenaStr + ", ustawiam 0");
            return BigDecimal.ZERO;
        }
    }
}