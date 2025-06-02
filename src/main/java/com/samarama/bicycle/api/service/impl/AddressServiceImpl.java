package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.AddressDto;
import com.samarama.bicycle.api.model.Address;
import com.samarama.bicycle.api.repository.AddressRepository;
import com.samarama.bicycle.api.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private static final Logger logger = Logger.getLogger(AddressServiceImpl.class.getName());

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(Long userId) {
        List<Address> addresses = addressRepository.findByUserIdAndActiveTrue(userId);
        return addresses.stream()
                .map(AddressDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<AddressDto> getAddressById(Long addressId, Long userId) {
        Optional<Address> addressOpt = addressRepository.findByIdAndUserId(addressId, userId);

        return addressOpt
                .map(address -> ResponseEntity.ok(AddressDto.fromEntity(address)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<?> createAddress(AddressDto addressDto, Long userId) {
        try {
            // Walidacja podstawowa
            ValidationResult validation = validateAddressData(addressDto);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", validation.getError()));
            }

            // Utwórz nowy adres
            Address address = addressDto.toEntity();
            address.setUserId(userId);


            Address savedAddress = addressRepository.save(address);

            logger.info("Created new address for user " + userId + ": " + savedAddress.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Adres został utworzony pomyślnie",
                    "address", AddressDto.fromEntity(savedAddress)
            ));

        } catch (Exception e) {
            logger.severe("Error creating address for user " + userId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas tworzenia adresu"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateAddress(Long addressId, AddressDto addressDto, Long userId) {
        try {
            Optional<Address> addressOpt = addressRepository.findByIdAndUserId(addressId, userId);
            if (addressOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Walidacja danych
            ValidationResult validation = validateAddressData(addressDto);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(Map.of("message", validation.getError()));
            }

            Address address = addressOpt.get();

            // Aktualizuj pola
            updateAddressFields(address, addressDto);

            Address savedAddress = addressRepository.save(address);

            return ResponseEntity.ok(Map.of(
                    "message", "Adres został zaktualizowany",
                    "address", AddressDto.fromEntity(savedAddress)
            ));

        } catch (Exception e) {
            logger.severe("Error updating address " + addressId + " for user " + userId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas aktualizacji adresu"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteAddress(Long addressId, Long userId) {
        try {
            Optional<Address> addressOpt = addressRepository.findByIdAndUserId(addressId, userId);
            if (addressOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }


            logger.info("Deleted address " + addressId + " for user " + userId);

            return ResponseEntity.ok(Map.of("message", "Adres został usunięty"));

        } catch (Exception e) {
            logger.severe("Error deleting address " + addressId + " for user " + userId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Błąd podczas usuwania adresu"));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<AddressDto> getOrCreateAddress(Long addressId, AddressDto newAddressData, Long userId) {
        // Jeśli podano addressId, spróbuj pobrać istniejący adres
        if (addressId != null) {
            Optional<Address> addressOpt = addressRepository.findByIdAndUserId(addressId, userId);
            if (addressOpt.isPresent()) {
                return ResponseEntity.ok(AddressDto.fromEntity(addressOpt.get()));
            }
        }

        // Jeśli nie ma addressId lub nie znaleziono adresu, utwórz nowy
        if (newAddressData != null) {
            ResponseEntity<?> createResponse = createAddress(newAddressData, userId);

            if (createResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) createResponse.getBody();
                if (responseBody != null && responseBody.containsKey("address")) {
                    AddressDto createdAddress = (AddressDto) responseBody.get("address");
                    return ResponseEntity.ok(createdAddress);
                }
            }
        }

        return ResponseEntity.badRequest().build();
    }

    // === METODY POMOCNICZE ===

    /**
     * Waliduje dane adresowe
     */
    private ValidationResult validateAddressData(AddressDto addressDto) {
        if (addressDto.street() == null || addressDto.street().trim().isEmpty()) {
            return ValidationResult.invalid("Ulica jest wymagana");
        }

        if (addressDto.buildingNumber() == null || addressDto.buildingNumber().trim().isEmpty()) {
            return ValidationResult.invalid("Numer budynku jest wymagany");
        }

        if (addressDto.city() == null || addressDto.city().trim().isEmpty()) {
            return ValidationResult.invalid("Miasto jest wymagane");
        }

        // Walidacja długości pól
        if (addressDto.street().length() > 255) {
            return ValidationResult.invalid("Nazwa ulicy jest zbyt długa");
        }

        if (addressDto.buildingNumber().length() > 20) {
            return ValidationResult.invalid("Numer budynku jest zbyt długi");
        }

        if (addressDto.apartmentNumber() != null && addressDto.apartmentNumber().length() > 20) {
            return ValidationResult.invalid("Numer mieszkania jest zbyt długi");
        }

        if (addressDto.city().length() > 100) {
            return ValidationResult.invalid("Nazwa miasta jest zbyt długa");
        }

        if (addressDto.postalCode() != null && addressDto.postalCode().length() > 10) {
            return ValidationResult.invalid("Kod pocztowy jest zbyt długi");
        }

        if (addressDto.name() != null && addressDto.name().length() > 100) {
            return ValidationResult.invalid("Nazwa adresu jest zbyt długa");
        }

        if (addressDto.transportNotes() != null && addressDto.transportNotes().length() > 500) {
            return ValidationResult.invalid("Notatki transportowe są zbyt długie");
        }

        return ValidationResult.valid();
    }

    /**
     * Aktualizuje pola adresu
     */
    private void updateAddressFields(Address address, AddressDto addressDto) {
        if (addressDto.street() != null) {
            address.setStreet(addressDto.street());
        }
        if (addressDto.buildingNumber() != null) {
            address.setBuildingNumber(addressDto.buildingNumber());
        }
        if (addressDto.apartmentNumber() != null) {
            address.setApartmentNumber(addressDto.apartmentNumber());
        }
        if (addressDto.city() != null) {
            address.setCity(addressDto.city());
        }
        if (addressDto.postalCode() != null) {
            address.setPostalCode(addressDto.postalCode());
        }
        if (addressDto.name() != null) {
            address.setName(addressDto.name());
        }
        if (addressDto.latitude() != null) {
            address.setLatitude(addressDto.latitude());
        }
        if (addressDto.longitude() != null) {
            address.setLongitude(addressDto.longitude());
        }
        if (addressDto.transportNotes() != null) {
            address.setTransportNotes(addressDto.transportNotes());
        }
    }


    /**
     * Klasa pomocnicza dla wyników walidacji
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String error;

        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        public boolean isValid() {
            return valid;
        }

        public String getError() {
            return error;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
    }
}