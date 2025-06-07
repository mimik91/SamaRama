package com.samarama.bicycle.api.service.helper;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Pomocnicza klasa do czytania plików CSV z obsługą różnych kodowań
 * Szczególnie przygotowana do obsługi polskich znaków
 */
@Component
public class CsvReader {

    private static final Logger logger = Logger.getLogger(CsvReader.class.getName());

    // Domyślne kodowanie - można je zmienić w runtime
    private Charset defaultCharset = null;

    /**
     * Ustawia domyślne kodowanie które będzie używane zamiast automatycznego wykrywania
     */
    public void setDefaultCharset(String charsetName) {
        try {
            this.defaultCharset = Charset.forName(charsetName);
            logger.info("Ustawiono domyślne kodowanie na: " + charsetName);
        } catch (Exception e) {
            logger.warning("Nieprawidłowe kodowanie: " + charsetName + ", używam automatycznego wykrywania");
            this.defaultCharset = null;
        }
    }

    /**
     * Ustawia domyślne kodowanie
     */
    public void setDefaultCharset(Charset charset) {
        this.defaultCharset = charset;
        logger.info("Ustawiono domyślne kodowanie na: " + (charset != null ? charset.name() : "automatyczne"));
    }

    /**
     * Wyczyść domyślne kodowanie (powrót do automatycznego wykrywania)
     */
    public void clearDefaultCharset() {
        this.defaultCharset = null;
        logger.info("Usunięto domyślne kodowanie, używam automatycznego wykrywania");
    }

    /**
     * Zwraca aktualnie ustawione domyślne kodowanie
     */
    public String getCurrentDefaultCharset() {
        return defaultCharset != null ? defaultCharset.name() : "automatyczne";
    }

    /**
     * Czyta plik CSV z automatycznym wykrywaniem kodowania
     */
    public CsvReadResult readCsvFile(MultipartFile file) {
        return readCsvFile(file, null);
    }

    /**
     * Czyta plik CSV z możliwością wymuszenia konkretnego kodowania
     */
    public CsvReadResult readCsvFile(MultipartFile file, Charset forcedCharset) {
        try {
            logger.info("Rozpoczęcie czytania pliku CSV: " + file.getOriginalFilename());

            // Pobierz zawartość pliku
            byte[] fileBytes = file.getBytes();

            // Wykryj lub użyj wymuszonego/domyślnego kodowania
            Charset charsetToUse;
            String source;

            if (forcedCharset != null) {
                charsetToUse = forcedCharset;
                source = "wymuszone";
            } else if (defaultCharset != null) {
                charsetToUse = defaultCharset;
                source = "domyślne";
            } else {
                charsetToUse = detectCharset(fileBytes);
                source = "wykryte";
            }

            logger.info("Używane kodowanie: " + charsetToUse.name() + " (" + source + ")");

            // Konwertuj zawartość do stringa
            String csvContent = new String(fileBytes, charsetToUse);

            // Log pierwszych 200 znaków do debugowania
            String debugSample = csvContent.length() > 200 ? csvContent.substring(0, 200) + "..." : csvContent;
            logger.info("Próbka zawartości: " + debugSample.replaceAll("\\n", "\\\\n"));

            // Usuń BOM jeśli istnieje
            csvContent = removeBOM(csvContent);

            // Podziel na linie
            String[] lines = csvContent.split("\\r?\\n");
            logger.info("Znaleziono " + lines.length + " linii");

            List<String[]> rows = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                // Pomijaj puste linie
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = parseCsvLine(line);
                    rows.add(fields);

                    // Log pierwszego wiersza danych dla debugowania
                    if (rows.size() == 1) {
                        logger.info("Pierwszy wiersz danych: " + Arrays.toString(fields));
                    }
                } catch (Exception e) {
                    errors.add("Błąd w linii " + (i + 1) + ": " + e.getMessage());
                    logger.warning("Błąd parsowania linii " + (i + 1) + ": " + e.getMessage());
                }
            }

            logger.info("Pomyślnie przeczytano " + rows.size() + " wierszy z pliku CSV");

