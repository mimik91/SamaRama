package com.samarama.bicycle.api.service.helper.csvReader;

import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Komponent odpowiedzialny za import serwisów rowerowych z plików CSV
 */
@Component
public class BikeServiceCsvImporter {

    private static final Logger logger = Logger.getLogger(BikeServiceCsvImporter.class.getName());

    private final BikeServiceRepository bikeServiceRepository;
    private final CsvReader csvReader;
    private final BikeServiceCsvParser csvParser;

    @Autowired
    public BikeServiceCsvImporter(BikeServiceRepository bikeServiceRepository,
                                  CsvReader csvReader,
                                  BikeServiceCsvParser csvParser) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.csvReader = csvReader;
        this.csvParser = csvParser;
    }

    /**
     * Importuje serwisy z pliku CSV
     */
    public ResponseEntity<?> importBikeServicesFromCsv(MultipartFile file, String adminEmail) {
        try {
            // Walidacja pliku
            if (!csvReader.isValidCsvFile(file)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Nieprawidłowy plik CSV. Sprawdź czy plik ma rozszerzenie .csv i nie jest większy niż 10MB"
                ));
            }

            logger.info("Rozpoczęcie importu serwisów z pliku: " + file.getOriginalFilename() + " przez " + adminEmail);

            Charset forcedCharset = StandardCharsets.UTF_8;
            logger.info("Używam kodowania: " + forcedCharset.name());

            // Czytaj plik CSV z wymuszonym kodowaniem
            CsvReader.CsvReadResult csvResult = csvReader.readCsvFile(file, forcedCharset);

            if (csvResult.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Plik CSV jest pusty lub nie zawiera danych"
                ));
            }

            logger.info("Przeczytano " + csvResult.getRowCount() + " wierszy");

            // Przetwórz dane
            ImportResult importResult = processCSVData(csvResult);

            // Zapisz wszystkie prawidłowe serwisy
            if (!importResult.getServicesToSave().isEmpty()) {
                List<BikeService> savedServices = bikeServiceRepository.saveAll(importResult.getServicesToSave());
                logger.info("Zapisano " + savedServices.size() + " serwisów do bazy danych");
            }

            // Przygotuj odpowiedź
            return buildImportResponse(importResult, csvResult);

        } catch (Exception e) {
            logger.severe("Błąd podczas importu CSV: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Błąd podczas importu pliku: " + e.getMessage()));
        }
    }

    /**
     * Przetwarza dane z pliku CSV
     */
    private ImportResult processCSVData(CsvReader.CsvReadResult csvResult) {
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
                BikeService service = csvParser.parseCsvRow(row, lineNumber);
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

        return new ImportResult(servicesToSave, processingErrors, successCount, errorCount + csvResult.getErrorCount());
    }

    /**
     * Buduje odpowiedź HTTP z wynikami importu
     */
    private ResponseEntity<?> buildImportResponse(ImportResult importResult, CsvReader.CsvReadResult csvResult) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Import CSV zakończony");
        result.put("successCount", importResult.getSuccessCount());
        result.put("errorCount", importResult.getErrorCount());
        result.put("totalProcessed", importResult.getSuccessCount() + importResult.getErrorCount());
        result.put("usedEncoding", csvResult.getDetectedEncoding());

        List<String> allErrors = importResult.getProcessingErrors();
        if (!allErrors.isEmpty()) {
            // Ogranicz liczbę błędów w odpowiedzi
            List<String> limitedErrors = allErrors.size() > 10 ?
                    allErrors.subList(0, 10) : allErrors;

            result.put("errors", limitedErrors);

            if (allErrors.size() > 10) {
                result.put("hasMoreErrors", true);
                result.put("totalErrors", allErrors.size());
            }
        }

        logger.info("Import zakończony: " + importResult.getSuccessCount() + " sukces, " +
                importResult.getErrorCount() + " błędów");

        return ResponseEntity.ok(result);
    }

    /**
     * Klasa przechowująca wyniki importu
     */
    private static class ImportResult {
        private final List<BikeService> servicesToSave;
        private final List<String> processingErrors;
        private final int successCount;
        private final int errorCount;

        public ImportResult(List<BikeService> servicesToSave, List<String> processingErrors,
                            int successCount, int errorCount) {
            this.servicesToSave = servicesToSave;
            this.processingErrors = processingErrors;
            this.successCount = successCount;
            this.errorCount = errorCount;
        }

        public List<BikeService> getServicesToSave() { return servicesToSave; }
        public List<String> getProcessingErrors() { return processingErrors; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
    }
}