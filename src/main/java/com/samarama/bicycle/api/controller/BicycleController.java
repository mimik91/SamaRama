package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.dto.IncompleteBikeDto;
import com.samarama.bicycle.api.dto.IncompleteBikeResponseDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.service.BicycleService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bicycles")
public class BicycleController {
    private final BicycleService bicycleService;

    public BicycleController(BicycleService bicycleService) {
        this.bicycleService = bicycleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<IncompleteBikeResponseDto>> getUserBikes() {
        List<IncompleteBikeResponseDto> bikes = bicycleService.getAllUserBikes();
        return ResponseEntity.ok(bikes);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'SERVICE')")
    public ResponseEntity<?> addBike(@Valid @RequestBody BicycleDto bicycleDto) {
        // Get information about the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isClient = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_CLIENT"));

        boolean isService = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_SERVICE"));

        // Dla klienta zawsze tworzymy IncompleteBike
        if (isClient) {
            IncompleteBikeDto incompleteBikeDto = new IncompleteBikeDto(
                    bicycleDto.brand(),
                    bicycleDto.model(),
                    bicycleDto.type(),
                    bicycleDto.frameMaterial(),
                    bicycleDto.productionDate()
            );
            return bicycleService.addIncompleteBike(incompleteBikeDto);
        }
        // Dla serwisu, używamy starej metody (która rozróżnia czy tworzymy Bicycle czy IncompleteBike)
        else if (isService) {
            return bicycleService.addBicycle(bicycleDto, isClient, isService);
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request"));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> uploadBicyclePhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo) {

        return bicycleService.uploadBicyclePhoto(id, photo);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<?> getBicyclePhoto(@PathVariable Long id) {
        return bicycleService.getBicyclePhoto(id);
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> deleteBicyclePhoto(
            @PathVariable Long id,
            @RequestParam(value = "isComplete", defaultValue = "true") boolean isComplete) {

        if (isComplete) {
            return bicycleService.deleteBicyclePhoto(id);
        } else {
            return bicycleService.deleteIncompleteBikePhoto(id);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> deleteBicycle(
            @PathVariable Long id,
            @RequestParam(value = "isComplete", defaultValue = "true") boolean isComplete) {

        if (isComplete) {
            return bicycleService.deleteBicycle(id);
        } else {
            return bicycleService.deleteIncompleteBike(id);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<IncompleteBikeResponseDto> getBicycleById(
            @PathVariable Long id,
            @RequestParam(value = "isComplete", required = false) Boolean isComplete) {

        // Jeśli parametr isComplete nie jest podany, spróbujmy znaleźć rower w obu typach
        if (isComplete == null) {
            // Najpierw sprawdzamy Bicycle (kompletny)
            ResponseEntity<IncompleteBikeResponseDto> response = bicycleService.getBikeById(id, true);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response;
            }

            // Jeśli nie znaleziono w Bicycle, sprawdź IncompleteBike
            return bicycleService.getBikeById(id, false);
        }

        // Jeśli parametr isComplete jest podany, użyj jego wartości
        return bicycleService.getBikeById(id, isComplete);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> updateBicycle(
            @PathVariable Long id,
            @Valid @RequestBody BicycleDto bicycleDto,
            @RequestParam(value = "isComplete", defaultValue = "true") boolean isComplete) {

        if (isComplete) {
            return bicycleService.updateBicycle(id, bicycleDto);
        } else {
            IncompleteBikeDto incompleteBikeDto = new IncompleteBikeDto(
                    bicycleDto.brand(),
                    bicycleDto.model(),
                    bicycleDto.type(),
                    bicycleDto.frameMaterial(),
                    bicycleDto.productionDate()
            );
            return bicycleService.updateIncompleteBike(id, incompleteBikeDto);
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> searchBicycleByFrameNumber(@RequestParam String frameNumber) {
        return bicycleService.searchBicycleByFrameNumber(frameNumber);
    }

    @PatchMapping("/{id}/frame-number")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> updateFrameNumber(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String frameNumber = payload.get("frameNumber");
        return bicycleService.updateFrameNumber(id, frameNumber);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> convertToComplete(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String frameNumber = payload.get("frameNumber");
        return bicycleService.convertToComplete(id, frameNumber);
    }
}