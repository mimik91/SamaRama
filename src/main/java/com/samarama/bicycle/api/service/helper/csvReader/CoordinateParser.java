package com.samarama.bicycle.api.service.helper.csvReader;

import com.samarama.bicycle.api.model.BikeService;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Komponent odpowiedzialny za parsowanie współrzędnych geograficznych
 */
@Component
public class CoordinateParser {

    private static final Logger logger = Logger.getLogger(CoordinateParser.class.getName());

    /**
     * Parsuje współrzędne geograficzne
     */
    public void parseCoordinates(BikeService service, String latStr, String lngStr) throws Exception {
        try {
            Double latitude = Double.parseDouble(latStr.trim());
            Double longitude = Double.parseDouble(lngStr.trim());

            // Walidacja zakresu współrzędnych
            if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                service.setLatitude(latitude);
                service.setLongitude(longitude);
                logger.info("Ustawiono współrzędne: " + latitude + ", " + longitude);
            } else {
                throw new Exception("Współrzędne poza dozwolonym zakresem (lat: " + latitude + ", lng: " + longitude + ")");
            }
        } catch (NumberFormatException e) {
            throw new Exception("Nieprawidłowy format współrzędnych geograficznych: lat='" + latStr + "', lng='" + lngStr + "'");
        }
    }
}