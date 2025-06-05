package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.GuestBicycleDto;
import com.samarama.bicycle.api.dto.ServiceOrTransportOrderDto;
import com.samarama.bicycle.api.model.*;
import com.samarama.bicycle.api.repository.*;
import com.samarama.bicycle.api.service.AddressService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Pomocniczy serwis do obsługi zamówień - konwertuje nowe DTO na odpowiednie encje
 */
@Component
public class OrderHelperService {

    private static final Logger logger = Logger.getLogger(OrderHelperService.class.getName());

    private final UserRepository userRepository;
    private final IncompleteUserRepository incompleteUserRepository;
    private final IncompleteBikeRepository incompleteBikeRepository;
    private final BikeServiceRepository bikeServiceRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final AddressRepository addressRepository;

    @Value("${app.internal.service.id}")
    private String internalServiceIdString;

    public OrderHelperService(
            UserRepository userRepository,
            IncompleteUserRepository incompleteUserRepository,
            IncompleteBikeRepository incompleteBikeRepository,
            BikeServiceRepository bikeServiceRepository,
            ServicePackageRepository servicePackageRepository,
            AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.incompleteUserRepository = incompleteUserRepository;
        this.incompleteBikeRepository = incompleteBikeRepository;
        this.bikeServiceRepository = bikeServiceRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.addressRepository = addressRepository;

    }

    // === USER OPERATIONS ===

