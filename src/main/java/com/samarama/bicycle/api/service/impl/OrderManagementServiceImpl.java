package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.OrderManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderManagementServiceImpl implements OrderManagementService {

    private static final Logger logger = Logger.getLogger(OrderManagementServiceImpl.class.getName());

    private final ServiceOrderRepository serviceOrderRepository;
    private final TransportOrderRepository transportOrderRepository;
    private final UserRepository userRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;

    public OrderManagementServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            TransportOrderRepository transportOrderRepository,
            UserRepository userRepository,
            ServicePackageRepository servicePackageRepository,
            BikeServiceRepository bikeServiceRepository,
            IncompleteBikeRepository incompleteBikeRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
    }

    // === ADMIN - ZAMÓWIENIA SERWISOWE ===

    @Override
    public Page<ServiceAndTransportOrdersDto> getAllServiceOrders(OrderFilterDto filter, Pageable pageable) {
        Specification<ServiceOrder> spec = buildServiceOrderSpecification(filter);
        Page<ServiceOrder> orders = serviceOrderRepository.findAll(spec, pageable);

        List<ServiceAndTransportOrdersDto> dtos = orders.getContent().stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> searchServiceOrders(String searchTerm) {
        List<ServiceOrder> orders = searchServiceOrdersByClientInfo(searchTerm);
        return orders.stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrder(Long orderId, ServiceOrderDto dto, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Admin może modyfikować na każdym etapie
        updateServiceOrderFields(order, dto);
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane",
                "order", ServiceAndTransportOrdersDto.fromServiceOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteServiceOrder(Long orderId, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();

        // Admin może usunąć na każdym etapie
        serviceOrderRepository.delete(order);

        logger.info("Admin " + adminEmail + " deleted service order " + orderId);

        return ResponseEntity.ok(Map.of("message", "Zamówienie zostało usunięte"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceOrderStatus(Long orderId, String newStatus, String adminEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Order.OrderStatus status = Order.OrderStatus.valueOf(newStatus);
            ServiceOrder order = orderOpt.get();

            order.setStatus(status);
            order.setLastModifiedBy(adminEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "message", "Status zamówienia został zaktualizowany",
                    "newStatus", status.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy status: " + newStatus));
        }
    }

    // === ADMIN - ZAMÓWIENIA TRANSPORTOWE ===

    @Override
    public Page<ServiceAndTransportOrdersDto> getAllTransportOrders(OrderFilterDto filter, Pageable pageable) {
        Specification<TransportOrder> spec = buildTransportOrderSpecification(filter);
        Page<TransportOrder> orders = transportOrderRepository.findAll(spec, pageable);

        List<ServiceAndTransportOrdersDto> dtos = orders.getContent().stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> searchTransportOrders(String searchTerm) {
        List<TransportOrder> orders = searchTransportOrdersByClientInfo(searchTerm);
        return orders.stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateTransportOrder(Long orderId, TransportOrderDto dto, String adminEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();

        updateTransportOrderFields(order, dto);
        order.setLastModifiedBy(adminEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        transportOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie transportowe zostało zaktualizowane",
                "order", ServiceAndTransportOrdersDto.fromTransportOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteTransportOrder(Long orderId, String adminEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        transportOrderRepository.deleteById(orderId);
        logger.info("Admin " + adminEmail + " deleted transport order " + orderId);

        return ResponseEntity.ok(Map.of("message", "Zamówienie transportowe zostało usunięte"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateTransportOrderStatus(Long orderId, String newStatus, String adminEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Sprawdź czy to status transportu czy zamówienia
            if (isTransportStatus(newStatus)) {
                TransportOrder.TransportStatus transportStatus = TransportOrder.TransportStatus.valueOf(newStatus);
                TransportOrder order = orderOpt.get();

                order.setTransportStatus(transportStatus);
                order.setLastModifiedBy(adminEmail);
                order.setLastModifiedDate(LocalDateTime.now());

                if (transportStatus == TransportOrder.TransportStatus.DELIVERED_TO_SERVICE) {
                    order.setActualDeliveryTime(LocalDateTime.now());
                }

                transportOrderRepository.save(order);

                return ResponseEntity.ok(Map.of(
                        "message", "Status transportu został zaktualizowany",
                        "newTransportStatus", transportStatus.toString()
                ));
            } else {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(newStatus);
                TransportOrder order = orderOpt.get();

                order.setStatus(orderStatus);
                order.setLastModifiedBy(adminEmail);
                order.setLastModifiedDate(LocalDateTime.now());

                transportOrderRepository.save(order);

                return ResponseEntity.ok(Map.of(
                        "message", "Status zamówienia został zaktualizowany",
                        "newStatus", orderStatus.toString()
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowy status: " + newStatus));
        }
    }

    // === ADMIN - WSZYSTKIE ZAMÓWIENIA ===

    @Override
    public Page<ServiceAndTransportOrdersDto> getAllOrders(OrderFilterDto filter, Pageable pageable) {
        // Pobierz zamówienia serwisowe
        List<ServiceOrder> serviceOrders = serviceOrderRepository.findAll();
        List<ServiceAndTransportOrdersDto> serviceDtos = serviceOrders.stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .collect(Collectors.toList());

        // Pobierz zamówienia transportowe
        List<TransportOrder> transportOrders = transportOrderRepository.findAll();
        List<ServiceAndTransportOrdersDto> transportDtos = transportOrders.stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .collect(Collectors.toList());

        // Połącz wszystkie zamówienia
        List<ServiceAndTransportOrdersDto> allOrders = Stream.concat(
                serviceDtos.stream(),
                transportDtos.stream()
        ).collect(Collectors.toList());

        // Zastosuj filtry
        List<ServiceAndTransportOrdersDto> filteredOrders = applyFilters(allOrders, filter);

        // Sortowanie
        sortOrders(filteredOrders, filter);

        // Paginacja ręczna
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredOrders.size());
        List<ServiceAndTransportOrdersDto> pageContent = start < filteredOrders.size() ?
                filteredOrders.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, filteredOrders.size());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> searchAllOrders(String searchTerm) {
        List<ServiceOrder> serviceOrders = searchServiceOrdersByClientInfo(searchTerm);
        List<TransportOrder> transportOrders = searchTransportOrdersByClientInfo(searchTerm);

        List<ServiceAndTransportOrdersDto> result = new ArrayList<>();
        result.addAll(serviceOrders.stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .collect(Collectors.toList()));
        result.addAll(transportOrders.stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .collect(Collectors.toList()));

        return result;
    }

    // === KLIENT - SWOJE ZAMÓWIENIA ===

    @Override
    public List<ServiceAndTransportOrdersDto> getUserServiceOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<ServiceOrder> orders = serviceOrderRepository.findByClient(user);

        return orders.stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .sorted(Comparator.comparing(ServiceAndTransportOrdersDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> getUserTransportOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<TransportOrder> orders = transportOrderRepository.findByClient(user);

        return orders.stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .sorted(Comparator.comparing(ServiceAndTransportOrdersDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> getUserAllOrders(String userEmail) {
        List<ServiceAndTransportOrdersDto> serviceOrders = getUserServiceOrders(userEmail);
        List<ServiceAndTransportOrdersDto> transportOrders = getUserTransportOrders(userEmail);

        return Stream.concat(serviceOrders.stream(), transportOrders.stream())
                .sorted(Comparator.comparing(ServiceAndTransportOrdersDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateUserServiceOrder(Long orderId, ServiceOrderDto dto, String userEmail) {
        Optional<ServiceOrder> orderOpt = serviceOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceOrder order = orderOpt.get();
        User user = getUserByEmail(userEmail);

        // Sprawdź właściciela
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        // Sprawdź czy można modyfikować
        if (!canModifyOrder(order, false)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        updateServiceOrderFields(order, dto);
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        serviceOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie zostało zaktualizowane",
                "order", ServiceAndTransportOrdersDto.fromServiceOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateUserTransportOrder(Long orderId, TransportOrderDto dto, String userEmail) {
        Optional<TransportOrder> orderOpt = transportOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransportOrder order = orderOpt.get();
        User user = getUserByEmail(userEmail);

        // Sprawdź właściciela
        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        // Sprawdź czy można modyfikować
        if (!canModifyOrder(order, false)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        updateTransportOrderFields(order, dto);
        order.setLastModifiedBy(userEmail);
        order.setLastModifiedDate(LocalDateTime.now());

        transportOrderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Zamówienie transportowe zostało zaktualizowane",
                "order", ServiceAndTransportOrdersDto.fromTransportOrder(order)
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> cancelUserOrder(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);

        // Sprawdź w zamówieniach serwisowych
        Optional<ServiceOrder> serviceOrderOpt = serviceOrderRepository.findById(orderId);
        if (serviceOrderOpt.isPresent()) {
            ServiceOrder order = serviceOrderOpt.get();

            if (!order.getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
            }

            if (!canModifyOrder(order, false)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Zamówienie można anulować tylko w statusie PENDING lub CONFIRMED"
                ));
            }

            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            serviceOrderRepository.save(order);

            return ResponseEntity.ok(Map.of("message", "Zamówienie zostało anulowane"));
        }

        // Sprawdź w zamówieniach transportowych
        Optional<TransportOrder> transportOrderOpt = transportOrderRepository.findById(orderId);
        if (transportOrderOpt.isPresent()) {
            TransportOrder order = transportOrderOpt.get();

            if (!order.getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
            }

            if (!canModifyOrder(order, false)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Zamówienie można anulować tylko w statusie PENDING lub CONFIRMED"
                ));
            }

            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setLastModifiedBy(userEmail);
            order.setLastModifiedDate(LocalDateTime.now());

            transportOrderRepository.save(order);

            return ResponseEntity.ok(Map.of("message", "Zamówienie transportowe zostało anulowane"));
        }

        return ResponseEntity.notFound().build();
    }

    // === POMOCNICZE ===

    @Override
    public ResponseEntity<ServiceAndTransportOrdersDto> getOrderDetails(Long orderId, String userEmail, boolean isAdmin) {
        User user = getUserByEmail(userEmail);

        // Sprawdź w zamówieniach serwisowych
        Optional<ServiceOrder> serviceOrderOpt = serviceOrderRepository.findById(orderId);
        if (serviceOrderOpt.isPresent()) {
            ServiceOrder order = serviceOrderOpt.get();

            if (!isAdmin && !order.getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }

            return ResponseEntity.ok(ServiceAndTransportOrdersDto.fromServiceOrder(order));
        }

        // Sprawdź w zamówieniach transportowych
        Optional<TransportOrder> transportOrderOpt = transportOrderRepository.findById(orderId);
        if (transportOrderOpt.isPresent()) {
            TransportOrder order = transportOrderOpt.get();

            if (!isAdmin && !order.getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }

            return ResponseEntity.ok(ServiceAndTransportOrdersDto.fromTransportOrder(order));
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    public boolean canModifyOrder(Order order, boolean isAdmin) {
        if (isAdmin) {
            return true; // Admin może modyfikować na każdym etapie
        }

        // Klient może modyfikować tylko PENDING i CONFIRMED
        return order.getStatus() == Order.OrderStatus.PENDING ||
                order.getStatus() == Order.OrderStatus.CONFIRMED;
    }

    // === METODY POMOCNICZE ===

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private Specification<ServiceOrder> buildServiceOrderSpecification(OrderFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.hasDateFilter()) {
                if (filter.pickupDateFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("pickupDate"), filter.pickupDateFrom()));
                }
                if (filter.pickupDateTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("pickupDate"), filter.pickupDateTo()));
                }
            }

            if (filter.hasStatusFilter()) {
                predicates.add(cb.equal(root.get("status"), Order.OrderStatus.valueOf(filter.status())));
            }

            if (filter.hasSearchTerm()) {
                String searchPattern = "%" + filter.searchTerm().toLowerCase() + "%";
                Predicate emailPredicate = cb.like(cb.lower(root.get("client").get("email")), searchPattern);
                Predicate phonePredicate = cb.like(cb.lower(root.get("client").get("phoneNumber")), searchPattern);
                predicates.add(cb.or(emailPredicate, phonePredicate));
            }

            if (filter.servicePackageCode() != null) {
                predicates.add(cb.equal(root.get("servicePackageCode"), filter.servicePackageCode()));
            }

            if (filter.servicePackageId() != null) {
                predicates.add(cb.equal(root.get("servicePackage").get("id"), filter.servicePackageId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<TransportOrder> buildTransportOrderSpecification(OrderFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.hasDateFilter()) {
                if (filter.pickupDateFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("pickupDate"), filter.pickupDateFrom()));
                }
                if (filter.pickupDateTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("pickupDate"), filter.pickupDateTo()));
                }
            }

            if (filter.hasStatusFilter()) {
                predicates.add(cb.equal(root.get("status"), Order.OrderStatus.valueOf(filter.status())));
            }

            if (filter.hasSearchTerm()) {
                String searchPattern = "%" + filter.searchTerm().toLowerCase() + "%";
                Predicate emailPredicate = cb.like(cb.lower(root.get("client").get("email")), searchPattern);
                Predicate phonePredicate = cb.like(cb.lower(root.get("client").get("phoneNumber")), searchPattern);
                predicates.add(cb.or(emailPredicate, phonePredicate));
            }

            if (filter.transportStatus() != null) {
                predicates.add(cb.equal(root.get("transportStatus"),
                        TransportOrder.TransportStatus.valueOf(filter.transportStatus())));
            }

            if (filter.transportType() != null) {
                predicates.add(cb.equal(root.get("transportType"),
                        TransportOrder.TransportType.valueOf(filter.transportType())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<ServiceOrder> searchServiceOrdersByClientInfo(String searchTerm) {
        return serviceOrderRepository.searchByClientInfo(searchTerm);
    }

    private List<TransportOrder> searchTransportOrdersByClientInfo(String searchTerm) {
        return transportOrderRepository.searchByClientInfo(searchTerm);
    }

    private List<ServiceAndTransportOrdersDto> applyFilters(List<ServiceAndTransportOrdersDto> orders, OrderFilterDto filter) {
        return orders.stream()
                .filter(order -> {
                    // Filtr dat
                    if (filter.hasDateFilter()) {
                        if (filter.pickupDateFrom() != null && order.pickupDate().isBefore(filter.pickupDateFrom())) {
                            return false;
                        }
                        if (filter.pickupDateTo() != null && order.pickupDate().isAfter(filter.pickupDateTo())) {
                            return false;
                        }
                    }

                    // Filtr statusu
                    if (filter.hasStatusFilter() && !filter.status().equals(order.status())) {
                        return false;
                    }

                    // Filtr typu zamówienia
                    if (filter.orderType() != null && !filter.orderType().equals(order.orderType())) {
                        return false;
                    }

                    // Search term
                    if (filter.hasSearchTerm()) {
                        String searchTerm = filter.searchTerm().toLowerCase();
                        boolean matchesEmail = order.clientEmail() != null &&
                                order.clientEmail().toLowerCase().contains(searchTerm);
                        boolean matchesPhone = order.clientPhone() != null &&
                                order.clientPhone().toLowerCase().contains(searchTerm);
                        if (!matchesEmail && !matchesPhone) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private void sortOrders(List<ServiceAndTransportOrdersDto> orders, OrderFilterDto filter) {
        String sortBy = filter.getEffectiveSortBy();
        boolean ascending = "ASC".equalsIgnoreCase(filter.getEffectiveSortOrder());

        Comparator<ServiceAndTransportOrdersDto> comparator = switch (sortBy) {
            case "pickupDate" -> Comparator.comparing(ServiceAndTransportOrdersDto::pickupDate);
            case "status" -> Comparator.comparing(ServiceAndTransportOrdersDto::status);
            case "client" -> Comparator.comparing(ServiceAndTransportOrdersDto::clientEmail);
            case "price" -> Comparator.comparing(ServiceAndTransportOrdersDto::price);
            default -> Comparator.comparing(ServiceAndTransportOrdersDto::orderDate);
        };

        if (!ascending) {
            comparator = comparator.reversed();
        }

        orders.sort(comparator);
    }

    private void updateServiceOrderFields(ServiceOrder order, ServiceOrderDto dto) {
        if (dto.pickupDate() != null) {
            order.setPickupDate(dto.pickupDate());
        }
        if (dto.pickupAddress() != null) {
            order.setPickupAddress(dto.pickupAddress());
        }

        // Aktualizuj współrzędne odbioru
        if (dto.pickupLatitude() != null) {
            order.setPickupLatitude(dto.pickupLatitude());
        }
        if (dto.pickupLongitude() != null) {
            order.setPickupLongitude(dto.pickupLongitude());
        }

        // Aktualizuj rower - sprawdź czy należy do klienta
        if (dto.bicycleIds() != null && !dto.bicycleIds().isEmpty()) {
            Long bicycleId = dto.bicycleIds().get(0); // Weź pierwszy rower
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
            if (bikeOpt.isPresent()) {
                IncompleteBike bike = bikeOpt.get();
                if (bike.getOwner().getId().equals(order.getClient().getId())) {
                    order.setBicycle(bike);
                }
            }
        }

        if (dto.servicePackageId() != null) {
            ServicePackage servicePackage = servicePackageRepository.findById(dto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setPrice(servicePackage.getPrice());
        } else if (dto.servicePackageCode() != null) {
            ServicePackage servicePackage = servicePackageRepository.findByCode(dto.servicePackageCode())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));
            order.setServicePackage(servicePackage);
            order.setServicePackageCode(servicePackage.getCode());
            order.setPrice(servicePackage.getPrice());
        }

        if (dto.additionalNotes() != null) {
            order.setAdditionalNotes(dto.additionalNotes());
        }
    }

    private void updateTransportOrderFields(TransportOrder order, TransportOrderDto dto) {
        if (dto.pickupDate() != null) {
            order.setPickupDate(dto.pickupDate());
        }
        if (dto.pickupAddress() != null) {
            order.setPickupAddress(dto.pickupAddress());
        }
        if (dto.pickupLatitude() != null) {
            order.setPickupLatitude(dto.pickupLatitude());
        }
        if (dto.pickupLongitude() != null) {
            order.setPickupLongitude(dto.pickupLongitude());
        }
        if (dto.deliveryAddress() != null) {
            order.setDeliveryAddress(dto.deliveryAddress());
        }
        if (dto.deliveryLatitude() != null) {
            order.setDeliveryLatitude(dto.deliveryLatitude());
        }
        if (dto.deliveryLongitude() != null) {
            order.setDeliveryLongitude(dto.deliveryLongitude());
        }

        // Aktualizuj rower
        if (dto.bicycleIds() != null && !dto.bicycleIds().isEmpty()) {
            Long bicycleId = dto.bicycleIds().get(0); // Weź pierwszy rower
            Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
            if (bikeOpt.isPresent()) {
                IncompleteBike bike = bikeOpt.get();
                if (bike.getOwner().getId().equals(order.getClient().getId())) {
                    order.setBicycle(bike);
                }
            }
        }

        if (dto.additionalNotes() != null) {
            order.setAdditionalNotes(dto.additionalNotes());
        }
    }

    private boolean isTransportStatus(String status) {
        try {
            TransportOrder.TransportStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}