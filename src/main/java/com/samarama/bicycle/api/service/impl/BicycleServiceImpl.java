package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.dto.IncompleteBikeDto;
import com.samarama.bicycle.api.dto.IncompleteBikeResponseDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BicyclePhoto;
import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicyclePhotoRepository;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.IncompleteBikeRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BicycleServiceImpl implements BicycleService {
    private final BicycleRepository bicycleRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final BicyclePhotoRepository bicyclePhotoRepository;
    private final UserRepository userRepository;

    public BicycleServiceImpl(
            BicycleRepository bicycleRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            BicyclePhotoRepository bicyclePhotoRepository,
            UserRepository userRepository
    ) {
        this.bicycleRepository = bicycleRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.bicyclePhotoRepository = bicyclePhotoRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<IncompleteBikeResponseDto> getAllUserBikes() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<IncompleteBikeResponseDto> result = new ArrayList<>();

        // Dodaj kompletne rowery (Bicycle)
        List<Bicycle> bicycles = bicycleRepository.findByOwner(user);
        result.addAll(bicycles.stream()
                .map(IncompleteBikeResponseDto::fromBicycleEntity)
                .collect(Collectors.toList()));

        // Dodaj niekompletne rowery (IncompleteBike)
        List<IncompleteBike> incompleteBikes = incompleteBikeRepository.findByOwner(user);
        result.addAll(incompleteBikes.stream()
                .map(IncompleteBikeResponseDto::fromEntity)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public List<IncompleteBike> getUserIncompleteBikes() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return incompleteBikeRepository.findByOwner(user);
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
    public ResponseEntity<Map<String, Object>> addIncompleteBike(IncompleteBikeDto incompleteBikeDto) {
        try {
            IncompleteBike incompleteBike = new IncompleteBike();
            incompleteBike.setBrand(incompleteBikeDto.brand());
            incompleteBike.setModel(incompleteBikeDto.model());
            incompleteBike.setType(incompleteBikeDto.type());
            incompleteBike.setFrameMaterial(incompleteBikeDto.frameMaterial());
            incompleteBike.setProductionDate(incompleteBikeDto.productionDate());
            incompleteBike.setCreatedAt(LocalDateTime.now());

            // Set owner
            String email = getCurrentUserEmail();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            incompleteBike.setOwner(user);

            // Save the incomplete bike
            IncompleteBike savedBike = incompleteBikeRepository.save(incompleteBike);

            return ResponseEntity.ok(Map.of(
                    "message", "Incomplete bike added successfully",
                    "bikeId", savedBike.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error adding incomplete bike: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> convertToComplete(Long incompleteBikeId, String frameNumber) {
        if (frameNumber == null || frameNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Frame number is required"));
        }

        // Check if the frame number is already in use
        if (bicycleRepository.existsByFrameNumber(frameNumber)) {
            return ResponseEntity.badRequest().body(Map.of("message", "This frame number is already in use"));
        }

        Optional<IncompleteBike> incompleteBikeOpt = incompleteBikeRepository.findById(incompleteBikeId);
        if (incompleteBikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike incompleteBike = incompleteBikeOpt.get();

        // Create a new Bicycle from the IncompleteBike
        Bicycle bicycle = new Bicycle();
        bicycle.setBrand(incompleteBike.getBrand());
        bicycle.setModel(incompleteBike.getModel());
        bicycle.setType(incompleteBike.getType());
        bicycle.setFrameMaterial(incompleteBike.getFrameMaterial());
        bicycle.setProductionDate(incompleteBike.getProductionDate());
        bicycle.setOwner(incompleteBike.getOwner());
        bicycle.setCreatedAt(incompleteBike.getCreatedAt());
        bicycle.setFrameNumber(frameNumber);

        // Handle photo if present
        BicyclePhoto photo = incompleteBike.getPhoto();
        if (photo != null) {
            photo.setBike(bicycle);
            bicycle.setPhoto(photo);
        }

        // Save the new Bicycle
        Bicycle savedBicycle = bicycleRepository.save(bicycle);

        // Delete the IncompleteBike
        incompleteBikeRepository.delete(incompleteBike);

        return ResponseEntity.ok(Map.of(
                "message", "Bike converted to complete successfully",
                "bicycleId", savedBicycle.getId(),
                "frameNumber", frameNumber
        ));
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

            // Dla klientów, zawsze tworzy IncompleteBike
            if (isClient) {
                IncompleteBike incompleteBike = new IncompleteBike();
                incompleteBike.setBrand(bicycleDto.brand());
                incompleteBike.setModel(bicycleDto.model());
                incompleteBike.setType(bicycleDto.type());
                incompleteBike.setFrameMaterial(bicycleDto.frameMaterial());
                incompleteBike.setProductionDate(bicycleDto.productionDate());
                incompleteBike.setCreatedAt(LocalDateTime.now());

                // Set owner
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                incompleteBike.setOwner(user);

                // Save the incomplete bike
                IncompleteBike savedBike = incompleteBikeRepository.save(incompleteBike);

                return ResponseEntity.ok(Map.of(
                        "message", "Incomplete bike added successfully",
                        "bikeId", savedBike.getId()
                ));
            }
            // Dla serwisu, tworzy od razu Bicycle jeśli podano numer ramy
            else if (isService) {
                Bicycle bicycle = new Bicycle();
                bicycle.setBrand(bicycleDto.brand());
                bicycle.setModel(bicycleDto.model());
                bicycle.setType(bicycleDto.type());
                bicycle.setFrameMaterial(bicycleDto.frameMaterial());
                bicycle.setProductionDate(bicycleDto.productionDate());
                bicycle.setCreatedAt(LocalDateTime.now());
                bicycle.setFrameNumber(bicycleDto.frameNumber());

                // Save the bicycle
                Bicycle savedBicycle = bicycleRepository.save(bicycle);

                return ResponseEntity.ok(Map.of(
                        "message", "Bicycle added successfully",
                        "bicycleId", savedBicycle.getId(),
                        "frameNumber", bicycleDto.frameNumber() != null ? bicycleDto.frameNumber() : ""
                ));
            }

            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error adding bicycle: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> uploadBicyclePhoto(Long id, MultipartFile photo) {
        try {
            // Najpierw sprawdź, czy istnieje niekompletny rower
            Optional<IncompleteBike> incompleteBikeOpt = incompleteBikeRepository.findById(id);
            if (incompleteBikeOpt.isPresent()) {
                IncompleteBike bike = incompleteBikeOpt.get();

                // Sprawdź uprawnienia
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bike"));
                }

                return uploadPhoto(photo, null, bike);
            }

            // Jeśli nie znaleziono niekompletnego roweru, sprawdź kompletny
            Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
            if (bicycleOpt.isPresent()) {
                Bicycle bicycle = bicycleOpt.get();

                // Sprawdź uprawnienia
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bicycle"));
                }

                return uploadPhoto(photo, bicycle, null);
            }

            // HACK: Dodanie dodatkowego zapytania synchronizującego - jeśli Hibernate nie zdążył zapisać roweru
            try {
                // Czekaj chwilę, aby dać szansę na zakończenie transakcji
                Thread.sleep(100);

                // Ponownie sprawdź, czy istnieje niekompletny rower
                incompleteBikeOpt = incompleteBikeRepository.findById(id);
                if (incompleteBikeOpt.isPresent()) {
                    IncompleteBike bike = incompleteBikeOpt.get();

                    // Sprawdź uprawnienia
                    String email = getCurrentUserEmail();
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bike"));
                    }

                    return uploadPhoto(photo, null, bike);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Error processing photo: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> uploadIncompleteBikePhoto(Long id, MultipartFile photo) {
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);
        if (bikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike bike = bikeOpt.get();

        // Check if the bike belongs to the user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bike"));
        }

        return uploadPhoto(photo, null, bike);
    }

    private ResponseEntity<?> uploadPhoto(MultipartFile photo, Bicycle bicycle, IncompleteBike incompleteBike) {
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
            // Get photo data
            byte[] photoData = photo.getBytes();

            // Wybierz odpowiedni rower do powiązania ze zdjęciem (bicycle ma priorytet)
            IncompleteBike bikeToUse = bicycle != null ? bicycle : incompleteBike;

            if (bikeToUse == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid request - no bike specified"));
            }

            // Zapewnij, że rower jest aktualizowany w bieżącej sesji Hibernate
            if (bicycle != null) {
                bicycle = bicycleRepository.save(bicycle);
            } else if (incompleteBike != null) {
                incompleteBike = incompleteBikeRepository.save(incompleteBike);
                bikeToUse = incompleteBike;
            }

            // Check if the bike already has a photo
            BicyclePhoto bicyclePhoto = null;

            Optional<BicyclePhoto> existingPhoto = bicyclePhotoRepository.findByBikeId(bikeToUse.getId());
            if (existingPhoto.isPresent()) {
                bicyclePhoto = existingPhoto.get();
            }

            if (bicyclePhoto == null) {
                // If no photo, create a new one
                bicyclePhoto = new BicyclePhoto();
                bicyclePhoto.setBike(bikeToUse);

                if (bicycle != null) {
                    bicycle.setPhoto(bicyclePhoto);
                } else if (incompleteBike != null) {
                    incompleteBike.setPhoto(bicyclePhoto);
                }
            }

            bicyclePhoto.setPhotoFromFile(photoData, contentType, photo.getSize());
            bicyclePhotoRepository.save(bicyclePhoto);

            // Upewnij się, że zmiany są zapisane i widoczne
            if (bicycle != null) {
                bicycleRepository.save(bicycle);
            } else if (incompleteBike != null) {
                incompleteBikeRepository.save(incompleteBike);
            }

            String entityType = bikeToUse instanceof Bicycle ? "bicycle" : "incomplete bike";
            Long entityId = bikeToUse.getId();

            return ResponseEntity.ok(Map.of(
                    "message", "Photo uploaded successfully",
                    "entityType", entityType,
                    "entityId", entityId
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
        // Najpierw sprawdź, czy istnieje zdjęcie dla niekompletnego roweru
        Optional<IncompleteBike> incompleteBikeOpt = incompleteBikeRepository.findById(id);
        if (incompleteBikeOpt.isPresent() && incompleteBikeOpt.get().getPhoto() != null) {
            IncompleteBike bike = incompleteBikeOpt.get();
            return getBikePhoto(bike);
        }

        // Jeśli nie znaleziono niekompletnego roweru lub nie ma zdjęcia, sprawdź kompletny rower
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);
        if (bicycleOpt.isPresent()) {
            Bicycle bicycle = bicycleOpt.get();
            return getBikePhoto(bicycle);
        }

        // Jeśli nie znaleziono żadnego roweru, zwróć 404
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<?> getIncompleteBikePhoto(Long id) {
        // Używamy nowego repozytorium dla zdjęć rowerów niekompletnych
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);

        if (bikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike bike = bikeOpt.get();
        return getBikePhoto(bike);
    }

    private ResponseEntity<?> getBikePhoto(IncompleteBike bike) {
        BicyclePhoto photo = bike.getPhoto();

        if (photo == null || photo.getPhotoData() == null) {
            return ResponseEntity.notFound().build();
        }

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
    @Transactional
    public ResponseEntity<?> deleteIncompleteBike(Long id) {
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);

        if (bikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike bike = bikeOpt.get();

        // Check if the bike belongs to the user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to delete this bike"));
        }

        incompleteBikeRepository.delete(bike);
        return ResponseEntity.ok(Map.of("message", "Incomplete bike deleted successfully"));
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
    public ResponseEntity<IncompleteBike> getIncompleteBikeById(Long id) {
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);

        if (bikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike bike = bikeOpt.get();

        // Check if the bike belongs to the authenticated user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(bike);
    }

    @Override
    public ResponseEntity<IncompleteBikeResponseDto> getBikeById(Long id, boolean isComplete) {
        // Najpierw spróbuj znaleźć rower w określonym typie (zgodnie z parametrem isComplete)
        if (isComplete) {
            Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);

            if (bicycleOpt.isPresent()) {
                Bicycle bicycle = bicycleOpt.get();

                // Check if the bicycle belongs to the authenticated user
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).build();
                }

                return ResponseEntity.ok(IncompleteBikeResponseDto.fromBicycleEntity(bicycle));
            }
        } else {
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);

            if (bikeOpt.isPresent()) {
                IncompleteBike bike = bikeOpt.get();

                // Check if the bike belongs to the authenticated user
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).build();
                }

                return ResponseEntity.ok(IncompleteBikeResponseDto.fromEntity(bike));
            }
        }

        // Jeśli nie znaleziono roweru w preferowanym typie, spróbujmy w drugim typie
        if (isComplete) {
            // Nie znaleziono w Bicycle, spróbuj w IncompleteBike
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);

            if (bikeOpt.isPresent()) {
                IncompleteBike bike = bikeOpt.get();

                // Check if the bike belongs to the authenticated user
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).build();
                }

                return ResponseEntity.ok(IncompleteBikeResponseDto.fromEntity(bike));
            }
        } else {
            // Nie znaleziono w IncompleteBike, spróbuj w Bicycle
            Optional<Bicycle> bicycleOpt = bicycleRepository.findById(id);

            if (bicycleOpt.isPresent()) {
                Bicycle bicycle = bicycleOpt.get();

                // Check if the bicycle belongs to the authenticated user
                String email = getCurrentUserEmail();
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).build();
                }

                return ResponseEntity.ok(IncompleteBikeResponseDto.fromBicycleEntity(bicycle));
            }
        }

        // Rower nie został znaleziony w żadnym z typów
        return ResponseEntity.notFound().build();
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
    @Transactional
    public ResponseEntity<?> updateIncompleteBike(Long id, IncompleteBikeDto incompleteBikeDto) {
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);

        if (bikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike bike = bikeOpt.get();

        // Check if the bike belongs to the authenticated user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to update this bike"));
        }

        bike.setBrand(incompleteBikeDto.brand());
        bike.setModel(incompleteBikeDto.model());
        bike.setType(incompleteBikeDto.type());
        bike.setFrameMaterial(incompleteBikeDto.frameMaterial());
        bike.setProductionDate(incompleteBikeDto.productionDate());

        incompleteBikeRepository.save(bike);

        return ResponseEntity.ok(Map.of("message", "Incomplete bike updated successfully"));
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
        return deleteBikePhoto(bicycle);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteIncompleteBikePhoto(Long id) {
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(id);
        if (bikeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        IncompleteBike bike = bikeOpt.get();
        return deleteBikePhoto(bike);
    }

    private ResponseEntity<?> deleteBikePhoto(IncompleteBike bike) {
        // Check if the bike belongs to the user
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (bike.getOwner() == null || !bike.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to delete this bike's photo"));
        }

        // Usuń zdjęcie, jeśli istnieje
        if (bike.getPhoto() != null) {
            bicyclePhotoRepository.delete(bike.getPhoto());
            bike.setPhoto(null);

            // Zapisz odpowiedni obiekt
            if (bike instanceof Bicycle) {
                bicycleRepository.save((Bicycle) bike);
            } else {
                incompleteBikeRepository.save(bike);
            }

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