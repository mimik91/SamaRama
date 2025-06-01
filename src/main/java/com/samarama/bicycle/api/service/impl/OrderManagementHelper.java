package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.*;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderManagementHelper {

    private final UserRepository userRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final IncompleteUserRepository incompleteUserRepository;

    public OrderManagementHelper(
            UserRepository userRepository,
            ServicePackageRepository servicePackageRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            IncompleteUserRepository incompleteUserRepository) {
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.incompleteUserRepository = incompleteUserRepository;
    }

    // === METODY POMOCNICZE ===

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // === SPECIFICATIONS ===

    public Specification<ServiceOrder> buildServiceOrderSpecification(OrderFilterDto filter) {
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

    public Specification<TransportOrder> buildTransportOrderSpecification(OrderFilterDto filter) {
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

    // === FILTERING AND SORTING ===

    public List<ServiceAndTransportOrdersDto> applyFilters(List<ServiceAndTransportOrdersDto> orders, OrderFilterDto filter) {
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

    public void sortOrders(List<ServiceAndTransportOrdersDto> orders, OrderFilterDto filter) {
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

    // === UPDATE METHODS ===

    public void updateServiceOrderFields(ServiceOrder order, ServiceOrderDto dto) {
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

    public void updateTransportOrderFields(TransportOrder order, TransportOrderDto dto) {
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

        if (dto.transportPrice() != null) {
            order.setTransportPrice(dto.transportPrice());
            order.setPrice(dto.transportPrice()); // Dla transportu TYLKO cena transportu
        }

        if (dto.pickupTimeFrom() != null) {
            order.setPickupTimeFrom(dto.pickupTimeFrom());
        }

        if (dto.pickupTimeTo() != null) {
            order.setPickupTimeTo(dto.pickupTimeTo());
        }

        if (dto.estimatedTime() != null) {
            order.setEstimatedTime(dto.estimatedTime());
        }

        if (dto.transportNotes() != null) {
            order.setTransportNotes(dto.transportNotes());
        }

        // Transport to zawsze TYLKO transport - brak logiki kombinowanej
        order.setTransportType(TransportOrder.TransportType.TO_SERVICE_ONLY);
    }

    // === GUEST USER AND BIKE CREATION ===

    public IncompleteUser createOrFindIncompleteUser(String email, String phone) {
        Optional<IncompleteUser> existingUser = incompleteUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            IncompleteUser user = existingUser.get();
            if (phone != null) {
                user.setPhoneNumber(phone);
            }
            return incompleteUserRepository.save(user);
        } else {
            IncompleteUser newUser = new IncompleteUser();
            newUser.setEmail(email);
            newUser.setPhoneNumber(phone);
            newUser.setCreatedAt(LocalDateTime.now());
            return incompleteUserRepository.save(newUser);
        }
    }

    public List<IncompleteBike> createIncompleteBikes(List<GuestBicycleDto> bicycleDtos, IncompleteUser owner) {
        List<IncompleteBike> bikes = new ArrayList<>();

        for (GuestBicycleDto bikeDto : bicycleDtos) {
            IncompleteBike bike = new IncompleteBike();
            bike.setBrand(bikeDto.brand());
            bike.setModel(bikeDto.model());
            if (bikeDto.additionalInfo() != null && !bikeDto.additionalInfo().isEmpty()) {
                bike.setType(bikeDto.additionalInfo());
            }
            bike.setOwner(owner);
            bike.setCreatedAt(LocalDateTime.now());

            bikes.add(incompleteBikeRepository.save(bike));
        }

        return bikes;
    }

    // === UTILITY METHODS ===

    public boolean isTransportStatus(String status) {
        try {
            TransportOrder.TransportStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // === VALIDATION METHODS ===

    public boolean validateBicycleOwnership(Long bicycleId, Long userId) {
        Optional<IncompleteBike> bikeOpt = incompleteBikeRepository.findById(bicycleId);
        if (bikeOpt.isEmpty()) {
            return false;
        }

        IncompleteBike bike = bikeOpt.get();
        return bike.getOwner() != null && bike.getOwner().getId().equals(userId);
    }

    public boolean validateServicePackage(Long packageId) {
        if (packageId == null) {
            return false;
        }
        return servicePackageRepository.existsById(packageId);
    }

    public boolean validateServicePackageCode(String packageCode) {
        if (packageCode == null || packageCode.trim().isEmpty()) {
            return false;
        }
        return servicePackageRepository.findByCode(packageCode).isPresent();
    }

    // === STATUS VALIDATION ===

    public boolean isValidStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus, boolean isAdmin) {
        if (currentStatus == newStatus) {
            return true; // Brak zmiany
        }

        if (!isAdmin) {
            // Klient może tylko anulować
            return newStatus == Order.OrderStatus.CANCELLED &&
                    (currentStatus == Order.OrderStatus.PENDING || currentStatus == Order.OrderStatus.CONFIRMED);
        }

        // Admin może wszystko, ale sprawdźmy logiczne przejścia
        if (currentStatus == Order.OrderStatus.CANCELLED) {
            return false; // Nie można zmienić z anulowanego (chyba że admin reaktywuje)
        }

        if (newStatus == Order.OrderStatus.CANCELLED) {
            return true; // Można anulować z dowolnego statusu
        }

        // Sprawdź normalny przepływ
        return switch (currentStatus) {
            case PENDING -> newStatus == Order.OrderStatus.CONFIRMED;
            case CONFIRMED -> newStatus == Order.OrderStatus.PICKED_UP;
            case PICKED_UP -> newStatus == Order.OrderStatus.IN_SERVICE;
            case IN_SERVICE -> newStatus == Order.OrderStatus.COMPLETED;
            case COMPLETED -> newStatus == Order.OrderStatus.DELIVERED;
            case DELIVERED -> false; // Koniec przepływu
            default -> false;
        };
    }

    public boolean isValidTransportStatusTransition(TransportOrder.TransportStatus currentStatus,
                                                    TransportOrder.TransportStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }

        return switch (currentStatus) {
            case PENDING -> newStatus == TransportOrder.TransportStatus.PICKED_UP;
            case PICKED_UP -> newStatus == TransportOrder.TransportStatus.IN_TRANSIT;
            case IN_TRANSIT -> newStatus == TransportOrder.TransportStatus.DELIVERED_TO_SERVICE;
            case DELIVERED_TO_SERVICE -> newStatus == TransportOrder.TransportStatus.PICKED_UP_FROM_SERVICE;
            case PICKED_UP_FROM_SERVICE -> newStatus == TransportOrder.TransportStatus.COMPLETED;
            case COMPLETED -> false; // Koniec przepływu
            default -> false;
        };
    }

    // === ERROR MESSAGES ===

    public String getInvalidStatusTransitionMessage(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        return String.format("Nieprawidłowa zmiana statusu z %s na %s", currentStatus, newStatus);
    }

    public String getInvalidTransportStatusTransitionMessage(TransportOrder.TransportStatus currentStatus,
                                                             TransportOrder.TransportStatus newStatus) {
        return String.format("Nieprawidłowa zmiana statusu transportu z %s na %s", currentStatus, newStatus);
    }

    public String getUnauthorizedAccessMessage() {
        return "Brak uprawnień do wykonania tej operacji";
    }

    public String getOrderNotModifiableMessage() {
        return "Zamówienie można modyfikować tylko w statusie PENDING lub CONFIRMED";
    }

    public String getBicycleNotFoundMessage(Long bicycleId) {
        return String.format("Rower o ID %d nie został znaleziony", bicycleId);
    }

    public String getBicycleOwnershipMessage() {
        return "Brak uprawnień do wybranego roweru";
    }

    public String getServicePackageNotFoundMessage() {
        return "Pakiet serwisowy nie został znaleziony";
    }
}