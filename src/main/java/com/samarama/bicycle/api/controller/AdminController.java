package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.service.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.helper.OrderValidator;
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
    private final BikeServiceService bikeServiceService;
    private final ServicePackageService servicePackageService;
    private final ServiceSlotService serviceSlotService;
    private final IncompleteUserRepository incompleteUserRepository;

    // Repositories for advanced queries
    private final UserRepository userRepository;
    private final TransportOrderRepository transportOrderRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final IncompleteBikeRepository bikeRepository;
    private final ServicePackageRepository servicePackageRepository;

    public AdminController(
            TransportOrderService transportOrderService,
            ServiceOrderService serviceOrderService,
            BikeServiceService bikeServiceService,
            ServicePackageService servicePackageService,
            ServiceSlotService serviceSlotService,
            IncompleteUserRepository incompleteUserRepository,
            UserRepository userRepository,
            TransportOrderRepository transportOrderRepository,
            ServiceOrderRepository serviceOrderRepository,
            IncompleteBikeRepository bikeRepository,
            ServicePackageRepository servicePackageRepository) {
        this.transportOrderService = transportOrderService;
        this.serviceOrderService = serviceOrderService;
        this.bikeServiceService = bikeServiceService;
        this.servicePackageService = servicePackageService;
        this.serviceSlotService = serviceSlotService;
        this.incompleteUserRepository = incompleteUserRepository;
        this.userRepository = userRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.bikeRepository = bikeRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    // =================== DASHBOARD & OVERVIEW ===================

    /**
     * Dashboard z wszystkimi kluczowymi metrykami
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_DASHBOARD", null, adminEmail);

        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Podstawowe statystyki
            dashboard.put("totalUsers", userRepository.count());
            dashboard.put("totalBicycles", bikeRepository.count());
            dashboard.put("totalServices", 0); // TODO: dodać gdy będzie BikeServiceRepository
            dashboard.put("pendingOrders", transportOrderRepository.countByStatus(TransportOrder.OrderStatus.PENDING));

            // Informacje o użytkowniku
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Map<String, Object> user = new HashMap<>();
                user.put("email", auth.getName());
                dashboard.put("user", user);

                List<String> authorities = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
                dashboard.put("authorities", authorities);
            }

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.severe("Error loading dashboard for admin " + adminEmail + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania dashboardu"));
        }
    }


    /**
     * Endpoint dla wszystkich zamówień (serwisowe + transportowe)
     */
    @GetMapping("/orders/all")
    public ResponseEntity<Map<String, Object>> getAllOrdersUnified(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDateTo) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_ALL_ORDERS", buildFilterString(status, orderType, searchTerm, pickupDateFrom, pickupDateTo), adminEmail);

        try {
            // Walidacja parametrów paginacji
            if (size > 100) size = 100;
            if (size < 1) size = 20;
            if (page < 0) page = 0;

            // Pobierz wszystkie zamówienia (transport + service)
            List<UnifiedOrderResponseDto> allOrders = transportOrderService.getAllOrders();

            // Zastosuj filtry
            List<UnifiedOrderResponseDto> filteredOrders = applyOrderFilters(
                    allOrders, status, orderType, searchTerm, pickupDateFrom, pickupDateTo);

            // Sortowanie
            if ("orderDate".equals(sortBy)) {
                filteredOrders.sort((a, b) -> "desc".equalsIgnoreCase(sortDir)
                        ? b.orderDate().compareTo(a.orderDate())
                        : a.orderDate().compareTo(b.orderDate()));
            } else if ("pickupDate".equals(sortBy)) {
                filteredOrders.sort((a, b) -> "desc".equalsIgnoreCase(sortDir)
                        ? b.pickupDate().compareTo(a.pickupDate())
                        : a.pickupDate().compareTo(b.pickupDate()));
            }

            // Paginacja
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error loading all orders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania zamówień"));
        }
    }

    @GetMapping("/bike-services")
    public ResponseEntity<List<BikeServiceDto>> getAllBikeServicesForAdmin() {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_BIKE_SERVICES", "admin_view", adminEmail);

        try {
            List<BikeServiceDto> bikeServices = bikeServiceService.getAllBikeServicesForAdmin();
            return ResponseEntity.ok(bikeServices);
        } catch (Exception e) {
            logger.severe("Error loading bike services for admin: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/bike-services/{id}")
    public ResponseEntity<?> updateBikeService(
            @PathVariable Long id,
            @Valid @RequestBody BikeServiceDto bikeServiceDto) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("UPDATE_BIKE_SERVICE", "id=" + id + ", name=" + bikeServiceDto.name(), adminEmail);

        try {
            return bikeServiceService.updateBikeService(id, bikeServiceDto);
        } catch (Exception e) {
            logger.severe("Error updating bike service: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Błąd aktualizacji serwisu: " + e.getMessage()));
        }
    }

    /**
     * Usuwanie serwisu rowerowego
     */
    @DeleteMapping("/bike-services/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Tylko ADMIN może usuwać
    public ResponseEntity<?> deleteBikeService(@PathVariable Long id) {
        String adminEmail = getCurrentUserEmail();

        if (!hasAdminRole()) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Tylko administrator może usuwać serwisy"));
        }

        if (Long.parseLong(OrderValidator.internalServiceIdString) == id) {
            logAdminAction("DELETE_BIKE_SERVICE_DENIED",
                    "id=" + id + ", reason=default_service", adminEmail);

            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nie można usunąć serwisu domyślnego",
                    "reason", "Serwis domyślny jest wymagany do działania systemu"
            ));
        }

        logAdminAction("DELETE_BIKE_SERVICE", "id=" + id, adminEmail);

        try {
            return bikeServiceService.deleteBikeService(id);
        } catch (Exception e) {
            logger.severe("Error deleting bike service: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Błąd usuwania serwisu: " + e.getMessage()));
        }
    }

    @PostMapping("/bike-services")
    public ResponseEntity<?> createBikeService(@Valid @RequestBody BikeServiceDto bikeServiceDto) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("CREATE_BIKE_SERVICE", "name=" + bikeServiceDto.name(), adminEmail);

        try {
            return bikeServiceService.createBikeService(bikeServiceDto);
        } catch (Exception e) {
            logger.severe("Error creating bike service: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Błąd tworzenia serwisu: " + e.getMessage()));
        }
    }

    /**
     * Endpoint dla zamówień serwisowych
     */
    @GetMapping("/orders/service")
    public ResponseEntity<Map<String, Object>> getServiceOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDateTo) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_SERVICE_ORDERS", buildFilterString(status, "SERVICE", searchTerm, pickupDateFrom, pickupDateTo), adminEmail);

        try {
            // Pobierz tylko zamówienia serwisowe
            List<UnifiedOrderResponseDto> serviceOrders = serviceOrderService.getAllServiceOrdersAsUnified();

            // Zastosuj filtry
            List<UnifiedOrderResponseDto> filteredOrders = applyOrderFilters(
                    serviceOrders, status, "SERVICE", searchTerm, pickupDateFrom, pickupDateTo);

            // Paginacja
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error loading service orders: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania zamówień serwisowych"));
        }
    }

    /**
     * Endpoint dla zamówień transportowych
     */
    @GetMapping("/orders/transport")
    public ResponseEntity<Map<String, Object>> getTransportOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDateTo) {

        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_TRANSPORT_ORDERS", buildFilterString(status, "TRANSPORT", searchTerm, pickupDateFrom, pickupDateTo), adminEmail);

        try {
            // Pobierz tylko zamówienia transportowe
            List<UnifiedOrderResponseDto> transportOrders = transportOrderService.getAllTransportOrdersAsUnified();

            // Zastosuj filtry
            List<UnifiedOrderResponseDto> filteredOrders = applyOrderFilters(
                    transportOrders, status, "TRANSPORT", searchTerm, pickupDateFrom, pickupDateTo);

            // Paginacja i odpowiedź
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error loading transport orders: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania zamówień transportowych"));
        }
    }

    /**
     * Endpoint dla szczegółów zamówienia
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("VIEW_ORDER_DETAILS", "orderId=" + orderId, adminEmail);

        try {
            // Sprawdź w zamówieniach transportowych
            Optional<UnifiedOrderResponseDto> transportOrder = transportOrderService.getOrderAsUnified(orderId);
            if (transportOrder.isPresent()) {
                return ResponseEntity.ok(transportOrder.get());
            }

            // Sprawdź w zamówieniach serwisowych
            Optional<UnifiedOrderResponseDto> serviceOrder = serviceOrderService.getOrderAsUnified(orderId);
            if (serviceOrder.isPresent()) {
                return ResponseEntity.ok(serviceOrder.get());
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.severe("Error loading order details: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd ładowania szczegółów zamówienia"));
        }
    }

    /**
     * Bezpieczna aktualizacja statusu z walidacją
     */
    @PatchMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody Map<String, String> request) {

        String adminEmail = getCurrentUserEmail();
        String newStatus = request.get("status");

        // Walidacja danych wejściowych
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        // Walidacja czy status jest poprawny
        if (!isValidOrderStatus(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nieprawidłowy status",
                    "validStatuses", getValidOrderStatuses()
            ));
        }

        logAdminAction("UPDATE_ORDER_STATUS", "orderId=" + orderId + ", status=" + newStatus, adminEmail);

        try {
            // Sprawdź czy zamówienie istnieje
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
     * Bezpieczne usuwanie zamówienia z walidacją
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
            // Sprawdź czy zamówienie można usunąć
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
     * Zarządzanie użytkownikami z paginacją i filtrowaniem
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
            // Walidacja parametrów
            if (size > 100) size = 100;
            if (size < 1) size = 20;
            if (page < 0) page = 0;

            String validatedSortBy = validateSortField(sortBy, Arrays.asList("createdAt", "email", "firstName", "lastName"));
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

            // Użyj repository z paginacją
            Page<IncompleteUser> userPage = getUsersWithFilters(pageable, searchTerm, role, verified);

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
     * Bezpieczna zmiana ról użytkownika
     */
    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')") // Tylko ADMIN może zmieniać role
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, Set<String>> request) {

        String adminEmail = getCurrentUserEmail();
        Set<String> newRoles = request.get("roles");

        // Walidacja ról
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

            // Nie pozwól adminowi usunąć sobie uprawnień ADMIN
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
     * Zarządzanie pakietami serwisowymi z walidacją
     */
    @PostMapping("/service-packages")
    public ResponseEntity<?> createServicePackage(@Valid @RequestBody ServicePackageDto packageDto) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("CREATE_SERVICE_PACKAGE", "code=" + packageDto.code(), adminEmail);

        try {
            // Dodatkowa walidacja biznesowa
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
     * Import serwisów z walidacją pliku
     */
    @PostMapping("/bike-services/import")
    public ResponseEntity<?> importBikeServices(@RequestParam("file") MultipartFile file) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("IMPORT_BIKE_SERVICES", "filename=" + file.getOriginalFilename(), adminEmail);

        try {
            // Walidacja pliku
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
     * Zarządzanie konfiguracją slotów z walidacją
     */
    @PostMapping("/service-slots/config")
    public ResponseEntity<?> createSlotConfig(@Valid @RequestBody ServiceSlotConfigDto configDto) {
        String adminEmail = getCurrentUserEmail();
        logAdminAction("CREATE_SLOT_CONFIG",
                "startDate=" + configDto.startDate() + ", maxBikes=" + configDto.maxBikesPerDay(), adminEmail);

        try {
            // Walidacja biznesowa
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

    @PatchMapping({"/orders/service/{orderId}/status", "/orders/transport/{orderId}/status"})
    public ResponseEntity<?> updateServiceOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody Map<String, String> request) {

        String adminEmail = getCurrentUserEmail();
        String newStatus = request.get("status");

        // Walidacja danych wejściowych
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status jest wymagany"));
        }

        logAdminAction("UPDATE_SERVICE_ORDER_STATUS", "orderId=" + orderId + ", status=" + newStatus, adminEmail);

        try {
            // Deleguj do serwisu
            return transportOrderService.updateOrderStatus(orderId, newStatus, adminEmail);
        } catch (Exception e) {
            logger.severe("Error updating service order status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Błąd aktualizacji statusu"));
        }
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
                        (order.clientEmail() != null && order.clientEmail().toLowerCase().contains(searchTerm.toLowerCase())) ||
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
        return transportOrderRepository.existsById(orderId) || serviceOrderRepository.existsById(orderId);
    }

    private boolean canDeleteOrder(Long orderId) {
        // TODO: Implementacja reguł biznesowych dla usuwania
        return true;
    }

    private Page<IncompleteUser> getUsersWithFilters(Pageable pageable, String searchTerm, String role, Boolean verified) {
        // TODO: Implementacja filtrowanego wyszukiwania użytkowników
        return incompleteUserRepository.findAll(pageable);
    }

    private Map<String, Object> mapUserToDto(IncompleteUser user) {
        if(user instanceof User fullUser) {
            return Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", fullUser.getFirstName() != null ? fullUser.getFirstName() : "",
                    "lastName", fullUser.getLastName() != null ? fullUser.getLastName() : "",
                    "verified", fullUser.isVerified(),
                    "roles", user.getRoles(),
                    "createdAt", user.getCreatedAt()
            );
        } else {
            return Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName",  "",
                    "lastName",  "",
                    "verified", false,
                    "roles", user.getRoles(),
                    "createdAt", user.getCreatedAt()
            );
        }
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