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
    private final OrderManagementHelper helper;

    public OrderManagementServiceImpl(
            ServiceOrderRepository serviceOrderRepository,
            TransportOrderRepository transportOrderRepository,
            UserRepository userRepository,
            ServicePackageRepository servicePackageRepository,
            BikeServiceRepository bikeServiceRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            OrderManagementHelper helper) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.helper = helper;
    }

    // === ADMIN - ZAMÓWIENIA SERWISOWE ===

    @Override
    public Page<ServiceAndTransportOrdersDto> getAllServiceOrders(OrderFilterDto filter, Pageable pageable) {
        Specification<ServiceOrder> spec = helper.buildServiceOrderSpecification(filter);
        Page<ServiceOrder> orders = serviceOrderRepository.findAll(spec, pageable);

        List<ServiceAndTransportOrdersDto> dtos = orders.getContent().stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> searchServiceOrders(String searchTerm) {
        List<ServiceOrder> orders = serviceOrderRepository.searchByClientInfo(searchTerm);
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
        helper.updateServiceOrderFields(order, dto);
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

        serviceOrderRepository.deleteById(orderId);
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
        Specification<TransportOrder> spec = helper.buildTransportOrderSpecification(filter);
        Page<TransportOrder> orders = transportOrderRepository.findAll(spec, pageable);

        List<ServiceAndTransportOrdersDto> dtos = orders.getContent().stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> searchTransportOrders(String searchTerm) {
        List<TransportOrder> orders = transportOrderRepository.searchByClientInfo(searchTerm);
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
        helper.updateTransportOrderFields(order, dto);
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
            TransportOrder order = orderOpt.get();

            if (helper.isTransportStatus(newStatus)) {
                TransportOrder.TransportStatus transportStatus = TransportOrder.TransportStatus.valueOf(newStatus);
                order.setTransportStatus(transportStatus);

                if (transportStatus == TransportOrder.TransportStatus.DELIVERED_TO_SERVICE) {
                    order.setActualDeliveryTime(LocalDateTime.now());
                }

                order.setLastModifiedBy(adminEmail);
                order.setLastModifiedDate(LocalDateTime.now());
                transportOrderRepository.save(order);

                return ResponseEntity.ok(Map.of(
                        "message", "Status transportu został zaktualizowany",
                        "newTransportStatus", transportStatus.toString()
                ));
            } else {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(newStatus);
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
        List<ServiceOrder> serviceOrders = serviceOrderRepository.findAll();
        List<ServiceAndTransportOrdersDto> serviceDtos = serviceOrders.stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .collect(Collectors.toList());

        List<TransportOrder> transportOrders = transportOrderRepository.findAll();
        List<ServiceAndTransportOrdersDto> transportDtos = transportOrders.stream()
                .map(ServiceAndTransportOrdersDto::fromTransportOrder)
                .collect(Collectors.toList());

        List<ServiceAndTransportOrdersDto> allOrders = Stream.concat(
                serviceDtos.stream(),
                transportDtos.stream()
        ).collect(Collectors.toList());

        List<ServiceAndTransportOrdersDto> filteredOrders = helper.applyFilters(allOrders, filter);
        helper.sortOrders(filteredOrders, filter);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredOrders.size());
        List<ServiceAndTransportOrdersDto> pageContent = start < filteredOrders.size() ?
                filteredOrders.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, filteredOrders.size());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> searchAllOrders(String searchTerm) {
        List<ServiceOrder> serviceOrders = serviceOrderRepository.searchByClientInfo(searchTerm);
        List<TransportOrder> transportOrders = transportOrderRepository.searchByClientInfo(searchTerm);

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
        User user = helper.getUserByEmail(userEmail);
        List<ServiceOrder> orders = serviceOrderRepository.findByClient(user);

        return orders.stream()
                .map(ServiceAndTransportOrdersDto::fromServiceOrder)
                .sorted(Comparator.comparing(ServiceAndTransportOrdersDto::orderDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceAndTransportOrdersDto> getUserTransportOrders(String userEmail) {
        User user = helper.getUserByEmail(userEmail);
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
        User user = helper.getUserByEmail(userEmail);

        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        if (!canModifyOrder(order, false)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        helper.updateServiceOrderFields(order, dto);
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
        User user = helper.getUserByEmail(userEmail);

        if (!order.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Brak uprawnień"));
        }

        if (!canModifyOrder(order, false)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED"
            ));
        }

        helper.updateTransportOrderFields(order, dto);
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
        User user = helper.getUserByEmail(userEmail);

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
        User user = helper.getUserByEmail(userEmail);

        Optional<ServiceOrder> serviceOrderOpt = serviceOrderRepository.findById(orderId);
        if (serviceOrderOpt.isPresent()) {
            ServiceOrder order = serviceOrderOpt.get();

            if (!isAdmin && !order.getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }

            return ResponseEntity.ok(ServiceAndTransportOrdersDto.fromServiceOrder(order));
        }

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
            return true;
        }

        return order.getStatus() == Order.OrderStatus.PENDING ||
                order.getStatus() == Order.OrderStatus.CONFIRMED;
    }
}