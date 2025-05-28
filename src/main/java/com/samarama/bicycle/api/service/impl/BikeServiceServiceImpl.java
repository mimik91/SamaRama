package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.BikeServicePinDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.service.BikeServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class BikeServiceServiceImpl implements BikeServiceService {

    private static final Logger logger = Logger.getLogger(BikeServiceServiceImpl.class.getName());

    private final BikeServiceRepository bikeServiceRepository;

    @Autowired
    public BikeServiceServiceImpl(BikeServiceRepository bikeServiceRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
    }

    // === PUBLICZNE METODY ===

    @Override
    @Transactional(readOnly = true)
    public List<BikeServicePinDto> getAllBikeServicePins() {
        logger.info("Pobieranie pinów wszystkich serwisów rowerowych");

        return bikeServiceRepository.findServicesWithCoordinates().stream()
                .map(BikeServicePinDto::fromEntity)
                .filter(BikeServicePinDto::hasValidCoordinates)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BikeServiceDto> getBikeServiceDetails(Long id) {
        logger.info("Pobieranie szczegółów serwisu rowerowego o ID: " + id);

        return bikeServiceRepository.findById(id)
                .map(BikeServiceDto::fromEntity);
    }

    // === METODY ADMINISTRACYJNE ===

    @Override
    @Transactional(readOnly = true)
    public List<BikeServiceDto> getAllBikeServicesForAdmin() {
        logger.info("Pobieranie wszystkich serwisów rowerowych dla administratora");

        return bikeServiceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BikeServiceDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> createBikeService(BikeServiceDto bikeServiceDto) {
        try {
            // Walidacja nazwy - obowiązkowe pole
            if (bikeServiceDto.name() == null || bikeServiceDto.name().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nazwa serwisu jest wymagana"));
            }

            // Walidacja - sprawdź czy nazwa nie jest już używana
            if (bikeServiceRepository.existsByNameIgnoreCase(bikeServiceDto.name())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Serwis o podanej nazwie już istnieje"));
            }

            // Utworzenie nowego serwisu
            BikeService bikeService = bikeServiceDto.toEntity();
            BikeService savedService = bikeServiceRepository.save(bikeService);

            logger.info("Utworzono nowy serwis rowerowy: " + savedService.getName() + " (ID: " + savedService.getId() + ")");

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis rowerowy został utworzony pomyślnie",
                    "id", savedService.getId(),
                    "service", BikeServiceDto.fromEntity(savedService)
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas tworzenia serwisu rowerowego: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas tworzenia serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateBikeService(Long id, BikeServiceDto bikeServiceDto) {
        try {
            Optional<BikeService> existingServiceOpt = bikeServiceRepository.findById(id);
            if (existingServiceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BikeService existingService = existingServiceOpt.get();

            // Walidacja nazwy - obowiązkowe pole
            if (bikeServiceDto.name() == null || bikeServiceDto.name().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Nazwa serwisu jest wymagana"));
            }

            // Walidacja nazwa - sprawdź czy nowa nazwa nie koliduje z innym serwisem
            if (!bikeServiceDto.name().equalsIgnoreCase(existingService.getName()) &&
                    bikeServiceRepository.existsByNameIgnoreCase(bikeServiceDto.name())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Podana nazwa jest już używana przez inny serwis"));
            }

            // Aktualizacja danych serwisu
            existingService.setName(bikeServiceDto.name());
            existingService.setDescription(bikeServiceDto.description());
            existingService.setEmail(bikeServiceDto.email());
            existingService.setWebsite(bikeServiceDto.website());
            existingService.setStreet(bikeServiceDto.street());
            existingService.setBuilding(bikeServiceDto.building());
            existingService.setFlat(bikeServiceDto.flat());
            existingService.setPostalCode(bikeServiceDto.postalCode());
            existingService.setCity(bikeServiceDto.city());
            existingService.setLatitude(bikeServiceDto.latitude());
            existingService.setLongitude(bikeServiceDto.longitude());
            existingService.setPhoneNumber(bikeServiceDto.phoneNumber());
            existingService.setBusinessPhone(bikeServiceDto.businessPhone());

            BikeService updatedService = bikeServiceRepository.save(existingService);

            logger.info("Zaktualizowano serwis rowerowy: " + updatedService.getName() + " (ID: " + id + ")");

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis rowerowy został zaktualizowany pomyślnie",
                    "service", BikeServiceDto.fromEntity(updatedService)
            ));

        } catch (Exception e) {
            logger.severe("Błąd podczas aktualizacji serwisu rowerowego o ID " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas aktualizacji serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteBikeService(Long id) {
        try {
            if (!bikeServiceRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            bikeServiceRepository.deleteById(id);
            logger.info("Usunięto serwis rowerowy o ID: " + id);

            return ResponseEntity.ok(Map.of("message", "Serwis rowerowy został usunięty pomyślnie"));

        } catch (Exception e) {
            logger.severe("Błąd podczas usuwania serwisu rowerowego o ID " + id + ": " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas usuwania serwisu: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> importBikeServicesFromCsv(MultipartFile file, String adminEmail) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Plik CSV jest pusty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Plik musi mieć rozszerzenie .csv"));
        }

        try {
            List<BikeService> servicesToSave = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    // Pomijamy nagłówek
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Pomijamy puste linie
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        BikeService service = parseCsvLine(line, lineNumber);
                        if (service != null) {
                            servicesToSave.add(service);
                            successCount++;
                        }
                    } catch (Exception e) {
                        errorCount++;
                        errors.add("Linia " + lineNumber + ": " + e.getMessage());
                        logger.warning("Błąd parsowania linii " + lineNumber + ": " + e.getMessage());
                    }
                }
            }

            // Zapisz wszystkie prawidłowe serwisy
            if (!servicesToSave.isEmpty()) {
                bikeServiceRepository.saveAll(servicesToSave);
                logger.info("Zaimportowano " + servicesToSave.size() + " serwisów przez " + adminEmail);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Import zakończony");
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("totalProcessed", successCount + errorCount);

            if (!errors.isEmpty()) {
                result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
                if (errors.size() > 10) {
                    result.put("hasMoreErrors", true);
                    result.put("totalErrors", errors.size());
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.severe("Błąd podczas importu CSV: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas importu pliku: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBikeServiceStatistics() {
        try {
            long totalServices = bikeServiceRepository.count();
            long servicesWithCoordinates = bikeServiceRepository.findServicesWithCoordinates().size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalServices", totalServices);
            stats.put("servicesWithCoordinates", servicesWithCoordinates);
            stats.put("servicesWithoutCoordinates", totalServices - servicesWithCoordinates);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.severe("Błąd podczas pobierania statystyk: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas pobierania statystyk: " + e.getMessage()));
        }
    }

    // === METODY POMOCNICZE DO PARSOWANIA CSV ===

    private BikeService parseCsvLine(String line, int lineNumber) throws Exception {
        // Parsowanie linii CSV: nazwa,adres,numer telefonu,latitude,longitude
        String[] fields = parseCsvFields(line);

        if (fields.length < 5) {
            throw new Exception("Nieprawidłowa liczba kolumn (oczekiwano 5, otrzymano " + fields.length + ")");
        }

        String name = fields[0].trim();
        String address = fields[1].trim();
        String phoneStr = fields[2].trim();
        String latStr = fields[3].trim();
        String lngStr = fields[4].trim();

        // Walidacja obowiązkowych pól
        if (name.isEmpty()) {
            throw new Exception("Nazwa serwisu jest wymagana");
        }

        if (address.isEmpty()) {
            throw new Exception("Adres serwisu jest wymagany");
        }

        // Sprawdź czy nazwa już istnieje
        if (bikeServiceRepository.existsByNameIgnoreCase(name)) {
            throw new Exception("Serwis o nazwie '" + name + "' już istnieje");
        }

        BikeService service = new BikeService();
        service.setName(name);

        // Parsowanie adresu
        parseAddress(service, address);

        // Parsowanie telefonu
        if (!phoneStr.isEmpty()) {
            service.setPhoneNumber(parsePhoneNumber(phoneStr));
        }

        // Parsowanie współrzędnych
        if (!latStr.isEmpty() && !lngStr.isEmpty()) {
            parseCoordinates(service, latStr, lngStr);
        }

        return service;
    }

    private void parseAddress(BikeService service, String address) {
        // Próbujemy wyciągnąć miasto i ulicę z adresu
        String[] addressParts = address.split(",");

        if (addressParts.length >= 2) {
            // Format z przecinkiem: "ulica, miasto"
            service.setStreet(addressParts[0].trim());
            String cityPart = addressParts[addressParts.length - 1].trim();

            // Wyciągnij miasto (zwykle na końcu)
            String[] cityWords = cityPart.split("\\s+");
            if (cityWords.length > 0) {
                service.setCity(cityWords[cityWords.length - 1]);
            }
        } else {
            // Format bez przecinka - próbujemy wykryć miasto
            service.setStreet(address);

            // Sprawdź czy adres zawiera znane miasta
            if (address.toLowerCase().contains("kraków") || address.toLowerCase().contains("krakow")) {
                service.setCity("Kraków");
            } else if (address.toLowerCase().contains("wieliczka")) {
                service.setCity("Wieliczka");
            } else {
                service.setCity("Kraków"); // Domyślnie Kraków
            }
        }
    }

    private String parsePhoneNumber(String phoneStr) {
        try {
            // Usuń wszelkie białe znaki i znaki specjalne
            String cleanPhone = phoneStr.replaceAll("[\\s\\-\\(\\)\\+]", "");

            // Spróbuj sparsować jako liczbę
            if (cleanPhone.matches("\\d+")) {
                return cleanPhone;
            } else {
                return phoneStr; // Zwróć oryginalną wartość jeśli nie da się oczyścić
            }
        } catch (Exception e) {
            return phoneStr; // Zwróć oryginalną wartość w przypadku błędu
        }
    }

    private void parseCoordinates(BikeService service, String latStr, String lngStr) throws Exception {
        try {
            Double latitude = Double.parseDouble(latStr.trim());
            Double longitude = Double.parseDouble(lngStr.trim());

            // Walidacja zakresu współrzędnych
            if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                service.setLatitude(latitude);
                service.setLongitude(longitude);
            } else {
                throw new Exception("Współrzędne poza dozwolonym zakresem");
            }
        } catch (NumberFormatException e) {
            throw new Exception("Nieprawidłowy format współrzędnych geograficznych");
        }
    }

    private String[] parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }
}