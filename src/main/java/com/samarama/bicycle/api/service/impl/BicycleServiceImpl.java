package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.BicycleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BicycleServiceImpl implements BicycleService {
    private final BicycleRepository bicycleRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BicycleServiceImpl(BicycleRepository bicycleRepository, UserRepository userRepository) {
        this.bicycleRepository = bicycleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Bicycle> getUserBicycles() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bicycleRepository.findByOwner(user);
    }

    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> addBicycle(BicycleDto bicycleDto, boolean isClient, boolean isService) {
        try {
            if (isService && bicycleDto.frameNumber() != null && !bicycleDto.frameNumber().isEmpty()) {
                if (bicycleRepository.existsByFrameNumber(bicycleDto.frameNumber())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Bicycle with this frame number already exists"));
                }
            } else if (isClient && bicycleDto.frameNumber() != null && !bicycleDto.frameNumber().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Clients cannot set frame number. This is reserved for service"));
            }

            Bicycle bicycle = new Bicycle();
            bicycle.setBrand(bicycleDto.brand());
            bicycle.setModel(bicycleDto.model());
            bicycle.setType(bicycleDto.type());
            bicycle.setFrameMaterial(bicycleDto.frameMaterial());
            bicycle.setProductionDate(bicycleDto.productionDate());
            bicycle.setCreatedAt(LocalDateTime.now());

            // Set frame number if service
            if (isService) {
                bicycle.setFrameNumber(bicycleDto.frameNumber());
            }

            // Set owner if client
            if (isClient) {
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                bicycle.setOwner(user);
            }

            // Save the bicycle
            Bicycle savedBicycle = bicycleRepository.save(bicycle);

            return ResponseEntity.ok(Map.of(
                    "message", "Bicycle added successfully",
                    "bicycleId", savedBicycle.getId(),
                    "frameNumber", isService && bicycleDto.frameNumber() != null ? bicycleDto.frameNumber() : ""
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error adding bicycle: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> uploadBicyclePhoto(Long id, MultipartFile photo) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();

        // Check if the bicycle belongs to the user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bicycle"));
        }

        // Check file size - max 1MB
        if (photo.getSize() > 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "Photo exceeds maximum size of 1MB"));
        }

        // Validate file type
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("message", "File must be an image"));
        }

        try {
            // Pobierz dane zdjęcia
            byte[] photoData = photo.getBytes();

            // Utwórz nowy obiekt Bicycle tylko do aktualizacji zdjęcia
            // Dzięki temu unikniemy problemów z innymi polami
            Bicycle photoUpdate = new Bicycle();
            photoUpdate.setId(bicycle.getId());
            photoUpdate.setPhoto(photoData);

            // Wykonaj częściową aktualizację za pomocą EntityManager
            entityManager.merge(photoUpdate);

            return ResponseEntity.ok(Map.of(
                    "message", "Photo uploaded successfully",
                    "bicycleId", bicycle.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Error processing photo: " + e.getMessage()
            ));
        }
    }

    @Override
    public ResponseEntity<?> getBicyclePhoto(Long id) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
        if (bicycleOpt.isEmpty() || bicycleOpt.get().getPhoto() == null) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bicycle.getPhoto());
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteBicycle(Long id) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);

        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();

        // Check if the bicycle belongs to the user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to delete this bicycle"));
        }

        bicycleRepository.delete(bicycle);
        return ResponseEntity.ok(Map.of("message", "Bicycle deleted successfully"));
    }

    @Override
    public ResponseEntity<Bicycle> getBicycleById(Long id) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);

        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();

        // Check if the bicycle belongs to the authenticated user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(bicycle);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateBicycle(Long id, BicycleDto bicycleDto) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);

        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bicycle bicycle = bicycleOpt.get();

        // Check if the bicycle belongs to the authenticated user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bicycle"));
        }

        // Nie aktualizuj numeru ramy - to może tylko serwis
        // bicycle.setFrameNumber(bicycleDto.frameNumber());

        bicycle.setBrand(bicycleDto.brand());
        bicycle.setModel(bicycleDto.model());
        bicycle.setType(bicycleDto.type());
        bicycle.setFrameMaterial(bicycleDto.frameMaterial());
        bicycle.setProductionDate(bicycleDto.productionDate());

        bicycleRepository.save(bicycle);

        return ResponseEntity.ok(Map.of("message", "Bicycle updated successfully"));
    }

    @Override
    public ResponseEntity<?> searchBicycleByFrameNumber(String frameNumber) {
        Optional<Bicycle> bicycle = bicycleRepository.findByFrameNumber(frameNumber);
        return bicycle.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateFrameNumber(Long id, String frameNumber) {
        if (frameNumber == null || frameNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Frame number is required"));
        }

        // Check if the frame number is already in use
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

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}