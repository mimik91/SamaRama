package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BicycleDto;
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
    public ResponseEntity<List<Bicycle>> getUserBicycles() {
        List<Bicycle> bicycles = bicycleService.getUserBicycles();
        return ResponseEntity.ok(bicycles);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'SERVICE')")
    public ResponseEntity<?> addBicycle(@Valid @RequestBody BicycleDto bicycleDto) {
        // Get information about the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isClient = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_CLIENT"));

        boolean isService = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_SERVICE"));

        return bicycleService.addBicycle(bicycleDto, isClient, isService);
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> deleteBicycle(@PathVariable Long id) {
        return bicycleService.deleteBicycle(id);
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
}