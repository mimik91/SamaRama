package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.ServiceOrderRepository;
import com.samarama.bicycle.api.service.ServiceOrderService;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.ServiceSlotService;
import com.samarama.bicycle.api.service.helper.ServiceOrderHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private static final Logger logger = Logger.getLogger(ServiceOrderServiceImpl.class.getName());

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceSlotService serviceSlotService;
    private final EmailService emailService;
    private final ServiceOrderHelper serviceOrderHelper;
    private final ServiceOrderValidator serviceOrderValidator;

    public ServiceOrderServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            ServiceSlotService serviceSlotService,
            EmailService emailService,
            ServiceOrderHelper serviceOrderHelper,
            ServiceOrderValidator serviceOrderValidator) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.serviceSlotService = serviceSlotService;
        this.emailService = emailService;
        this.serviceOrderHelper = serviceOrderHelper;
        this.serviceOrderValidator = serviceOrderValidator;
    }

    // === CREATION METHODS ===

    @Override
    @Transactional
    public ResponseEntity<?> createServiceOrder(ServiceOrTransportOrderDto dto, String userEmail) {
        try {
            // Validation
            ServiceOrderValidator.ValidationResult validation = serviceOrderValidator.validateUserServiceOrder(dto);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", validation.getFirstError()));
            }

            // Check slot availability
            int bikesCount = dto.bicycleIds().size();
            ServiceOrderValidator.SlotValidationResult slotValidation =
                    serviceOrderValidator.validateSlotAvailability(dto.pickupDate(), bikesCount);

            if (!slotValidation.isAvailable()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", slotValidation.getMessage(),
                        "details", slotValidation.getAdditionalInfo()
                ));
            }

            // Get entities
            User user = serviceOrderHelper.getUserByEmail(userEmail);
            ServicePackage servicePackage = serviceOrderHelper.getServicePackage(dto);
            BikeService ownService = serviceOrderHelper.getOwnService();
            List<IncompleteBike> bikes = serviceOrderHelper.validateAndGetBikes(dto.bicycleIds(), user.getId());

            // Create orders
            List<ServiceOrder> orders = serviceOrderHelper.createServiceOrdersForUser(
                    bikes, user, dto, servicePackage, ownService);

            // Save and notify
            List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);
            sendEmailNotifications(savedOrders);

            // Log success
            savedOrders.forEach(order -> serviceOrderHelper.logServiceOrderCreation(order, userEmail));

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                    "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                    "orderCount", savedOrders.size()
            ));

        } catch (RuntimeException e) {
            logger.warning("Failed to create service order for user " + userEmail + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> createGuestServiceOrder(ServiceOrTransportOrderDto dto) {
        try {
            // Validation
            ServiceOrderValidator.ValidationResult validation = serviceOrderValidator.validateGuestServiceOrder(dto);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", validation.getFirstError()));
            }

            // Check slot availability
            int bikesCount = dto.bicycles().size();
            ServiceOrderValidator.SlotValidationResult slotValidation =
                    serviceOrderValidator.validateSlotAvailability(dto.pickupDate(), bikesCount);

            if (!slotValidation.isAvailable()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", slotValidation.getMessage(),
                        "details", slotValidation.getAdditionalInfo()
                ));
            }

            // Get entities
            IncompleteUser guestUser = serviceOrderHelper.createOrFindIncompleteUser(dto.clientEmail(), dto.clientPhone());
            ServicePackage servicePackage = serviceOrderHelper.getServicePackage(dto);
            BikeService ownService = serviceOrderHelper.getOwnService();
            List<IncompleteBike> bikes = serviceOrderHelper.createIncompleteBikes(dto.bicycles(), guestUser);

            // Create orders
            List<ServiceOrder> orders = serviceOrderHelper.createServiceOrdersForGuest(
                    bikes, guestUser, dto, servicePackage, ownService);

            // Save orders
            List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                    "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                    "guestUserId", guestUser.getId()
            ));

        } catch (RuntimeException e) {
            logger.warning("Failed to create guest service order: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // === RETRIEVAL METHODS ===

    @Override
    public List<UnifiedOrderResponseDto> getUserServiceOrders(String userEmail) {
        User user = serviceOrderHelper.getUserByEmail(userEmail);
        List<ServiceOrder> orders = serviceOrderRepository.findByClient(user);

        return orders.stream()
                .map(UnifiedOrderResponseDto::fromServiceOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<UnifiedOrderResponseDto> getServiceOrderDetails(Long orderId, String userEmail) {
        User user = serviceOrderHelper.getUserByEmail(userEmail);

        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Check ownership
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(UnifiedOrderResponseDto.fromServiceOrder(order));
    }

    // === UPDATE METHODS ===

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrTransportOrderDto dto, String userEmail) {
        try {
            User user = serviceOrderHelper.getUserByEmail(userEmail);
            ServiceOrder order = getServiceOrderForUser(orderId, user);

            // Check if can modify
            if (!serviceOrderValidator.canUserModifyOrder(order, userEmail)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
                ));
            }

            // Update fields
            serviceOrderHelper.updateServiceOrderFields(order, dto);
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienie zostało zaktualizowane",
                    "order", UnifiedOrderResponseDto.fromServiceOrder(order)
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // === SERVICE LIFECYCLE METHODS ===

    @Override
    @Transactional
    public ResponseEntity<?> startService(Long orderId, String userEmail) {
        try {
            ServiceOrder order = getServiceOrderById(orderId);

            ServiceOrderValidator.ValidationResult validation = serviceOrderValidator.validateServiceStart(order);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", validation.getFirstError()));
            }

            order.startService();
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);
            serviceOrderHelper.logServiceAction("START_SERVICE", orderId, userEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis został rozpoczęty",
                    "serviceStartDate", order.getServiceStartDate()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> completeService(Long orderId, String userEmail) {
        try {
            ServiceOrder order = getServiceOrderById(orderId);

            ServiceOrderValidator.ValidationResult validation = serviceOrderValidator.validateServiceCompletion(order);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", validation.getFirstError()));
            }

            order.completeService();
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);
            serviceOrderHelper.logServiceAction("COMPLETE_SERVICE", orderId, userEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Serwis został zakończony",
                    "serviceCompletionDate", order.getServiceCompletionDate(),
                    "serviceDuration", order.getServiceDurationDisplay()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceNotes(Long orderId, String notes, String userEmail) {
        try {
            ServiceOrder order = getServiceOrderById(orderId);

            order.setServiceNotes(notes);
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);
            serviceOrderHelper.logServiceAction("UPDATE_NOTES", orderId, userEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Notatki serwisowe zostały zaktualizowane",
                    "serviceNotes", notes
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // === ADMIN METHODS ===

    @Override
    public List<UnifiedOrderResponseDto> getAllServiceOrders() {
        List<ServiceOrder> orders = serviceOrderRepository.findAllActive();
        return orders.stream()
                .map(UnifiedOrderResponseDto::fromServiceOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrderByAdmin(Long orderId, ServiceOrTransportOrderDto dto, String adminEmail) {
        try {
            ServiceOrder order = getServiceOrderById(orderId);

            serviceOrderHelper.updateServiceOrderFields(order, dto);
            order.setLastModifiedBy(adminEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);
            serviceOrderHelper.logServiceAction("ADMIN_UPDATE", orderId, adminEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienie zostało zaktualizowane",
                    "order", UnifiedOrderResponseDto.fromServiceOrder(order)
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteServiceOrder(Long orderId, String adminEmail) {
        if (!serviceOrderRepository.existsById(orderId)) {
            return ResponseEntity.notFound().build();
        }

        serviceOrderRepository.deleteById(orderId);
        serviceOrderHelper.logServiceAction("ADMIN_DELETE", orderId, adminEmail);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało usunięte"));
    }

    // === STATISTICS METHODS ===

    @Override
    public List<Object[]> getServicePackageStatistics() {
        return serviceOrderRepository.getServicePackageStatistics();
    }


    @Override
    public List<Object[]> getServiceRevenue() {
        return serviceOrderRepository.getServicePackageRevenue();
    }

    // === PRIVATE HELPER METHODS ===

    private ServiceOrder getServiceOrderById(Long orderId) {
        return serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Service order not found: " + orderId));
    }

    private ServiceOrder getServiceOrderForUser(Long orderId, User user) {
        ServiceOrder order = getServiceOrderById(orderId);

        if (!order.getClient().getId().equals(user.getId())) {
            throw new RuntimeException("Brak uprawnień do zamówienia: " + orderId);
        }

        return order;
    }

    private void sendEmailNotifications(List<ServiceOrder> orders) {
        for (ServiceOrder order : orders) {
            try {
                emailService.sendOrderNotificationEmail(order);
            } catch (Exception e) {
                logger.warning("Failed to send email notification for service order: " + order.getId());
            }
        }
    }

    // Dodaj te metody do ServiceOrderServiceImpl

    @Override
    public List<UnifiedOrderResponseDto> getAllServiceOrdersAsUnified() {
        List<ServiceOrder> orders = serviceOrderRepository.findAllActive();
        return orders.stream()
                .map(UnifiedOrderResponseDto::fromServiceOrder)
                .sorted(Comparator.comparing(UnifiedOrderResponseDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UnifiedOrderResponseDto> getOrderAsUnified(Long orderId) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }

        ServiceOrder order = orderOpt.get();
        return Optional.of(UnifiedOrderResponseDto.fromServiceOrder(order));
    }
}