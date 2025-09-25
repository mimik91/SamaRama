package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.service.BikeRepairCoverageService;
import com.samarama.bicycle.api.service.BikeServiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bike-services")
public class BikeServiceController {

    private final BikeServiceService bikeServiceService;
    private final BikeRepairCoverageService bikeRepairCoverageService;

    @Autowired
    public BikeServiceController(BikeServiceService bikeServiceService, BikeRepairCoverageService bikeRepairCoverageService) {
        this.bikeServiceService = bikeServiceService;
        this.bikeRepairCoverageService = bikeRepairCoverageService;
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

    /**
     * Pobierz szczegółowe informacje o swoim zarejestrowanym serwisie rowerowym
     */
    @GetMapping("/my-data")
    public ResponseEntity<?> getMyBikeServiceRegisteredDetails(Authentication authentication) {
        return bikeServiceService.getMyBikeServiceRegisteredDetails(authentication.getName());
    }

    /**
     * Zarejestruj nowy serwis rowerowy
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerMyBikeService(
            @Valid @RequestBody BikeServiceRegisteredDto bikeServiceRegisteredDto) {
        return bikeServiceService.registerMyBikeService(bikeServiceRegisteredDto);
    }

    /**
     * Zaktualizuj dane swojego zarejestrowanego serwisu rowerowego
     */
    @PutMapping("/edit-data")
    public ResponseEntity<?> updateMyBikeServiceRegistered(
            @Valid @RequestBody BikeServiceRegisteredDto bikeServiceRegisteredDto,
            Authentication authentication) {
        return bikeServiceService.updateMyBikeServiceRegistered(bikeServiceRegisteredDto, authentication.getName());
    }

    // ==========================================
    // BIKE REPAIR COVERAGE ENDPOINTS
    // ==========================================

    /**
     * Pobierz wszystkie dostępne pokrycia napraw pogrupowane po kategoriach
     * Endpoint publiczny do wyświetlania dostępnych usług
     */
    @GetMapping("/repair-coverage/all")
    public ResponseEntity<BikeRepairCoverageMapDto> getAllRepairCoverages() {
        BikeRepairCoverageMapDto coverages = bikeRepairCoverageService.getAllRepairCoverages();
        return ResponseEntity.ok(coverages);
    }

    /**
     * Pobierz pokrycia napraw przypisane do aktualnie zalogowanego użytkownika serwisu
     * oraz zaktualizuj pokrycia napraw dla tego użytkownika
     */
    @GetMapping("/repair-coverage/my")
    public ResponseEntity<?> getMyRepairCoverages(Authentication authentication) {
        return bikeRepairCoverageService.getMyRepairCoverages(authentication.getName());
    }

//    @PutMapping("/repair-coverage/assign/{bikeServiceId}")
//    public ResponseEntity<?> assignMyRepairCoverages(
//            @PathVariable Long bikeServiceId,
//            @RequestBody BikeRepairCoverageMapDto coverages) {
//        return bikeRepairCoverageService.assignMyRepairCoverages(bikeServiceId, coverages);
//    }


    @PutMapping("/repair-coverage/assign/{bikeServiceId}")
    public ResponseEntity<?> assignFlexibleRepairCoverages(
            @PathVariable Long bikeServiceId,
            @RequestBody ServiceCoverageAssignmentDto coverageAssignment) {
        return bikeRepairCoverageService.assignMyRepairCoverages(bikeServiceId, coverageAssignment);
    }

    @GetMapping("/check-suffix")
    public ResponseEntity<Boolean> checkSuffixAvailability(@RequestParam String suffix) {
        boolean isTaken = bikeServiceService.isSuffixTaken(suffix);
        return new ResponseEntity<>(isTaken, HttpStatus.OK);
    }

}