    /**
     * Pobierz użytkownika po email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Pobierz użytkownika po ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    /**
     * Utwórz lub znajdź IncompleteUser dla gościa
     */
    public IncompleteUser createOrFindIncompleteUser(String email, String phone) {
        Optional<IncompleteUser> existingUser = incompleteUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            IncompleteUser user = existingUser.get();
            if (phone != null && !phone.equals(user.getPhoneNumber())) {
                user.setPhoneNumber(phone);
                return incompleteUserRepository.save(user);
            }
            return user;
        } else {
            IncompleteUser newUser = new IncompleteUser();
            newUser.setEmail(email);
            newUser.setPhoneNumber(phone);
            newUser.setCreatedAt(LocalDateTime.now());
            return incompleteUserRepository.save(newUser);
        }
    }

    // === ADDRESS OPERATIONS ===

    /**
     * Pobierz lub utwórz adres z DTO
     */
    public Address getOrCreateAddress(ServiceOrTransportOrderDto dto, Long userId) {
        // Jeśli podano ID adresu, spróbuj go pobrać
        if (dto.getPickupAddressId() != null) {
            Optional<Address> existingAddress = addressRepository.findByIdAndUserId(dto.getPickupAddressId(), userId);
            if (existingAddress.isPresent()) {
                return existingAddress.get();
            } else {
                throw new RuntimeException("Adres o ID " + dto.getPickupAddressId() + " nie należy do użytkownika lub nie istnieje");
            }
        }

        // Jeśli nie ma adresu w systemie, utwórz z danych z DTO
        if (dto.usesNewAddress()) {
            return createAddressFromDto(dto, userId);
        }

        throw new RuntimeException("Brak danych adresowych");
    }

    /**
     * Utwórz adres z danych DTO (nie zapisuje do bazy - tylko dla zamówień)
     */
    public Address createTemporaryAddressFromDto(ServiceOrTransportOrderDto dto, Long userId) {
        Address address = new Address();
        address.setStreet(dto.getPickupStreet());
        address.setBuildingNumber(dto.getPickupBuildingNumber());
        address.setApartmentNumber(dto.getPickupApartmentNumber());
        address.setCity(dto.getPickupCity());
        address.setPostalCode(dto.getPickupPostalCode());
        address.setLatitude(dto.getPickupLatitude());
        address.setLongitude(dto.getPickupLongitude());
        address.setUserId(userId);
        address.setName("Adres z zamówienia"); // domyślna nazwa
        address.setActive(true);
        return address; // Nie zapisuje do bazy
    }

    /**
     * Utwórz i zapisz adres z danych DTO
     */
    public Address createAddressFromDto(ServiceOrTransportOrderDto dto, Long userId) {
        Address address = createTemporaryAddressFromDto(dto, userId);
        return addressRepository.save(address);
    }

    /**
     * Utwórz tymczasowy adres dla gościa (nie zapisuje do bazy)
     */
    public String createGuestAddressString(ServiceOrTransportOrderDto dto) {
        if (!dto.usesNewAddress()) {
            throw new RuntimeException("Brak danych adresowych dla gościa");
        }

        StringBuilder address = new StringBuilder();
        address.append(dto.getPickupStreet()).append(" ").append(dto.getPickupBuildingNumber());

        if (dto.getPickupApartmentNumber() != null && !dto.getPickupApartmentNumber().trim().isEmpty()) {
            address.append("/").append(dto.getPickupApartmentNumber());
        }

        address.append(", ").append(dto.getPickupCity());

        if (dto.getPickupPostalCode() != null && !dto.getPickupPostalCode().trim().isEmpty()) {
            address.append(" ").append(dto.getPickupPostalCode());
        }

        return address.toString();
    }

    // === BICYCLE OPERATIONS ===

    /**
     * Waliduj i pobierz rowery dla zalogowanego użytkownika
     */
    public List<IncompleteBike> validateAndGetBikes(List<Long> bicycleIds, Long userId) {
        List<IncompleteBike> bikes = new ArrayList<>();

        for (Long bicycleId : bicycleIds) {
            IncompleteBike bike = incompleteBikeRepository.findById(bicycleId)
                    .orElseThrow(() -> new RuntimeException("Bike not found: " + bicycleId));

            if (bike.getOwner() == null || !bike.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Brak uprawnień do roweru: " + bicycleId);
            }

            bikes.add(bike);
        }

        return bikes;
    }

    /**
     * Utwórz rowery dla gościa
     */
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

    // === SERVICE OPERATIONS ===

    /**
     * Pobierz pakiet serwisowy i jego cenę
     */
    public ServicePackage getServicePackage(ServiceOrTransportOrderDto dto) {
        if (dto.getServicePackageId() != null) {
            return servicePackageRepository.findById(dto.getServicePackageId())
                    .orElseThrow(() -> new RuntimeException("Service package not found: " + dto.getServicePackageId()));
        } else {
            throw new RuntimeException("Brak ID pakietu serwisowego");
        }
    }

    /**
     * Pobierz serwis własny (ID = 1)
     */
    public BikeService getOwnService() {
        return bikeServiceRepository.findById(Long.parseLong(internalServiceIdString))
                .orElseThrow(() -> new RuntimeException("Own service not found"));
    }

    /**
     * Pobierz serwis docelowy
     */
    public BikeService getTargetService(Long targetServiceId) {
        return bikeServiceRepository.findById(targetServiceId)
                .orElseThrow(() -> new RuntimeException("Target service not found: " + targetServiceId));
    }


    // === VALIDATION ===

    /**
     * Waliduj DTO dla zalogowanego użytkownika
     */
    public void validateUserOrder(ServiceOrTransportOrderDto dto) {
        if (!dto.isValidForLoggedUser()) {
            throw new RuntimeException("Nieprawidłowe dane zamówienia dla użytkownika");
        }

        if (dto.isServiceOrder() && dto.getTargetServiceId() != null && !dto.getTargetServiceId().equals(1L)) {
            throw new RuntimeException("Zamówienie serwisowe musi być kierowane do serwisu własnego");
        }

        if (dto.isTransportOnlyOrder() && dto.getTargetServiceId() != null && dto.getTargetServiceId().equals(1L)) {
            throw new RuntimeException("Zamówienie transportowe nie może być kierowane do serwisu własnego");
        }
    }

    /**
     * Waliduj DTO dla gościa
     */
    public void validateGuestOrder(ServiceOrTransportOrderDto dto) {
        if (!dto.isValidForGuest()) {
            throw new RuntimeException("Nieprawidłowe dane zamówienia dla gościa");
        }

        // Goście mogą składać tylko zamówienia serwisowe
        if (!dto.isServiceOrder()) {
            throw new RuntimeException("Goście mogą składać tylko zamówienia serwisowe");
        }
    }

    // === LOGGING ===

    /**
     * Loguj utworzenie zamówienia
     */
    public void logOrderCreation(Long orderId, String orderType, String userEmail, BigDecimal totalPrice) {
        logger.info(String.format(
                "Order created: ID=%d, Type=%s, User=%s, Price=%s",
                orderId, orderType, userEmail, totalPrice
        ));
    }

    /**
     * Loguj błąd tworzenia zamówienia
     */
    public void logOrderError(String operation, String userEmail, String error) {
        logger.severe(String.format(
                "Order error: Operation=%s, User=%s, Error=%s",
                operation, userEmail, error
        ));
    }
}