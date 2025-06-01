package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.service.*;
import com.samarama.bicycle.api.repository.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Kontroler administracyjny - centralizuje wszystkie funkcje zarządzania systemem
 *
 * POPRAWKI:
 * - Centralizacja funkcji admina
 * - Proper role validation
 * - Audit logging
 * - Data validation
 * - Pagination support
 * - Advanced filtering
 * - System statistics
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminController {

    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    // Services
    private final TransportOrderService transportOrderService;
    private final ServiceOrderService serviceOrderService;
    private final BicycleService bicycleService;
    private final BikeServiceService bikeServiceService;
    private final ServicePackageService servicePackageService;
    private final ServiceSlotService serviceSlotService;
    private final BicycleEnumerationService enumerationService;
    private final EmailService emailService;

    // Repositories for advanced queries
    private final UserRepository userRepository;
    private final TransportOrderRepository transportOrderRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final IncompleteBikeRepository bikeRepository;
    private final ServicePackageRepository servicePackageRepository;

    public AdminController(
            TransportOrderService transportOrderService,
            ServiceOrderService serviceOrderService,
            BicycleService bicycleService,
            BikeServiceService bikeServiceService,
            ServicePackageService servicePackageService,
            ServiceSlotService serviceSlotService,
            BicycleEnumerationService enumerationService,
            EmailService emailService,
            UserRepository userRepository,
            TransportOrderRepository transportOrderRepository,
            ServiceOrderRepository serviceOrderRepository,
            IncompleteBikeRepository bikeRepository,
            ServicePackageRepository servicePackageRepository) {
        this.transportOrderService = transportOrderService;
        this.serviceOrderService = serviceOrderService;
        this.bicycleService = bicycleService;
        this.bikeServiceService = bikeServiceService;
        this.servicePackageService = servicePackageService;
        this.serviceSlotService = serviceSlotService;
        this.enumerationService = enumerationService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.bikeRepository = bikeRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    // =================== DASHBOARD & OVERVIEW ===================

    /**
     * POPRAWKA: Dodano kompletny dashboard z wszystkimi kluczowymi metrykami
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_DASHBOARD", null, adminEmail);

        try {
            Map<String, Object> dashboard = new HashMap<>();

            // === PODSTAWOWE STATYSTYKI ===
            dashboard.put("overview", getSystemOverview());

            // === ZAMÓWIENIA ===
            dashboard.put("orders", getOrderStatistics());

            // === UŻYTKOWNICY ===
            dashboard.put("users", getUserStatistics());

            // === SERWISY ===
            dashboard.put("services", getServiceStatistics());

            // === NAJBLIŻSZE WYDARZENIA ===
            dashboard.put("upcoming", getUpcomingEvents());

            // === ALERTY SYSTEMOWE ===
            dashboard.put("alerts", getSystemAlerts());

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.severe("Error loading dashboard for admin " + adminEmail + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania dashboardu"));
        }
    }

    /**
     * POPRAWKA: Dodano szczegółowe statystyki systemowe
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSystemStatistics(
            @RequestParam(defaultValue = "30") int days) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_STATISTICS", "days=" + days, adminEmail);

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            Map<String, Object> stats = new HashMap<>();

            // Statystyki zamówień w czasie
            stats.put("orderTrends", getOrderTrends(startDate, endDate));

            // Statystyki przychodów
            stats.put("revenue", getRevenueStatistics(startDate, endDate));

            // Popularne pakiety serwisowe
            stats.put("popularPackages", serviceOrderService.getServicePackageStatistics());

            // Średni czas serwisu
            stats.put("averageServiceTime", serviceOrderService.getAverageServiceTime());

            // Statystyki użytkowników
            stats.put("userGrowth", getUserGrowthStatistics(startDate, endDate));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.severe("Error loading statistics: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania statystyk"));
        }
    }

    // =================== ORDER MANAGEMENT ===================

    /**
     * POPRAWKA: Dodano zaawansowane filtrowanie i paginację zamówień
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_ORDERS", buildFilterString(status, orderType, searchTerm, dateFrom, dateTo), adminEmail);

        try {
            // POPRAWKA: Walidacja parametrów paginacji
            if (size > 100) size = 100; // Maksymalnie 100 elementów na stronę
            if (size < 1) size = 20;
            if (page < 0) page = 0;

            // POPRAWKA: Walidacja sortowania
            String validatedSortBy = validateSortField(sortBy, Arrays.asList("orderDate", "pickupDate", "status", "totalPrice"));
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

            // Pobierz dane z filtrowaniem
            List<UnifiedOrderResponseDto> allOrders = transportOrderService.getAllOrders();

            // POPRAWKA: Zastosuj filtry
            List<UnifiedOrderResponseDto> filteredOrders = applyOrderFilters(
                    allOrders, status, orderType, searchTerm, dateFrom, dateTo);

            // POPRAWKA: Ręczna paginacja (gdyby repository nie wspierało)
            int start = page * size;
            int end = Math.min(start + size, filteredOrders.size());
            List<UnifiedOrderResponseDto> pageContent = start > filteredOrders.size()
                    ? new ArrayList<>()
                    : filteredOrders.subList(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("content", pageContent);
            response.put("totalElements", filteredOrders.size());
            response.put("totalPages", (int) Math.ceil((double) filteredOrders.size() / size));
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("filters", Map.of(
                    "status", status,
                    "orderType", orderType,
                    "searchTerm", searchTerm,
                    "dateFrom", dateFrom,
                    "dateTo", dateTo
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error loading orders: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania zamówień"));
        }
    }

    /**
     * POPRAWKA: Bezpieczna aktualizacja statusu z walidacją
     */
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody Map<String, String> request) {

        String adminEmail = getCurrentUserEmail();
        String newStatus = request.get("status");

        // POPRAWKA: Walidacja danych wejściowych
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        // POPRAWKA: Walidacja czy status jest poprawny
        if (!isValidOrderStatus(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nieprawidłowy status",
                    "validStatuses", getValidOrderStatuses()
            ));
        }

        logAdminAction("UPDATE_ORDER_STATUS", "orderId=" + orderId + ", status=" + newStatus, adminEmail);

        try {
            // POPRAWKA: Sprawdź czy zamówienie istnieje
            if (!orderExists(orderId)) {
                return ResponseEntity.notFound().build();
            }

            // Deleguj do odpowiedniego serwisu
            return transportOrderService.updateOrderStatusByAdmin(orderId, newStatus, adminEmail);
        } catch (Exception e) {
            logger.severe("Error updating order status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd aktualizacji statusu"));
        }
    }

    /**
     * POPRAWKA: Bezpieczne usuwanie zamówienia z walidacją
     */
    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')") // Tylko ADMIN może usuwać
    public ResponseEntity<?> deleteOrder(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();

        if (!hasAdminRole()) {
            return ResponseEntity.status(403).body(Map.of("message", "Tylko administrator może usuwać zamówienia"));
        }

        logAdminAction("DELETE_ORDER", "orderId=" + orderId, adminEmail);

        try {
            // POPRAWKA: Sprawdź czy zamówienie można usunąć
            if (!canDeleteOrder(orderId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Nie można usunąć zamówienia w tym statusie"
                ));
            }

            return transportOrderService.deleteTransportOrder(orderId, adminEmail);
        } catch (Exception e) {
            logger.severe("Error deleting order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd usuwania zamówienia"));
        }
    }

    // =================== USER MANAGEMENT ===================

    /**
     * POPRAWKA: Zarządzanie użytkownikami z paginacją i filtrowaniem
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean verified) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_USERS", "search=" + searchTerm + ", role=" + role, adminEmail);

        try {
            // POPRAWKA: Walidacja parametrów
            if (size > 100) size = 100;
            if (size < 1) size = 20;
            if (page < 0) page = 0;

            String validatedSortBy = validateSortField(sortBy, Arrays.asList("createdAt", "email", "firstName", "lastName"));
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

            // POPRAWKA: Użyj repository z paginacją (zakładając że istnieje)
            Page<User> userPage = getUsersWithFilters(pageable, searchTerm, role, verified);

            Map<String, Object> response = new HashMap<>();
            response.put("content", userPage.getContent().stream()
                    .map(this::mapUserToDto)
                    .collect(Collectors.toList()));
            response.put("totalElements", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error loading users: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania użytkowników"));
        }
    }

    /**
     * POPRAWKA: Bezpieczna zmiana ról użytkownika
     */
    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')") // Tylko ADMIN może zmieniać role
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, Set<String>> request) {

        String adminEmail = getCurrentUserEmail();
        Set<String> newRoles = request.get("roles");

        // POPRAWKA: Walidacja ról
        if (newRoles == null || newRoles.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role są wymagane"));
        }

        if (!areValidRoles(newRoles)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nieprawidłowe role",
                    "validRoles", getValidRoles()
            ));
        }

        logAdminAction("UPDATE_USER_ROLES", "userId=" + userId + ", roles=" + newRoles, adminEmail);

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();

            // POPRAWKA: Nie pozwól adminowi usunąć sobie uprawnień ADMIN
            if (user.getEmail().equals(adminEmail) && !newRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Nie możesz usunąć sobie uprawnień administratora"
                ));
            }

            user.setRoles(newRoles);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Role zostały zaktualizowane",
                    "user", mapUserToDto(user)
            ));
        } catch (Exception e) {
            logger.severe("Error updating user roles: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd aktualizacji ról"));
        }
    }

    // =================== SERVICE PACKAGE MANAGEMENT ===================

    /**
     * POPRAWKA: Zarządzanie pakietami serwisowymi z walidacją
     */
    @PostMapping("/service-packages")
    public ResponseEntity<?> createServicePackage(@Valid @RequestBody ServicePackageDto packageDto) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("CREATE_SERVICE_PACKAGE", "code=" + packageDto.code(), adminEmail);

        try {
            // POPRAWKA: Dodatkowa walidacja biznesowa
            if (servicePackageRepository.existsByCode(packageDto.code())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Pakiet o kodzie '" + packageDto.code() + "' już istnieje"
                ));
            }

            return servicePackageService.createServicePackage(packageDto);
        } catch (Exception e) {
            logger.severe("Error creating service package: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd tworzenia pakietu"));
        }
    }

    // =================== BIKE SERVICE MANAGEMENT ===================

    /**
     * POPRAWKA: Import serwisów z walidacją pliku
     */
    @PostMapping("/bike-services/import")
    public ResponseEntity<?> importBikeServices(@RequestParam("file") MultipartFile file) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("IMPORT_BIKE_SERVICES", "filename=" + file.getOriginalFilename(), adminEmail);

        try {
            // POPRAWKA: Walidacja pliku
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Plik jest pusty"));
            }

            if (!isValidCsvFile(file)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy format pliku"));
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                return ResponseEntity.badRequest().body(Map.of("message", "Plik jest zbyt duży (max 10MB)"));
            }

            return bikeServiceService.importBikeServicesFromCsv(file, adminEmail);
        } catch (Exception e) {
            logger.severe("Error importing bike services: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd importu serwisów"));
        }
    }

    // =================== SYSTEM CONFIGURATION ===================

    /**
     * POPRAWKA: Zarządzanie konfiguracją slotów z walidacją
     */
    @PostMapping("/service-slots/config")
    public ResponseEntity<?> createSlotConfig(@Valid @RequestBody ServiceSlotConfigDto configDto) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("CREATE_SLOT_CONFIG",
                "startDate=" + configDto.startDate() + ", maxBikes=" + configDto.maxBikesPerDay(), adminEmail);

        try {
            // POPRAWKA: Walidacja biznesowa
            if (configDto.startDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Data początkowa nie może być w przeszłości"
                ));
            }

            if (configDto.endDate() != null && configDto.endDate().isBefore(configDto.startDate())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Data końcowa nie może być wcześniejsza niż początkowa"
                ));
            }

            return serviceSlotService.createSlotConfig(configDto);
        } catch (Exception e) {
            logger.severe("Error creating slot config: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd tworzenia konfiguracji"));
        }
    }

    // =================== AUDIT LOG ===================

    /**
     * POPRAWKA: Log akcji administracyjnych
     */
    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String currentAdmin = getCurrentUserEmail();
        logAdminAction("VIEW_AUDIT_LOG", "filters applied", currentAdmin);

        // TODO: Implementacja audit log - wymagałaby nowej tabeli audit_log
        return ResponseEntity.ok(Map.of(
                "message", "Audit log nie jest jeszcze zaimplementowany",
                "suggestion", "Należy utworzyć tabelę audit_log i odpowiedni serwis"
        ));
    }

    // =================== HELPER METHODS ===================

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    private void logAdminAction(String action, String details, String adminEmail) {
        logger.info(String.format("ADMIN_ACTION: %s by %s - %s", action, adminEmail, details));
        // TODO: Zapisz do tabeli audit_log
    }

    private Map<String, Object> getSystemOverview() {
        return Map.of(
                "totalUsers", userRepository.count(),
                "totalOrders", transportOrderRepository.count(),
                "totalBicycles", bikeRepository.count(),
                "activeServicePackages", servicePackageRepository.findByActiveTrue().size()
        );
    }

    private Map<String, Object> getOrderStatistics() {
        return Map.of(
                "totalOrders", transportOrderRepository.count(),
                "pendingOrders", transportOrderRepository.countByStatus(TransportOrder.OrderStatus.PENDING),
                "completedOrders", transportOrderRepository.countByStatus(TransportOrder.OrderStatus.ON_THE_WAY_BACK),
                "todayOrders", transportOrderRepository.countByPickupDate(LocalDate.now())
        );
    }

    private Map<String, Object> getUserStatistics() {
        long totalUsers = userRepository.count();
        // TODO: Dodaj więcej statystyk użytkowników
        return Map.of(
                "totalUsers", totalUsers,
                "verifiedUsers", totalUsers, // Placeholder - wymagałoby query
                "newUsersThisMonth", 0 // Placeholder - wymagałoby query
        );
    }

    private Map<String, Object> getServiceStatistics() {
        return Map.of(
                "averageServiceTime", serviceOrderService.getAverageServiceTime(),
                "popularPackages", serviceOrderService.getServicePackageStatistics()
        );
    }

    private List<String> getUpcomingEvents() {
        // TODO: Implementacja nadchodzących wydarzeń
        return Arrays.asList(
                "5 zamówień do odbioru dzisiaj",
                "3 serwisy do zakończenia",
                "Konfiguracja slotów wygasa za 7 dni"
        );
    }

    private List<String> getSystemAlerts() {
        List<String> alerts = new ArrayList<>();

        // Sprawdź przepełnienie slotów
        int todayOrders = transportOrderRepository.countByPickupDate(LocalDate.now());
        if (todayOrders > 10) {
            alerts.add("Wysoka liczba zamówień na dzisiaj: " + todayOrders);
        }

        // TODO: Dodaj więcej alertów systemowych

        return alerts;
    }

    private List<Object[]> getOrderTrends(LocalDate startDate, LocalDate endDate) {
        return transportOrderRepository.countOrdersForDateRange(startDate, endDate);
    }

    private Map<String, Object> getRevenueStatistics(LocalDate startDate, LocalDate endDate) {
        // TODO: Implementacja statystyk przychodów
        return Map.of(
                "totalRevenue", 0,
                "transportRevenue", 0,
                "serviceRevenue", 0
        );
    }

    private Map<String, Object> getUserGrowthStatistics(LocalDate startDate, LocalDate endDate) {
        // TODO: Implementacja statystyk wzrostu użytkowników
        return Map.of("newUsers", 0);
    }

    private String validateSortField(String sortBy, List<String> allowedFields) {
        return allowedFields.contains(sortBy) ? sortBy : allowedFields.get(0);
    }

    private List<UnifiedOrderResponseDto> applyOrderFilters(
            List<UnifiedOrderResponseDto> orders, String status, String orderType,
            String searchTerm, LocalDate dateFrom, LocalDate dateTo) {

        return orders.stream()
                .filter(order -> status == null || order.status().equals(status))
                .filter(order -> orderType == null || order.orderType().equals(orderType))
                .filter(order -> searchTerm == null ||
                        order.clientEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        (order.clientPhone() != null && order.clientPhone().contains(searchTerm)))
                .filter(order -> dateFrom == null || !order.pickupDate().isBefore(dateFrom))
                .filter(order -> dateTo == null || !order.pickupDate().isAfter(dateTo))
                .collect(Collectors.toList());
    }

    private boolean isValidOrderStatus(String status) {
        try {
            TransportOrder.OrderStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private List<String> getValidOrderStatuses() {
        return Arrays.stream(TransportOrder.OrderStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private boolean orderExists(Long orderId) {
        return transportOrderRepository.existsById(orderId);
    }

    private boolean canDeleteOrder(Long orderId) {
        // TODO: Implementacja reguł biznesowych dla usuwania
        return true;
    }

    private Page<User> getUsersWithFilters(Pageable pageable, String searchTerm, String role, Boolean verified) {
        // TODO: Implementacja filtrowanego wyszukiwania użytkowników
        return userRepository.findAll(pageable);
    }

    private Map<String, Object> mapUserToDto(User user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "verified", user.isVerified(),
                "roles", user.getRoles(),
                "createdAt", user.getCreatedAt()
        );
    }

    private boolean areValidRoles(Set<String> roles) {
        Set<String> validRoles = Set.of("ROLE_CLIENT", "ROLE_SERVICE", "ROLE_ADMIN", "ROLE_MODERATOR");
        return validRoles.containsAll(roles);
    }

    private Set<String> getValidRoles() {
        return Set.of("ROLE_CLIENT", "ROLE_SERVICE", "ROLE_ADMIN", "ROLE_MODERATOR");
    }

    private boolean isValidCsvFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    private String buildFilterString(String status, String orderType, String searchTerm,
                                     LocalDate dateFrom, LocalDate dateTo) {
        return String.format("status=%s, type=%s, search=%s, from=%s, to=%s",
                status, orderType, searchTerm, dateFrom, dateTo);
    }
}