            return new CsvReadResult(rows, errors, charsetToUse.name());

        } catch (Exception e) {
            logger.severe("Błąd podczas czytania pliku CSV: " + e.getMessage());
            List<String> errors = new ArrayList<>();
            errors.add("Błąd podczas czytania pliku: " + e.getMessage());
            return new CsvReadResult(new ArrayList<>(), errors, "UNKNOWN");
        }
    }

    /**
     * Wykrywa kodowanie pliku na podstawie zawartości
     */
    private Charset detectCharset(byte[] fileBytes) {
        // Sprawdź BOM
        if (fileBytes.length >= 3) {
            // UTF-8 BOM
            if (fileBytes[0] == (byte) 0xEF &&
                    fileBytes[1] == (byte) 0xBB &&
                    fileBytes[2] == (byte) 0xBF) {
                logger.info("Wykryto UTF-8 BOM");
                return StandardCharsets.UTF_8;
            }
        }

        if (fileBytes.length >= 2) {
            // UTF-16 BE BOM
            if (fileBytes[0] == (byte) 0xFE && fileBytes[1] == (byte) 0xFF) {
                logger.info("Wykryto UTF-16 BE BOM");
                return StandardCharsets.UTF_16BE;
            }

            // UTF-16 LE BOM
            if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xFE) {
                logger.info("Wykryto UTF-16 LE BOM");
                return StandardCharsets.UTF_16LE;
            }
        }

        // Testuj różne kodowania dla polskich znaków (w kolejności prawdopodobieństwa)
        Charset[] polishCharsets = {
                Charset.forName("Windows-1252"),  // Windows Western Europe (najczęstszy problem)
                Charset.forName("Windows-1250"),  // Windows Central Europe
                StandardCharsets.UTF_8,           // UTF-8
                Charset.forName("ISO-8859-2"),    // Latin-2 (Środkowa Europa)
                Charset.forName("ISO-8859-1"),    // Latin-1 (Western Europe)
                Charset.forName("CP1252"),        // Code Page 1252 (alias dla Windows-1252)
                Charset.forName("CP852")          // DOS Środkowa Europa
        };

        for (Charset charset : polishCharsets) {
            try {
                String testContent = new String(fileBytes, charset);

                if (isValidPolishContent(testContent)) {
                    logger.info("Wykryto kodowanie przez analizę treści: " + charset.name());
                    return charset;
                }
            } catch (Exception e) {
                // Ignoruj błędy kodowania
            }
        }

        // Domyślnie UTF-8
        logger.info("Używam domyślnego kodowania UTF-8");
        return StandardCharsets.UTF_8;
    }

    /**
     * Sprawdza czy treść zawiera prawidłowe polskie znaki
     */
    private boolean isValidPolishContent(String content) {
        // Sprawdź czy zawiera polskie litery
        boolean hasPolishChars = content.matches(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*");

        // Sprawdź czy nie ma znaków zastępczych
        boolean hasReplacementChars = content.contains("�") ||
                content.contains("\uFFFD") ||
                content.contains("?");

        // Sprawdź czy zawiera sensowne słowa polskie
        boolean hasSensibleWords = content.toLowerCase().matches(".*\\b(kraków|krakow|warszawa|ulica|miasto|telefon|adres|cena)\\b.*");

        // Dobry kandydat jeśli:
        // - Ma polskie znaki ALBO sensowne słowa polskie
        // - I nie ma znaków zastępczych
        return (hasPolishChars || hasSensibleWords) && !hasReplacementChars;
    }

    /**
     * Usuwa BOM (Byte Order Mark) z początku stringa
     */
    private String removeBOM(String content) {
        if (content.startsWith("\uFEFF")) {
            logger.info("Usuwam BOM z początku pliku");
            return content.substring(1);
        }
        return content;
    }

    /**
     * Parsuje linię CSV obsługując cudzysłowy i przecinki wewnątrz pól
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escapeNext) {
                currentField.append(c);
                escapeNext = false;
            } else if (c == '\\') {
                escapeNext = true;
            } else if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Podwójny cudzysłów - to escape
                    currentField.append('"');
                    i++; // Pomiń następny cudzysłów
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // Dodaj ostatnie pole
        fields.add(currentField.toString().trim());

        return fields.toArray(new String[0]);
    }

    /**
     * Waliduje format pliku CSV
     */
    public boolean isValidCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        // Sprawdź rozszerzenie
        if (!filename.toLowerCase().endsWith(".csv")) {
            return false;
        }

        // Sprawdź rozmiar (maksymalnie 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return false;
        }

        return true;
    }

    /**
     * Testuje plik z konkretnym kodowaniem i zwraca próbkę
     */
    public String testFileWithEncoding(MultipartFile file, String charsetName) {
        try {
            byte[] fileBytes = file.getBytes();
            Charset charset = Charset.forName(charsetName);
            String content = new String(fileBytes, charset);

            // Usuń BOM
            content = removeBOM(content);

            // Zwróć pierwsze 500 znaków
            return content.length() > 500 ? content.substring(0, 500) + "..." : content;

        } catch (Exception e) {
            return "Błąd: " + e.getMessage();
        }
    }

    /**
     * Klasa reprezentująca wynik czytania CSV
     */
    public static class CsvReadResult {
        private final List<String[]> rows;
        private final List<String> errors;
        private final String detectedEncoding;

        public CsvReadResult(List<String[]> rows, List<String> errors, String detectedEncoding) {
            this.rows = rows;
            this.errors = errors;
            this.detectedEncoding = detectedEncoding;
        }

        public List<String[]> getRows() {
            return rows;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getDetectedEncoding() {
            return detectedEncoding;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public int getRowCount() {
            return rows.size();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }

        public String[] getHeader() {
            if (rows.isEmpty()) {
                return new String[0];
            }
            return rows.get(0);
        }

        public List<String[]> getDataRows() {
            if (rows.size() <= 1) {
                return new ArrayList<>();
            }
            return rows.subList(1, rows.size());
        }

        @Override
        public String toString() {
            return String.format("CsvReadResult{rows=%d, errors=%d, encoding=%s}",
                    rows.size(), errors.size(), detectedEncoding);
        }
    }
}