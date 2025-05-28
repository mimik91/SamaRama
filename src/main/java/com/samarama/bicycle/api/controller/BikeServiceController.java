package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.BikeServicePinDto;
import com.samarama.bicycle.api.service.BikeServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bike-services")
public class BikeServiceController {

    private final BikeServiceService bikeServiceService;

    @Autowired
    public BikeServiceController(BikeServiceService bikeServiceService) {
        this.bikeServiceService = bikeServiceService;
    }

    /**
     * Pobierz piny wszystkich serwisów rowerowych (tylko ID i współrzędne geograficzne)
     * Endpoint publiczny dla mapy serwisów
     */
    @GetMapping("/pins")
    public ResponseEntity<List<BikeServicePinDto>> getAllBikeServicePins() {
        List<BikeServicePinDto> pins = bikeServiceService.getAllBikeServicePins();
        return ResponseEntity.ok(pins);
    }

    /**
     * Pobierz szczegółowe informacje o serwisie rowerowym po ID
     * Endpoint publiczny dla szczegółów serwisu
     */
    @GetMapping("/{id}")
    public ResponseEntity<BikeServiceDto> getBikeServiceDetails(@PathVariable Long id) {
        Optional<BikeServiceDto> service = bikeServiceService.getBikeServiceDetails(id);
        return service.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}