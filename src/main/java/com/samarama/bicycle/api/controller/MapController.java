package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.mapDto.*;
import com.samarama.bicycle.api.service.impl.GeocodingService;
import com.samarama.bicycle.api.service.impl.MapServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapServiceImpl mapService;
    private final GeocodingService geocodingService;

    @Autowired
    public MapController(MapServiceImpl mapService, GeocodingService geocodingService) {
        this.mapService = mapService;
        this.geocodingService = geocodingService;
    }

    /**
     * Główny endpoint mapy - podobny do Napravelo
     * Zwraca serwisy w zadanym obszarze geograficznym z paginacją i filtrowaniem
     *
     * Przykład: /api/map/services?bounds=49.5,19.0,50.5,20.0&page=0&perPage=20&city=Kraków
     */
    @PostMapping("/services")
    public ResponseEntity<MapServicesResponseDto> getMapServices(
            @RequestBody MapServicesRequestDto request,
            HttpServletRequest httpRequest) {


        MapServicesResponseDto response = mapService.getServicesInBounds(request);
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint do wyszukiwania serwisów po nazwie/mieście
     */
    @GetMapping("/search")
    public ResponseEntity<MapServicesResponseDto> searchServices(
            @RequestParam String query,
            @RequestParam(required = false) String bounds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestParam(defaultValue = "false") boolean verifiedOnly,
            HttpServletRequest httpRequest) {

        MapSearchRequestDto searchRequest = MapSearchRequestDto.builder()
                .query(query)
                .bounds(bounds)
                .page(page)
                .perPage(perPage)
                .verifiedOnly(verifiedOnly)
                .sessionId(httpRequest.getSession(true).getId())
                .build();

        MapServicesResponseDto response = mapService.searchServices(searchRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint do pobrania szczegółów serwisu z cache'owaniem
     */
    @GetMapping("/service/{id}")
    public ResponseEntity<BikeServiceDto> getServiceDetail(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String sessionId = httpRequest.getSession(true).getId();
        BikeServiceDto  service = mapService.getServiceDetail(id, sessionId);

        return service != null ? ResponseEntity.ok(service) : ResponseEntity.notFound().build();
    }

    /**
     * Endpoint do zapisywania statystyk/metryk (jak Napravelo)
     */
    @PostMapping("/analytics")
    public ResponseEntity<Void> trackAnalytics(@RequestBody AnalyticsEventDto event,
                                               HttpServletRequest httpRequest) {
        event.setSessionId(httpRequest.getSession().getId());
        event.setTimestamp(System.currentTimeMillis());

        mapService.trackAnalyticsEvent(event);
        return ResponseEntity.ok().build();
    }

    /**
     * Autocomplete endpoint - podpowiedzi miast
     * GET /api/geocoding/cities/search?q=War
     * Zwraca sugestie po wpisaniu min. 3 liter
     */
    @CrossOrigin(origins = "*")
    @GetMapping("/cities/search")
    public ResponseEntity<List<CitySuggestionDto>> searchCities(
            @RequestParam("q") String query) {

        if (query == null || query.trim().length() < 3) {
            return ResponseEntity.ok(List.of());
        }


        List<CitySuggestionDto> suggestions = geocodingService.searchCities(query);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Pobiera pełne dane granic miasta (boundingbox)
     * GET /api/geocoding/cities/Warszawa/bounds
     */
    @GetMapping("/cities/{cityName}/bounds")
    public ResponseEntity<BoundsDto> getCityBounds(
            @PathVariable String cityName) {

        BoundsDto bounds = geocodingService.getCityBounds(cityName);

        if (bounds == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(bounds);
    }

    @GetMapping("/services/autocomplete")
    public ResponseEntity<MapServicesResponseDto> searchServicesAutocomplete(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "true") boolean registeredFirst) {

        if (query == null || query.trim().length() < 3) {
            return ResponseEntity.ok(MapServicesResponseDto.builder()
                    .data(Collections.emptyList())
                    .total(0)
                    .build());
        }

        MapServicesResponseDto response = mapService.searchServicesByName(
                query, registeredFirst, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pins/clustered")
    public ResponseEntity<MapPinsResponseDto> getClusteredPins(
            @RequestParam(required = false) String bounds,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(defaultValue = "100") int perPage,
            @RequestParam(required = false) String city,
            HttpServletRequest httpRequest) {

        MapPinsRequestDto request = MapPinsRequestDto.builder()
                .bounds(bounds)
                .zoom(zoom)
                .perPage(perPage)
                .city(city)
                .sessionId(httpRequest.getSession(true).getId())
                .build();

        MapPinsResponseDto response = mapService.getClusteredPins(request);
        return ResponseEntity.ok(response);
    }
}