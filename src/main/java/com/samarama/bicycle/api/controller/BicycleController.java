package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bicycles")
public class BicycleController {
    private final BicycleRepository bicycleRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BicycleController(BicycleRepository bicycleRepository, UserRepository userRepository) {
        this.bicycleRepository = bicycleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Bicycle>> getUserBicycles() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Bicycle> bicycles = bicycleRepository.findByOwner(user);
        return ResponseEntity.ok(bicycles);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'SERVICE')")
    @Transactional
    public ResponseEntity<?> addBicycle(@Valid @RequestBody BicycleDto bicycleDto) {
        // Pobierz informacje o zalogowanym użytkowniku
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isClient = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_CLIENT"));

        boolean isService = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_SERVICE"));

        // Używamy natywnego SQL, aby mieć pełną kontrolę nad zapytaniem INSERT
        // i pominąć problematyczną kolumnę photo
        String sql = "INSERT INTO bicycles(brand, model, type, framematerial, framenumber, owner_id, createdat, productiondate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // Walidacja numeru ramy
            if (isService && bicycleDto.frameNumber() != null && !bicycleDto.frameNumber().isEmpty()) {
                if (bicycleRepository.existsByFrameNumber(bicycleDto.frameNumber())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Bicycle with this frame number already exists"));
                }
            } else if (isClient && bicycleDto.frameNumber() != null && !bicycleDto.frameNumber().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Clients cannot set frame number. This is reserved for service"));
            }

            // Znajdź identyfikator właściciela
            Long ownerId = null;
            if (isClient) {
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                ownerId = user.getId();
            }

            // Wykonaj zapytanie natywne z pominięciem kolumny photo
            Object[] params = new Object[] {
                    bicycleDto.brand(),
                    bicycleDto.model(),
                    bicycleDto.type(),
                    bicycleDto.frameMaterial(),
                    isService ? bicycleDto.frameNumber() : null,
                    ownerId,
                    Timestamp.valueOf(LocalDateTime.now()),
                    bicycleDto.productionDate() != null ? java.sql.Date.valueOf(bicycleDto.productionDate()) : null
            };

            // Wykonaj zapytanie i pobierz wygenerowany ID
            Query query = entityManager.createNativeQuery(sql);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }

            int rowsAffected = query.executeUpdate();

            if (rowsAffected > 0) {
                // Pobierz ID ostatnio wstawionego roweru
                Long bicycleId = (Long) entityManager.createNativeQuery(
                                "SELECT currval('bicycles_id_seq')")
                        .getSingleResult();

                return ResponseEntity.ok(Map.of(
                        "message", "Bicycle added successfully",
                        "bicycleId", bicycleId,
                        "frameNumber", isService && bicycleDto.frameNumber() != null ? bicycleDto.frameNumber() : ""
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Failed to add bicycle"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error adding bicycle: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> uploadBicyclePhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo) {

        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();

        // Sprawdź czy rower należy do użytkownika
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bicycle"));
        }

        // Sprawdź rozmiar pliku - max 1MB
        if (photo.getSize() > 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "Photo exceeds maximum size of 1MB"));
        }

        try {
            // Używamy natywnego SQL do aktualizacji tylko kolumny photo
            byte[] photoData = photo.getBytes();

            // Aktualizuj za pomocą natywnego zapytania SQL
            int updated = entityManager.createNativeQuery(
                            "UPDATE bicycles SET photo = ? WHERE id = ?")
                    .setParameter(1, photoData)
                    .setParameter(2, id)
                    .executeUpdate();

            if (updated > 0) {
                return ResponseEntity.ok(Map.of("message", "Photo uploaded successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Failed to update photo"));
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error processing photo: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<?> getBicyclePhoto(@PathVariable Long id) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
        if (bicycleOpt.isEmpty() || bicycleOpt.get().getPhoto() == null) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bicycle.getPhoto());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> deleteBicycle(@PathVariable Long id) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);

        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();

        // Sprawdź czy rower należy do użytkownika
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to delete this bicycle"));
        }

        bicycleRepository.delete(bicycle);
        return ResponseEntity.ok(Map.of("message", "Bicycle deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> searchBicycleByFrameNumber(@RequestParam String frameNumber) {
        Optional<Bicycle> bicycle = bicycleRepository.findByFrameNumber(frameNumber);
        return bicycle.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @PatchMapping("/{id}/frame-number")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> updateFrameNumber(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String frameNumber = payload.get("frameNumber");
        if (frameNumber == null || frameNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Frame number is required"));
        }

        // Sprawdź, czy numer ramy jest już używany
        if (bicycleRepository.existsByFrameNumber(frameNumber)) {
            return ResponseEntity.badRequest().body(Map.of("message", "This frame number is already in use"));
        }

        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();
        bicycle.setFrameNumber(frameNumber);
        bicycleRepository.save(bicycle);

        return ResponseEntity.ok(Map.of("message", "Frame number added successfully"));
    }
}