package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.mapDto.*;

/**
 * Interfejs serwisu mapy obsługujący funkcjonalności podobne do Napravelo
 * - Geospatial queries z bounds
 * - Session management i cache
 * - Wyszukiwanie i filtrowanie
 * - Analytics tracking
 */
public interface MapService {

    /**
     * Pobiera serwisy w zadanym obszarze geograficznym z paginacją i filtrowaniem
     *
     * @param request żądanie zawierające bounds, filtry, informacje o sesji
     * @return odpowiedź z listą serwisów, metadanymi paginacji i bounds
     */
    MapServicesResponseDto getServicesInBounds(MapServicesRequestDto request);

    /**
     * Szybkie pobranie pinów bez szczegółowych informacji
     * Używane do szybkiego renderowania mapy
     *
     * @param request żądanie z podstawowymi filtrami (miasto, bounds)
     * @return lista pinów z podstawowymi informacjami
     */
    MapPinsResponseDto getMapPins(MapPinsRequestDto request);

    /**
     * Wyszukiwanie serwisów po nazwie, mieście lub innych kryteriach
     *
     * @param request żądanie wyszukiwania z query, filtrami i paginacją
     * @return wyniki wyszukiwania z metadanymi
     */
    MapServicesResponseDto searchServices(MapSearchRequestDto request);

    /**
     * Pobieranie szczegółowych informacji o serwisie z cache'owaniem
     *
     * @param serviceId identyfikator serwisu
     * @param sessionId identyfikator sesji dla analytics
     * @return szczegółowe informacje o serwisie lub null jeśli nie znaleziono
     */
    BikeServiceDto getServiceDetail(String serviceId, String sessionId);

    /**
     * Zapisywanie eventów analitycznych
     * - kliknięcia w serwisy
     * - wyszukiwania
     * - zmiany zoom/bounds mapy
     * - zapytania o transport
     *
     * @param event event analityczny do zapisania
     */
    void trackAnalyticsEvent(AnalyticsEventDto event);

    /**
     * Wyszukiwanie serwisów po nazwie z opcją sortowania po statusie rejestracji
     *
     * @param query fraza wyszukiwania (minimum 3 znaki)
     * @param registeredFirst czy sortować zarejestrowane serwisy najpierw
     * @param limit maksymalna liczba wyników
     * @return odpowiedź z wynikami wyszukiwania
     */
    MapServicesResponseDto searchServicesByName(String query, boolean registeredFirst, int limit);

    /**
     * Pobiera klastrowane piny - grupuje serwisy w zależności od poziomu zoom
     * @param request żądanie z bounds, zoom level i opcjonalnym filtrem miasta
     * @return zgrupowane piny z informacją o liczbie serwisów w każdym klastrze
     */
    MapPinsResponseDto getClusteredPins(MapPinsRequestDto request);
}