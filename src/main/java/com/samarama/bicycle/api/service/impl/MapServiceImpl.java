package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.mapDto.*;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.BikeServiceRegistered;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.service.MapService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MapServiceImpl implements MapService {

    @Value("${map.pagination.default-size:25}")
    private int defaultPageSize;

    private final BikeServiceRepository bikeServiceRepository;
    private final AnalyticsService analyticsService;
    private final BikeServiceCacheService cacheService;

    @Autowired
    public MapServiceImpl(BikeServiceRepository bikeServiceRepository,
                          AnalyticsService analyticsService,
                          BikeServiceCacheService cacheService) {
        this.bikeServiceRepository = bikeServiceRepository;
        this.analyticsService = analyticsService;
        this.cacheService = cacheService;
    }

    @PostConstruct
    public void warmUpCache() {
        log.info("Warming up cache on application startup...");
        try {
            cacheService.getAllServicesFromCache();
            log.info("Cache warm-up completed successfully");
        } catch (Exception e) {
            log.error("Failed to warm up cache", e);
        }
    }

    /**
     * Pobiera serwisy w zadanym obszarze geograficznym z paginacją
     */
    @Override
    public MapServicesResponseDto getServicesInBounds(MapServicesRequestDto request) {
        log.info("Getting services in bounds - page: {}, perPage: {}", request.getPage(), request.getPerPage());

        // Pobierz wszystkie serwisy z cache
        List<BikeService> allServices = cacheService.getAllServicesFromCache();

        // Filtruj po bounds jeśli podane
        List<BikeService> filteredServices = allServices;
        if (request.getBounds() != null && !request.getBounds().isEmpty()) {
            filteredServices = filterByBounds(allServices, request.getBounds());
        }

        // Filtruj serwisy bez współrzędnych i sortuj
        List<BikeService> sortedServices = filteredServices.stream()
                .filter(service -> service.getLatitude() != null && service.getLongitude() != null)
                .sorted(this::compareServices)
                .collect(Collectors.toList());

        // Parametry paginacji
        int page = request.getPage() != null ? request.getPage() : 0;
        int perPage = request.getPerPage() != null ? request.getPerPage() : defaultPageSize;
        int total = sortedServices.size();
        int totalPages = (int) Math.ceil((double) total / perPage);

        // Oblicz zakres dla bieżącej strony
        int startIndex = page * perPage;
        int endIndex = Math.min(startIndex + perPage, total);

        // Pobierz serwisy dla bieżącej strony
        List<ServiceLocationDto> serviceLocations = Collections.emptyList();
        if (startIndex < total) {
            serviceLocations = sortedServices.subList(startIndex, endIndex).stream()
                    .map(this::convertToServiceLocationDto)
                    .collect(Collectors.toList());
        }

        return MapServicesResponseDto.builder()
                .data(serviceLocations)
                .total(total)
                .totalPages(totalPages)
                .sortColumn("name")
                .sortDirection("ASC")
                .page(page)
                .previous(page > 0 ? page - 1 : 0)
                .next(page < totalPages - 1 ? page + 1 : page)
                .perPage(perPage)
                .bounds(serviceLocations.isEmpty() ? createDefaultBounds() : calculateBounds(serviceLocations))
                .cache("HIT")
                .build();
    }

    /**
     * Wyszukiwanie serwisów po nazwie z sortowaniem
     */
    @Override
    public MapServicesResponseDto searchServicesByName(String query, boolean registeredFirst, int limit) {
        if (query == null || query.length() < 3) {
            return MapServicesResponseDto.builder()
                    .data(Collections.emptyList())
                    .total(0)
                    .build();
        }

        List<BikeService> allServices = cacheService.getAllServicesFromCache();
        List<BikeService> services = filterByName(allServices, query);

        List<ServiceLocationDto> results = services.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .sorted((s1, s2) -> registeredFirst ? compareServicesWithRegisteredFirst(s1, s2) : compareServices(s1, s2))
                .limit(limit)
                .map(this::convertToServiceLocationDto)
                .collect(Collectors.toList());

        return MapServicesResponseDto.builder()
                .data(results)
                .total(results.size())
                .build();
    }

    /**
     * Pobiera klastrowane piny - GŁÓWNA METODA DLA MAPY
     */
    @Override
    @Cacheable(value = "clusteredPins", key = "#request.bounds + '_' + #request.zoom + '_' + #request.city", sync = true)
    public MapPinsResponseDto getClusteredPins(MapPinsRequestDto request) {
        log.info("Getting clustered pins for zoom: {}, bounds: {}, city: {}",
                request.getZoom(), request.getBounds(), request.getCity());

        List<BikeService> services = cacheService.getAllServicesFromCache();

        // Filtrowanie po mieście
        if (request.getCity() != null && !request.getCity().isEmpty()) {
            services = filterByCity(services, request.getCity());
        }

        // Filtruj po bounds jeśli podane
        if (request.getBounds() != null) {
            services = filterByBounds(services, request.getBounds());
        }

        // Usuń serwisy bez współrzędnych
        services = services.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .collect(Collectors.toList());

        List<ServicePinDto> pins;

        // Jeśli zoom >= 14, nie klastruj - pokaż wszystkie pojedyncze piny
        if (request.getZoom() != null && request.getZoom() >= 14) {
            pins = services.stream()
                    .map(this::convertToServicePinDto)
                    .collect(Collectors.toList());
        } else {
            // W przeciwnym razie klastruj według grid size
            double radius = calculateClusterRadius(request.getZoom() != null ? request.getZoom() : 10);
            pins = clusterServicesByGrid(services, radius);
        }

        return MapPinsResponseDto.builder()
                .data(pins)
                .total(pins.size())
                .bounds(calculateBoundsFromServices(services))
                .cache("HIT")
                .build();
    }

    /**
     * Szybkie pobranie pinów bez szczegółów (BEZ KLASTERINGU)
     */
    @Override
    @Cacheable(value = "mapPins", key = "#request.city + '_' + #request.bounds", sync = true)
    public MapPinsResponseDto getMapPins(MapPinsRequestDto request) {
        log.info("Getting map pins for city: {}, bounds: {}", request.getCity(), request.getBounds());

        List<BikeService> services = cacheService.getAllServicesFromCache();

        if (request.getCity() != null && !request.getCity().isEmpty()) {
            services = filterByCity(services, request.getCity());
        }

        // Filtruj po bounds jeśli podane
        if (request.getBounds() != null) {
            services = filterByBounds(services, request.getBounds());
        }

        List<ServicePinDto> pins = services.stream()
                .filter(service -> service.getLatitude() != null && service.getLongitude() != null)
                .map(this::convertToServicePinDto)
                .collect(Collectors.toList());

        return MapPinsResponseDto.builder()
                .data(pins)
                .total(pins.size())
                .bounds(calculateBoundsFromServices(services))
                .cache("HIT")
                .build();
    }

    /**
     * Wyszukiwanie serwisów z paginacją
     */
    @Override
    public MapServicesResponseDto searchServices(MapSearchRequestDto request) {
        log.info("Searching services with query: {}", request.getQuery());

        List<BikeService> services = cacheService.getAllServicesFromCache();

        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            services = filterByNameOrCity(services, request.getQuery().trim());
        }

        // Filtruj zweryfikowane jeśli potrzeba
        if (request.isVerifiedOnly()) {
            services = services.stream()
                    .filter(BikeService::getVerified)
                    .collect(Collectors.toList());
        }

        // Paginacja
        int startIndex = request.getPage() * request.getPerPage();
        int endIndex = Math.min(startIndex + request.getPerPage(), services.size());

        List<ServiceLocationDto> serviceLocations = Collections.emptyList();
        if (startIndex < services.size()) {
            List<BikeService> paginatedServices = services.subList(startIndex, endIndex);
            serviceLocations = paginatedServices.stream()
                    .map(this::convertToServiceLocationDto)
                    .collect(Collectors.toList());
        }

        return MapServicesResponseDto.builder()
                .data(serviceLocations)
                .total(services.size())
                .totalPages((int) Math.ceil((double) services.size() / request.getPerPage()))
                .sortColumn("name")
                .sortDirection("ASC")
                .page(request.getPage())
                .previous(request.getPage() > 0 ? request.getPage() - 1 : 0)
                .next(endIndex < services.size() ? request.getPage() + 1 : request.getPage())
                .perPage(request.getPerPage())
                .bounds(serviceLocations.isEmpty() ? createDefaultBounds() : calculateBounds(serviceLocations))
                .cache("HIT")
                .build();
    }

    /**
     * Pobiera szczegóły serwisu
     */
    @Override
    @Cacheable(value = "serviceDetails", key = "#serviceId", sync = true)
    public BikeServiceDto getServiceDetail(String serviceId, String sessionId) {
        log.info("Getting service detail for ID: {} (session: {})", serviceId, sessionId);

        try {
            Long id = Long.parseLong(serviceId);

            List<BikeService> allServices = cacheService.getAllServicesFromCache();
            Optional<BikeService> serviceOpt = allServices.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst();

            if (serviceOpt.isPresent()) {
                BikeService service = serviceOpt.get();
                return BikeServiceDto.fromEntity(service);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid service ID format: {}", serviceId);
        }

        return null;
    }

    /**
     * Zapisywanie eventów analitycznych
     */
    @Override
    public void trackAnalyticsEvent(AnalyticsEventDto event) {
        log.info("Tracking analytics event: {} for service: {}", event.getEventType(), event.getServiceId());
        analyticsService.trackEvent(event);
    }

    // ==================== FILTERING HELPER METHODS ====================

    private List<BikeService> filterByName(List<BikeService> services, String query) {
        String queryLower = query.trim().toLowerCase();
        return services.stream()
                .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(queryLower))
                .collect(Collectors.toList());
    }

    private List<BikeService> filterByCity(List<BikeService> services, String city) {
        String cityLower = city.toLowerCase();
        return services.stream()
                .filter(s -> s.getCity() != null && s.getCity().toLowerCase().equals(cityLower))
                .collect(Collectors.toList());
    }

    private List<BikeService> filterByNameOrCity(List<BikeService> services, String query) {
        String queryLower = query.toLowerCase();
        return services.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(queryLower)) ||
                        (s.getCity() != null && s.getCity().toLowerCase().contains(queryLower)))
                .collect(Collectors.toList());
    }

    private List<BikeService> filterByBounds(List<BikeService> services, String boundsStr) {
        try {
            String[] parts = boundsStr.split(",");
            if (parts.length == 4) {
                double swLat = Double.parseDouble(parts[0]);
                double swLng = Double.parseDouble(parts[1]);
                double neLat = Double.parseDouble(parts[2]);
                double neLng = Double.parseDouble(parts[3]);

                return services.stream()
                        .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                        .filter(s -> s.getLatitude() >= swLat && s.getLatitude() <= neLat)
                        .filter(s -> s.getLongitude() >= swLng && s.getLongitude() <= neLng)
                        .collect(Collectors.toList());
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid bounds format: {}", boundsStr);
        }
        return services;
    }

    // ==================== SORTING HELPER METHODS ====================

    private int compareServices(BikeService s1, BikeService s2) {
        return s1.getName().compareToIgnoreCase(s2.getName());
    }

    private int compareServicesWithRegisteredFirst(BikeService s1, BikeService s2) {
        boolean s1Reg = s1 instanceof BikeServiceRegistered;
        boolean s2Reg = s2 instanceof BikeServiceRegistered;
        if (s1Reg != s2Reg) {
            return s1Reg ? -1 : 1;
        }
        return s1.getName().compareToIgnoreCase(s2.getName());
    }

    // ==================== CLUSTERING METHODS ====================

    private List<ServicePinDto> clusterServicesByGrid(List<BikeService> services, double radiusKm) {
        List<BikeService> unclustered = new ArrayList<>(services);
        List<List<BikeService>> clusters = new ArrayList<>();

        while (!unclustered.isEmpty()) {
            BikeService seed = unclustered.remove(0);
            List<BikeService> cluster = new ArrayList<>();
            cluster.add(seed);

            unclustered.removeIf(service -> {
                if (calculateDistance(seed, service) <= radiusKm) {
                    cluster.add(service);
                    return true;
                }
                return false;
            });

            clusters.add(cluster);
        }

        return clusters.stream()
                .map(this::createClusterPin)
                .collect(Collectors.toList());
    }

    private double calculateDistance(BikeService s1, BikeService s2) {
        double lat1 = Math.toRadians(s1.getLatitude());
        double lat2 = Math.toRadians(s2.getLatitude());
        double lon1 = Math.toRadians(s1.getLongitude());
        double lon2 = Math.toRadians(s2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private double calculateClusterRadius(int zoom) {
        if (zoom >= 14) return 0.1;
        if (zoom >= 12) return 1.0;
        if (zoom >= 10) return 5.0;
        if (zoom >= 8) return 20.0;
        if (zoom >= 6) return 100.0;
        if (zoom >= 4) return 500.0;
        return 1000.0;
    }

    private ServicePinDto createClusterPin(List<BikeService> servicesInCluster) {
        if (servicesInCluster.size() == 1) {
            return convertToServicePinDto(servicesInCluster.get(0));
        }

        double avgLat = servicesInCluster.stream()
                .mapToDouble(BikeService::getLatitude)
                .average().orElse(0);
        double avgLng = servicesInCluster.stream()
                .mapToDouble(BikeService::getLongitude)
                .average().orElse(0);

        String clusterIds = servicesInCluster.stream()
                .map(s -> s.getId().toString())
                .collect(Collectors.joining(","));

        return ServicePinDto.builder()
                .id("cluster_" + servicesInCluster.size() + "_" + clusterIds.hashCode())
                .name(servicesInCluster.size() + " serwisów")
                .latitude(avgLat)
                .longitude(avgLng)
                .verified(false)
                .category("cluster")
                .build();
    }

    // ==================== CONVERSION METHODS ====================

    private ServiceLocationDto convertToServiceLocationDto(BikeService service) {
        return ServiceLocationDto.builder()
                .id(service.getId())
                .name(service.getName())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .address(buildAddress(service))
                .phoneNumber(service.getPhoneNumber())
                .email(service.getEmail())
                .verified(service.getVerified())
                .region(service.getCity())
                .build();
    }

    private ServicePinDto convertToServicePinDto(BikeService service) {
        return ServicePinDto.builder()
                .id(service.getId().toString())
                .name(service.getName())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .verified(service.getVerified())
                .category("bike_service")
                .build();
    }

    // ==================== BOUNDS CALCULATION ====================

    private BoundsDto calculateBounds(List<ServiceLocationDto> services) {
        if (services.isEmpty()) {
            return createDefaultBounds();
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (ServiceLocationDto service : services) {
            double lat = service.getLatitude();
            double lng = service.getLongitude();

            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLng = Math.min(minLng, lng);
            maxLng = Math.max(maxLng, lng);
        }

        return BoundsDto.builder()
                .sw(BoundsDto.PointDto.builder().latitude(minLat).longitude(minLng).build())
                .ne(BoundsDto.PointDto.builder().latitude(maxLat).longitude(maxLng).build())
                .center(BoundsDto.PointDto.builder()
                        .latitude((minLat + maxLat) / 2)
                        .longitude((minLng + maxLng) / 2)
                        .build())
                .zoom(calculateZoom(maxLat - minLat, maxLng - minLng))
                .build();
    }

    private BoundsDto calculateBoundsFromServices(List<BikeService> services) {
        if (services.isEmpty()) {
            return createDefaultBounds();
        }

        double minLat = services.stream().mapToDouble(BikeService::getLatitude).min().orElse(50.0);
        double maxLat = services.stream().mapToDouble(BikeService::getLatitude).max().orElse(50.1);
        double minLng = services.stream().mapToDouble(BikeService::getLongitude).min().orElse(19.0);
        double maxLng = services.stream().mapToDouble(BikeService::getLongitude).max().orElse(19.1);

        return BoundsDto.builder()
                .sw(BoundsDto.PointDto.builder().latitude(minLat).longitude(minLng).build())
                .ne(BoundsDto.PointDto.builder().latitude(maxLat).longitude(maxLng).build())
                .center(BoundsDto.PointDto.builder()
                        .latitude((minLat + maxLat) / 2)
                        .longitude((minLng + maxLng) / 2)
                        .build())
                .zoom(calculateZoom(maxLat - minLat, maxLng - minLng))
                .build();
    }

    private BoundsDto createDefaultBounds() {
        return BoundsDto.builder()
                .sw(BoundsDto.PointDto.builder().latitude(49.9).longitude(19.8).build())
                .ne(BoundsDto.PointDto.builder().latitude(50.2).longitude(20.2).build())
                .center(BoundsDto.PointDto.builder().latitude(50.0647).longitude(19.9450).build())
                .zoom(11)
                .build();
    }

    private int calculateZoom(double latDiff, double lngDiff) {
        double maxDiff = Math.max(latDiff, lngDiff);
        if (maxDiff > 10) return 6;
        if (maxDiff > 5) return 7;
        if (maxDiff > 2) return 8;
        if (maxDiff > 1) return 9;
        if (maxDiff > 0.5) return 10;
        if (maxDiff > 0.25) return 11;
        if (maxDiff > 0.1) return 12;
        if (maxDiff > 0.05) return 13;
        return 14;
    }

    private String buildAddress(BikeService service) {
        StringBuilder addr = new StringBuilder();
        if (service.getStreet() != null) addr.append(service.getStreet()).append(" ");
        if (service.getBuilding() != null) addr.append(service.getBuilding());
        if (service.getCity() != null) addr.append(", ").append(service.getCity());
        return addr.toString().trim();
    }
}

// === BIKE SERVICE CACHE SERVICE ===

@Service
@Slf4j
class BikeServiceCacheService {

    private final BikeServiceRepository bikeServiceRepository;

    @Autowired
    public BikeServiceCacheService(BikeServiceRepository bikeServiceRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
    }

    @Cacheable(value = "allBikeServices", sync = true)
    public List<BikeService> getAllServicesFromCache() {
        log.info("Loading all services from database (cache miss)");
        List<BikeService> services = bikeServiceRepository.findAll();

        if (services == null) {
            log.warn("Repository returned null, returning empty list");
            return new ArrayList<>();
        }

        return services;
    }

    @CacheEvict(value = "allBikeServices", allEntries = true)
    public void evictCache() {
        log.info("Evicting all bike services cache");
    }
}

// === ANALYTICS SERVICE ===

@Service
@Slf4j
class AnalyticsService {
    public void trackEvent(AnalyticsEventDto event) {
        log.info("Analytics event tracked: type={}, serviceId={}, sessionId={}",
                event.getEventType(), event.getServiceId(), event.getSessionId());
    }
}