package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.BikeServiceDto;
import com.samarama.bicycle.api.dto.BikeServicePinDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.service.BikeServiceService;
import com.samarama.bicycle.api.service.helper.CsvReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BikeServiceServiceImpl implements BikeServiceService {

    private static final Logger logger = Logger.getLogger(BikeServiceServiceImpl.class.getName());

    private final BikeServiceRepository bikeServiceRepository;
    private final CsvReader csvReader;

    @Autowired
    public BikeServiceServiceImpl(BikeServiceRepository bikeServiceRepository, CsvReader csvReader) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.csvReader = csvReader;
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
        try {
            // Walidacja pliku
            if (!csvReader.isValidCsvFile(file)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Nieprawidłowy plik CSV. Sprawdź czy plik ma rozszerzenie .csv i nie jest większy niż 10MB"
                ));
            }

            logger.info("Rozpoczęcie importu serwisów z pliku: " + file.getOriginalFilename() + " przez " + adminEmail);

            // WYMUSZAM kodowanie Windows-1250
            Charset forcedCharset = Charset.forName("Windows-1250");
            logger.info("Używam kodowania: " + forcedCharset.name());

            // Czytaj plik CSV z wymuszonym kodowaniem Windows-1250
            CsvReader.CsvReadResult csvResult = csvReader.readCsvFile(file, forcedCharset);

            if (csvResult.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Plik CSV jest pusty lub nie zawiera danych"
                ));
            }

            logger.info("Przeczytano " + csvResult.getRowCount() + " wierszy");

            // Przetwórz dane
            List<BikeService> servicesToSave = new ArrayList<>();
            List<String> processingErrors = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            // Pomiń pierwszy wiersz (nagłówek) jeśli istnieje
            List<String[]> dataRows = csvResult.getDataRows();

            // Jeśli nie ma nagłówka, użyj wszystkich wierszy
            if (dataRows.isEmpty() && !csvResult.getRows().isEmpty()) {
                dataRows = csvResult.getRows();
            }

            for (int i = 0; i < dataRows.size(); i++) {
                String[] row = dataRows.get(i);
                int lineNumber = i + 2; // +2 bo pominęliśmy nagłówek i liczymy od 1

                try {
                    BikeService service = parseCsvRow(row, lineNumber);
                    if (service != null) {
                        servicesToSave.add(service);
                        successCount++;
                        logger.info("Sparsowano serwis: " + service.getName() + " (" + service.getCity() + ")");
                    }
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Linia " + lineNumber + ": " + e.getMessage();
                    processingErrors.add(errorMsg);
                    logger.warning("Błąd parsowania: " + errorMsg);
                }
            }

            // Dodaj błędy z czytania CSV
            processingErrors.addAll(csvResult.getErrors());

            // Zapisz wszystkie prawidłowe serwisy
            if (!servicesToSave.isEmpty()) {
                List<BikeService> savedServices = bikeServiceRepository.saveAll(servicesToSave);
                logger.info("Zapisano " + savedServices.size() + " serwisów do bazy danych");
            }

            // Przygotuj odpowiedź
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Import CSV zakończony");
            result.put("successCount", successCount);
            result.put("errorCount", errorCount + csvResult.getErrorCount());
            result.put("totalProcessed", successCount + errorCount);
            result.put("usedEncoding", "Windows-1250");

            if (!processingErrors.isEmpty()) {
                // Ogranicz liczbę błędów w odpowiedzi
                List<String> limitedErrors = processingErrors.size() > 10 ?
                        processingErrors.subList(0, 10) : processingErrors;

                result.put("errors", limitedErrors);

                if (processingErrors.size() > 10) {
                    result.put("hasMoreErrors", true);
                    result.put("totalErrors", processingErrors.size());
                }
            }

            logger.info("Import zakończony: " + successCount + " sukces, " + (errorCount + csvResult.getErrorCount()) + " błędów");

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

    /**
     * Parsuje wiersz CSV do obiektu BikeService
     * Oczekiwany format: nazwa,adres,telefon,latitude,longitude
     */
    private BikeService parseCsvRow(String[] fields, int lineNumber) throws Exception {
        if (fields.length < 3) {
            throw new Exception("Nieprawidłowa liczba kolumn (minimum 3: nazwa, adres, telefon)");
        }

        String name = fields[0].trim();
        String address = fields[1].trim();
        String phoneStr = fields.length > 2 ? fields[2].trim() : "";
        String latStr = fields.length > 3 ? fields[3].trim() : "";
        String lngStr = fields.length > 4 ? fields[4].trim() : "";

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

    /**
     * Parsuje adres w różnych formatach
     */
    private void parseAddress(BikeService service, String address) {
        try {
            logger.info("Parsowanie adresu: " + address);

            // Wzorce dla różnych formatów adresów
            String[] addressParts = address.split(",");

            if (addressParts.length >= 2) {
                // Format: "Ulica Numer, Miasto"
                String streetPart = addressParts[0].trim();
                String cityPart = addressParts[addressParts.length - 1].trim();

                parseStreetAndNumber(service, streetPart);
                service.setCity(extractCityName(cityPart));

            } else {
                // Brak przecinka - spróbuj wykryć miasto
                String detectedCity = detectCityFromAddress(address);
                if (detectedCity != null) {
                    service.setCity(detectedCity);
                    String streetPart = address.replace(detectedCity, "").trim();
                    parseStreetAndNumber(service, streetPart);
                } else {
                    // Nie udało się wykryć miasta
                    parseStreetAndNumber(service, address);
                    service.setCity("Kraków"); // Domyślnie
                }
            }

            logger.info("Sparsowany adres - Ulica: " + service.getStreet() +
                    ", Numer: " + service.getBuilding() +
                    ", Miasto: " + service.getCity());

        } catch (Exception e) {
            logger.warning("Błąd parsowania adresu '" + address + "': " + e.getMessage());
            // W przypadku błędu, ustaw przynajmniej podstawowe dane
            service.setStreet(address);
            service.setCity("Kraków");
        }
    }

    /**
     * Parsuje ulicę i numer budynku
     */
    private void parseStreetAndNumber(BikeService service, String streetPart) {
        if (streetPart == null || streetPart.trim().isEmpty()) {
            return;
        }

        streetPart = streetPart.trim();

        // Wzorzec: ulica + numer (może zawierać literę)
        Pattern pattern = Pattern.compile("^(.+?)\\s+([0-9]+[A-Za-z]?(?:/[0-9]+[A-Za-z]?)?)$");
        Matcher matcher = pattern.matcher(streetPart);

        if (matcher.matches()) {
            String street = matcher.group(1).trim();
            String buildingNumber = matcher.group(2).trim();

            // Usuń przedrostki jak "ul.", "al."
            street = street.replaceAll("^(ul\\.|ulica|al\\.|aleja)\\s+", "");

            service.setStreet(street);

            // Obsługa numeru z mieszkaniem (12/4)
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
            // Nie udało się wyodrębnić numeru
            service.setStreet(streetPart);
        }
    }

    /**
     * Czyści numer telefonu
     */
    private String parsePhoneNumber(String phoneStr) {
        if (phoneStr == null || phoneStr.trim().isEmpty()) {
            return null;
        }

        // Usuń białe znaki i znaki specjalne oprócz cyfr i +
        String cleaned = phoneStr.replaceAll("[\\s\\-\\(\\)]", "");

        // Sprawdź czy to sensowny numer telefonu
        if (cleaned.matches("^\\+?[0-9]{9,15}$")) {
            return cleaned;
        }

        return phoneStr; // Zwróć oryginalną wartość jeśli nie pasuje do wzorca
    }

    /**
     * Parsuje współrzędne geograficzne
     */
    private void parseCoordinates(BikeService service, String latStr, String lngStr) throws Exception {
        try {
            Double latitude = Double.parseDouble(latStr.trim());
            Double longitude = Double.parseDouble(lngStr.trim());

            // Walidacja zakresu współrzędnych
            if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                service.setLatitude(latitude);
                service.setLongitude(longitude);
                logger.info("Ustawiono współrzędne: " + latitude + ", " + longitude);
            } else {
                throw new Exception("Współrzędne poza dozwolonym zakresem (lat: " + latitude + ", lng: " + longitude + ")");
            }
        } catch (NumberFormatException e) {
            throw new Exception("Nieprawidłowy format współrzędnych geograficznych: lat='" + latStr + "', lng='" + lngStr + "'");
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
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        String lowerAddress = address.toLowerCase();

        // Lista znanych miast w województwie małopolskim i okolicznych
        String[] cities = {
                "kraków", "krakow", "wieliczka", "skawina", "niepołomice", "niepolomice",
                "myślenice", "myslenice", "chrzastowice", "zabierzów", "zabierzow",
                "zielonki", "michałowice", "michalowice", "liszki", "czernichów", "czernichow",
                "mogilany", "świątniki górne", "swiatniki gorne", "krzeszowice",
                "warszawa", "gdańsk", "gdansk", "wrocław", "wroclaw", "poznań", "poznan",
                "łódź", "lodz", "katowice", "lublin", "białystok", "bialystok",
                "bydgoszcz", "toruń", "torun", "rzeszów", "rzeszow"
        };

        for (String city : cities) {
            if (lowerAddress.endsWith(" " + city) || lowerAddress.equals(city)) {
                // Zwróć z poprawną wielkością liter (pierwsza wielka)
                return city.substring(0, 1).toUpperCase() + city.substring(1);
            }
        }

        return null;
    }
}