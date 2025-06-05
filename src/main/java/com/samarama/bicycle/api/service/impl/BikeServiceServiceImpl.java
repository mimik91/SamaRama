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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            existingService.setTransportCost(bikeServiceDto.transportCost());

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

            // PROSTE ROZWIĄZANIE: Przeczytaj plik jako bytes, a potem jako string
            byte[] fileBytes = file.getBytes();
            String csvContent = new String(fileBytes, StandardCharsets.UTF_8);

            // Usuń BOM jeśli istnieje
            if (csvContent.startsWith("\uFEFF")) {
                csvContent = csvContent.substring(1);
            }

            logger.info("CSV file size: " + csvContent.length() + " characters");

            String[] lines = csvContent.split("\\r?\\n");
            logger.info("CSV lines count: " + lines.length);

            boolean isFirstLine = true;
            int lineNumber = 0;

            for (String line : lines) {
                lineNumber++;

                // Pomijamy nagłówek
                if (isFirstLine) {
                    isFirstLine = false;
                    logger.info("CSV Header: " + line);
                    continue;
                }

                // Pomijamy puste linie
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    logger.info("Processing line " + lineNumber + ": " + line);

                    BikeService service = parseCsvLine(line, lineNumber);
                    if (service != null) {
                        servicesToSave.add(service);
                        successCount++;

                        logger.info("Parsed service: " + service.getName() + " at " + service.getStreet() + " " + service.getBuilding());
                    }
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Linia " + lineNumber + ": " + e.getMessage());
                    logger.warning("Błąd parsowania linii " + lineNumber + ": " + e.getMessage());
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
            e.printStackTrace();
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
        // Wzorce dla różnych formatów adresów
        // Format 1: "Ulica Numer, Miasto"
        // Format 2: "Ulica Numer Mieszkanie, Miasto"
        // Format 3: "Ulica, Miasto" (bez numeru)

        String[] addressParts = address.split(",");

        if (addressParts.length >= 2) {
            // Mamy przecinek - ostatnia część to miasto
            String streetPart = addressParts[0].trim();
            String cityPart = addressParts[addressParts.length - 1].trim();

            // Parsuj część z ulicą i numerem
            parseStreetAndNumber(service, streetPart);

            // Ustaw miasto
            service.setCity(extractCityName(cityPart));

        } else if (addressParts.length == 1) {
            // Brak przecinka - spróbuj wykryć format
            String fullAddress = address.trim();

            // Sprawdź czy kończy się znanym miastem
            String detectedCity = detectCityFromAddress(fullAddress);
            if (detectedCity != null) {
                service.setCity(detectedCity);
                // Usuń miasto z adresu i parsuj resztę
                String streetPart = fullAddress.replace(detectedCity, "").trim();
                parseStreetAndNumber(service, streetPart);
            } else {
                // Nie udało się wykryć miasta - parsuj całość jako ulicę
                parseStreetAndNumber(service, fullAddress);
                service.setCity("Kraków"); // Domyślnie
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

    private void parseStreetAndNumber(BikeService service, String streetPart) {
        if (streetPart == null || streetPart.trim().isEmpty()) {
            return;
        }

        streetPart = streetPart.trim();

        // Wzorzec: wszystko do ostatniej spacji to ulica, po spacji to numer
        // Przykłady: "Wielkotyrnowska 12", "Aleja Mickiewicza 24A", "ul. Floriańska 15"
        Pattern pattern = Pattern.compile("^(.+?)\\s+([0-9]+[A-Za-z]?(?:/[0-9]+[A-Za-z]?)?)$");
        Matcher matcher = pattern.matcher(streetPart);

        if (matcher.matches()) {
            String street = matcher.group(1).trim();
            String buildingNumber = matcher.group(2).trim();

            service.setStreet(street);

            // Sprawdź czy numer zawiera mieszkanie (format: "12/4")
            if (buildingNumber.contains("/")) {
                String[] parts = buildingNumber.split("/");
                service.setBuilding(parts[0]);
                if (parts.length > 1) {
                    service.setFlat(parts[1]);
                }
            } else {
                service.setBuilding(buildingNumber);
            }
        } else {
            // Nie udało się wyodrębnić numeru - cała część to ulica
            service.setStreet(streetPart);
        }
    }

    /**
     * Wyciąga nazwę miasta (usuwa kod pocztowy)
     */
    private String extractCityName(String cityPart) {
        if (cityPart == null || cityPart.trim().isEmpty()) {
            return null;
        }

        // Usuń kod pocztowy (XX-XXX)
        String cleaned = cityPart.replaceAll("\\d{2}-\\d{3}", "").trim();

        if (!cleaned.isEmpty()) {
            return cleaned;
        }

        return cityPart.trim();
    }

    /**
     * Próbuje wykryć miasto na końcu adresu
     */
    private String detectCityFromAddress(String address) {
        String lowerAddress = address.toLowerCase();

        // Lista znanych miast
        String[] cities = {
                "kraków", "krakow", "wieliczka", "skawina",
                "niepołomice", "niepolomice", "warszawa",
                "gdańsk", "gdansk", "wrocław", "wroclaw",
                "poznań", "poznan", "łódź", "lodz"
        };

        for (String city : cities) {
            if (lowerAddress.endsWith(" " + city) || lowerAddress.equals(city)) {
                // Zwróć z poprawną wielkością liter
                return city;
            }
        }

        return null;
    }

    private BufferedReader createBufferedReaderWithProperEncoding(MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();

        // Najpierw spróbuj wykryć kodowanie przez sprawdzenie BOM
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 3);
        byte[] bom = new byte[3];
        int bytesRead = pushbackInputStream.read(bom);

        String encoding = "UTF-8"; // Domyślnie UTF-8

        if (bytesRead >= 3) {
            if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                // UTF-8 BOM - zostaw UTF-8
                encoding = "UTF-8";
            } else {
                // Brak BOM - sprawdź inne kodowania
                pushbackInputStream.unread(bom, 0, bytesRead);

                // Dla polskich znaków często używane kodowania
                encoding = detectPolishEncoding(pushbackInputStream);
            }
        } else {
            // Przywróć przeczytane bajty
            pushbackInputStream.unread(bom, 0, bytesRead);
        }

        logger.info("Wykryte kodowanie CSV: " + encoding);

        return new BufferedReader(new InputStreamReader(pushbackInputStream, encoding));
    }

    /**
     * Próbuje wykryć polskie kodowanie na podstawie zawartości
     */
    private String detectPolishEncoding(PushbackInputStream inputStream) throws Exception {
        // Przeczytaj próbkę danych
        byte[] sample = new byte[1024];
        int sampleSize = inputStream.read(sample);
        inputStream.unread(sample, 0, sampleSize);

        // Testuj różne kodowania z polskimi znakami
        String[] encodings = {"UTF-8", "ISO-8859-2", "Windows-1250", "CP852"};

        for (String encoding : encodings) {
            try {
                String testString = new String(sample, 0, sampleSize, encoding);

                // Sprawdź czy zawiera sensowne polskie znaki
                if (containsValidPolishCharacters(testString)) {
                    logger.info("Wykryto kodowanie na podstawie polskich znaków: " + encoding);
                    return encoding;
                }
            } catch (Exception e) {
                // Ignoruj błędy kodowania
            }
        }

        // Jeśli nic nie pasuje, użyj UTF-8
        return "UTF-8";
    }

    /**
     * Sprawdza czy string zawiera prawidłowe polskie znaki
     */
    private boolean containsValidPolishCharacters(String text) {
        // Sprawdź czy zawiera polskie litery i czy nie ma "romba ze znakiem zapytania"
        boolean hasPolishChars = text.matches(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*");
        boolean hasReplacementChars = text.contains("�") || text.contains("?");

        // Dobry kandydat jeśli ma polskie znaki i nie ma znaków zastępczych
        return hasPolishChars && !hasReplacementChars;
    }
}