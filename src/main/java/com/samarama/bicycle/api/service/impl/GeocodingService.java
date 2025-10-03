package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.mapDto.BoundsDto;
import com.samarama.bicycle.api.dto.mapDto.CitySuggestionDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "BikeServiceApp/1.0";

    /**
     * Zwraca pełne dane granic miasta (boundingbox)
     */
    @Cacheable("cityCoordinates")
    public BoundsDto getCityBounds(String cityName) {
        try {
            String url = String.format("%s?city=%s&country=Poland&format=json&limit=1",
                    NOMINATIM_URL, URLEncoder.encode(cityName, "UTF-8"));

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, NominatimResponse[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                NominatimResponse result = response.getBody()[0];
                return createBoundsFromNominatim(result);
            }
        } catch (Exception e) {
            log.error("Error geocoding city: {}", cityName, e);
        }
        return null;
    }

    /**
     * Autocomplete - zwraca sugestie miast na podstawie wpisanych liter
     * Minimalnie 3 litery wymagane
     */
    @Cacheable(value = "citySuggestions", key = "#query")
    public List<CitySuggestionDto> searchCities(String query) {
        if (query == null || query.trim().length() < 3) {
            return Collections.emptyList();
        }

        try {
            // Wyszukiwanie z filtrowaniem tylko na miasta/miejscowości w Polsce
            String url = String.format(
                    "%s?q=%s&country=Poland&format=json&limit=10&addressdetails=1&featuretype=settlement",
                    NOMINATIM_URL,
                    URLEncoder.encode(query.trim(), "UTF-8"));

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, NominatimResponse[].class);

            if (response.getBody() != null) {
                return Arrays.stream(response.getBody())
                        .filter(this::isCityOrTown) // Filtrujemy tylko miasta
                        .map(this::mapToSuggestion)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error searching cities with query: {}", query, e);
        }

        return Collections.emptyList();
    }

    /**
     * Filtruje wyniki - zostawia tylko miasta i miejscowości
     */
    private boolean isCityOrTown(NominatimResponse response) {
        if (response.getType() == null) {
            return false;
        }
        String type = response.getType().toLowerCase();
        return type.equals("city") ||
                type.equals("town") ||
                type.equals("village") ||
                type.equals("municipality") ||
                type.equals("administrative");
    }

    /**
     * Mapuje odpowiedź Nominatim na DTO do autocomplete
     */
    private CitySuggestionDto mapToSuggestion(NominatimResponse response) {
        return new CitySuggestionDto(
                extractCityName(response),
                response.getDisplay_name(),
                Double.parseDouble(response.getLat()),
                Double.parseDouble(response.getLon()),
                response.getType());
    }

    /**
     * Wyciąga nazwę miasta z odpowiedzi
     */
    private String extractCityName(NominatimResponse response) {
        // Próbujemy wyciągnąć nazwę z display_name (pierwsza część przed przecinkiem)
        String displayName = response.getDisplay_name();
        if (displayName != null && displayName.contains(",")) {
            return displayName.split(",")[0].trim();
        }
        return displayName;
    }

    private BoundsDto createBoundsFromNominatim(NominatimResponse result) {
        return BoundsDto.builder()
                .sw(BoundsDto.PointDto.builder()
                        .latitude(Double.parseDouble(result.boundingbox[0]))
                        .longitude(Double.parseDouble(result.boundingbox[2]))
                        .build())
                .ne(BoundsDto.PointDto.builder()
                        .latitude(Double.parseDouble(result.boundingbox[1]))
                        .longitude(Double.parseDouble(result.boundingbox[3]))
                        .build())
                .center(BoundsDto.PointDto.builder()
                        .latitude(Double.parseDouble(result.lat))
                        .longitude(Double.parseDouble(result.lon))
                        .build())
                .zoom(12)
                .build();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        return headers;
    }

    @Data
    static class NominatimResponse {
        private String lat;
        private String lon;
        private String[] boundingbox;
        private String display_name; // np. "Warszawa, województwo mazowieckie, Polska"
        private String type; // np. "city", "town", "village"
    }
}