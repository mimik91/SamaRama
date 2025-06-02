package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.GuestBicycleDto;
import com.samarama.bicycle.api.dto.ServiceOrTransportOrderDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.EmailService;
import com.samarama.bicycle.api.service.GuestOrderService;
import com.samarama.bicycle.api.service.helper.GuestOrderValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuestOrderServiceImpl implements GuestOrderService {

    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final TransportOrderRepository transportOrderRepository; // Dodane
    private final BikeServiceRepository bikeServiceRepository; // Dodane
    private final EmailService emailService;
    private final GuestOrderValidator validator;

    public GuestOrderServiceImpl(
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            ServicePackageRepository servicePackageRepository,
            ServiceOrderRepository serviceOrderRepository,
            TransportOrderRepository transportOrderRepository,
            BikeServiceRepository bikeServiceRepository,
            EmailService emailService,
            GuestOrderValidator validator) {
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.transportOrderRepository = transportOrderRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.emailService = emailService;
        this.validator = validator;
    }

    @Override
    @Transactional
    public ResponseEntity<?> processGuestOrder(ServiceOrTransportOrderDto orderDto) {
        try {
            // Walidacja
            List<String> errors = validator.validateGuestOrder(orderDto);
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", String.join("; ", errors),
                        "errors", errors
                ));
            }

            // Pobierz pakiet serwisowy
            ServicePackage servicePackage = servicePackageRepository.findById(orderDto.servicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found"));

            // Pobierz serwis własny (ID = 1)
            BikeService ownService = bikeServiceRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Own service not found"));

            // 1. Tworzenie użytkownika tymczasowego
            IncompleteUser incompleteUser = createOrFindIncompleteUser(orderDto);

            // 2. Tworzenie rowerów
            List<IncompleteBike> bikes = createIncompleteBikes(orderDto.bicycles(), incompleteUser);

            // 3. Tworzenie zamówień serwisowych (NOWA STRUKTURA)
            List<ServiceOrder> orders = createServiceOrdersNewStructure(
                    bikes, servicePackage, orderDto, incompleteUser, ownService);

            // 4. Zapisywanie zamówień
            List<ServiceOrder> savedOrders = serviceOrderRepository.saveAll(orders);

            // 5. Wysłanie powiadomień email
            for (ServiceOrder savedOrder : savedOrders) {
                try {
                    emailService.sendOrderNotificationEmail(savedOrder);
                } catch (Exception e) {
                    System.err.println("Failed to send email notification for guest order ID: " +
                            savedOrder.getId() + ", error: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Zamówienia serwisowe zostały utworzone pomyślnie",
                    "orderIds", savedOrders.stream().map(ServiceOrder::getId).collect(Collectors.toList()),
                    "userId", incompleteUser.getId(),
                    "orderCount", savedOrders.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Wystąpił błąd podczas przetwarzania zamówienia: " + e.getMessage()
            ));
        }
    }

    private IncompleteUser createOrFindIncompleteUser(GuestServiceOrderDto orderDto) {
        Optional<IncompleteUser> existingUser = incompleteUserRepository.findByEmail(orderDto.email());

        if (existingUser.isPresent()) {
            IncompleteUser user = existingUser.get();
            user.setPhoneNumber(orderDto.phone());
            return incompleteUserRepository.save(user);
        } else {
            IncompleteUser newUser = new IncompleteUser();
            newUser.setEmail(orderDto.email());
            newUser.setPhoneNumber(orderDto.phone());
            newUser.setCreatedAt(LocalDateTime.now());
            return incompleteUserRepository.save(newUser);
        }
    }

    private List<IncompleteBike> createIncompleteBikes(List<GuestBicycleDto> bicycleDtos, IncompleteUser owner) {
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

    /**
     * Tworzy zamówienia serwisowe w nowej strukturze (ServiceOrder dziedziczy po TransportOrder)
     */
    private List<ServiceOrder> createServiceOrdersNewStructure(
            List<IncompleteBike> bikes,
            ServicePackage servicePackage,
            GuestServiceOrderDto orderDto,
            IncompleteUser user,
            BikeService ownService) {

        List<ServiceOrder> orders = new ArrayList<>();

        // Kalkulacja ceny transportu (przykładowe wartości)
        BigDecimal transportPrice = calculateTransportPrice(bikes.size());

        for (IncompleteBike bike : bikes) {
            // KROK 1: Tworzymy bazowy TransportOrder
            TransportOrder transportOrder = new TransportOrder();
            transportOrder.setBicycle(bike);
            transportOrder.setClient(user);
            transportOrder.setPickupDate(orderDto.pickupDate());
            transportOrder.setPickupAddress(orderDto.address() + ", " + orderDto.city());
            transportOrder.setPickupLatitude(null); // Guest orders nie mają koordinat
            transportOrder.setPickupLongitude(null);
            transportOrder.setPickupTimeFrom(null);
            transportOrder.setPickupTimeTo(null);

            // Dla zamówień serwisowych target_service to zawsze serwis własny
            transportOrder.setTargetService(ownService);
            transportOrder.setDeliveryAddress("SERWIS WŁASNY");
            transportOrder.setDeliveryLatitude(ownService.getLatitude());
            transportOrder.setDeliveryLongitude(ownService.getLongitude());

            transportOrder.setTransportPrice(transportPrice);
            transportOrder.setEstimatedTime(60); // 60 minut domyślnie
            transportOrder.setTransportNotes(null);
            transportOrder.setAdditionalNotes(orderDto.notes());
            transportOrder.setStatus(TransportOrder.OrderStatus.PENDING);
            transportOrder.setOrderDate(LocalDateTime.now());

            // KROK 2: Zapisujemy TransportOrder żeby uzyskać ID
            TransportOrder savedTransportOrder = transportOrderRepository.save(transportOrder);

            // KROK 3: Tworzymy ServiceOrder z tym samym ID
            ServiceOrder serviceOrder = new ServiceOrder();
            serviceOrder.setId(savedTransportOrder.getId()); // Używamy tego samego ID!

            // Kopiujemy wszystkie pola z TransportOrder (ServiceOrder dziedziczy)
            copyTransportOrderFields(serviceOrder, savedTransportOrder);

            // Dodajemy pola specyficzne dla ServiceOrder
            serviceOrder.setServicePackage(servicePackage);
            serviceOrder.setServicePackageCode(servicePackage.getCode());
            serviceOrder.setServicePrice(servicePackage.getPrice());
            serviceOrder.setServiceNotes(null);
            serviceOrder.setServiceStartDate(null);
            serviceOrder.setServiceCompletionDate(null);

            orders.add(serviceOrder);
        }

        return orders;
    }

    /**
     * Kopiuje pola z TransportOrder do ServiceOrder
     */
    private void copyTransportOrderFields(ServiceOrder serviceOrder, TransportOrder transportOrder) {
        serviceOrder.setBicycle(transportOrder.getBicycle());
        serviceOrder.setClient(transportOrder.getClient());
        serviceOrder.setPickupDate(transportOrder.getPickupDate());
        serviceOrder.setPickupAddress(transportOrder.getPickupAddress());
        serviceOrder.setPickupLatitude(transportOrder.getPickupLatitude());
        serviceOrder.setPickupLongitude(transportOrder.getPickupLongitude());
        serviceOrder.setPickupTimeFrom(transportOrder.getPickupTimeFrom());
        serviceOrder.setPickupTimeTo(transportOrder.getPickupTimeTo());
        serviceOrder.setDeliveryAddress(transportOrder.getDeliveryAddress());
        serviceOrder.setDeliveryLatitude(transportOrder.getDeliveryLatitude());
        serviceOrder.setDeliveryLongitude(transportOrder.getDeliveryLongitude());
        serviceOrder.setTargetService(transportOrder.getTargetService());
        serviceOrder.setStatus(transportOrder.getStatus());
        serviceOrder.setOrderDate(transportOrder.getOrderDate());
        serviceOrder.setTransportPrice(transportOrder.getTransportPrice());
        serviceOrder.setEstimatedTime(transportOrder.getEstimatedTime());
        serviceOrder.setActualPickupTime(transportOrder.getActualPickupTime());
        serviceOrder.setActualDeliveryTime(transportOrder.getActualDeliveryTime());
        serviceOrder.setTransportNotes(transportOrder.getTransportNotes());
        serviceOrder.setAdditionalNotes(transportOrder.getAdditionalNotes());
        serviceOrder.setLastModifiedBy(transportOrder.getLastModifiedBy());
        serviceOrder.setLastModifiedDate(transportOrder.getLastModifiedDate());
    }

    /**
     * Oblicza cenę transportu dla zamówienia gościa
     */
    private BigDecimal calculateTransportPrice(int bikesCount) {
        BigDecimal baseCost = new BigDecimal("30.00");
        BigDecimal perBikeCost = new BigDecimal("15.00");

        BigDecimal totalCost = baseCost;
        if (bikesCount > 1) {
            totalCost = totalCost.add(perBikeCost.multiply(new BigDecimal(bikesCount - 1)));
        }

        // 10% rabat dla serwisu własnego
        return totalCost.multiply(new BigDecimal("0.9"));
    }
}