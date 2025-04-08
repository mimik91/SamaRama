package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BicyclePhoto;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicyclePhotoRepository;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.BicycleService;
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BicycleServiceImpl implements BicycleService {
    private final BicycleRepository bicycleRepository;
    private final BicyclePhotoRepository bicyclePhotoRepository;
    private final UserRepository userRepository;

    public BicycleServiceImpl(BicycleRepository bicycleRepository,
                              BicyclePhotoRepository bicyclePhotoRepository,
                              UserRepository userRepository) {
        this.bicycleRepository = bicycleRepository;
        this.bicyclePhotoRepository = bicyclePhotoRepository;
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

            // Sprawdź, czy rower już ma zdjęcie
            BicyclePhoto bicyclePhoto = bicycle.getPhoto();

            if (bicyclePhoto == null) {
                // Jeśli nie ma, utwórz nowe
                bicyclePhoto = new BicyclePhoto();
                bicyclePhoto.setBicycle(bicycle);
                bicycle.setPhoto(bicyclePhoto);
            }

            bicyclePhoto.setPhotoFromFile(photoData, contentType, photo.getSize());
            System.out.println("w8");

            bicyclePhotoRepository.save(bicyclePhoto);
            return ResponseEntity.ok(Map.of(
                    "message", "Photo uploaded successfully",
                    "bicycleId", bicycle.getId()
            ));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Error processing photo: " + e.getMessage()
            ));
        }
    }

    @Override
    public ResponseEntity<?> getBicyclePhoto(Long id) {
        Optional<BicyclePhoto> photoOpt = bicyclePhotoRepository.findByBicycleId(id);

        if (photoOpt.isEmpty() || photoOpt.get().getPhotoData() == null) {
            return ResponseEntity.notFound().build();
        }

        BicyclePhoto photo = photoOpt.get();

        // Określ typ mediów na podstawie zapisanego contentType lub domyślnie JPEG
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (photo.getContentType() != null && !photo.getContentType().isEmpty()) {
            try {
                mediaType = MediaType.parseMediaType(photo.getContentType());
            } catch (Exception e) {
                // Jeśli nie można sparsować typu, zostajemy przy domyślnym
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(photo.getPhotoData());
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

    @Override
    @Transactional
    public ResponseEntity<?> deleteBicyclePhoto(Long id) {
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
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to delete this bicycle's photo"));
        }

        // Usuń zdjęcie, jeśli istnieje
        if (bicycle.getPhoto() != null) {
            bicyclePhotoRepository.delete(bicycle.getPhoto());
            bicycle.setPhoto(null);
            bicycleRepository.save(bicycle);
            return ResponseEntity.ok(Map.of("message", "Photo deleted successfully"));
        } else {
            return ResponseEntity.ok(Map.of("message", "No photo to delete"));
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}