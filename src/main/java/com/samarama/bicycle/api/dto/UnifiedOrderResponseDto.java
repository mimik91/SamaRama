package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.TransportOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Unified DTO dla odpowiedzi z zamówień (zarówno transport jak i serwis)
 */
public record UnifiedOrderResponseDto(
        Long id,
        String orderType, // "TRANSPORT" lub "SERVICE"

        // === BASIC INFO ===
        Long bicycleId,
        String bicycleBrand,
        String bicycleModel,
        String clientEmail,
        String clientPhone,
        String clientName,

        // === TRANSPORT INFO ===
        LocalDate pickupDate,
        String pickupAddress,
        String deliveryAddress,
        String targetServiceName,
        BigDecimal transportPrice,

        // === SERVICE INFO (tylko dla ServiceOrder) ===
        String servicePackageCode,
        String servicePackageName,
        BigDecimal servicePrice,
        BigDecimal totalPrice,
        LocalDateTime serviceStartDate,
        LocalDateTime serviceCompletionDate,
        String serviceNotes,

        // === STATUS AND DATES ===
        String status,
        String statusDisplayName,
        LocalDateTime orderDate,
        String additionalNotes,
        String transportNotes,

        // === METADATA ===
        String lastModifiedBy,
        LocalDateTime lastModifiedDate
) {
    /**
     * Tworzy DTO z TransportOrder
     */
    public static UnifiedOrderResponseDto fromTransportOrder(TransportOrder entity) {
        String clientName = getClientName(entity.getClient());

        return new UnifiedOrderResponseDto(
                entity.getId(),
                entity.getOrderType(),
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getClient() != null ? entity.getClient().getEmail() : null,
                entity.getClient() != null ? entity.getClient().getPhoneNumber() : null,
                clientName,
                entity.getPickupDate(),
                entity.getFullPickupAddress(), // Używamy nowej metody z rozbitego adresu
                entity.getFullDeliveryAddress(), // Używamy nowej metody z rozbitego adresu
                entity.getTargetService() != null ? entity.getTargetService().getName() : null,
                entity.getTransportPrice(),
                // Service fields - null dla czystego transportu
                null, null, null, entity.getTotalPrice(), null, null, null,
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getStatusDisplayName(),
                entity.getOrderDate(),
                entity.getAdditionalNotes(),
                entity.getTransportNotes(),
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Tworzy DTO z ServiceOrder
     */
    public static UnifiedOrderResponseDto fromServiceOrder(ServiceOrder entity) {
        String clientName = getClientName(entity.getClient());

        return new UnifiedOrderResponseDto(
                entity.getId(),
                entity.getOrderType(),
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getClient() != null ? entity.getClient().getEmail() : null,
                entity.getClient() != null ? entity.getClient().getPhoneNumber() : null,
                clientName,
                entity.getPickupDate(),
                entity.getFullPickupAddress(), // Używamy nowej metody z rozbitego adresu
                entity.getFullDeliveryAddress(), // Używamy nowej metody z rozbitego adresu
                entity.getTargetService() != null ? entity.getTargetService().getName() : "SERWIS WŁASNY",
                entity.getTransportPrice(),
                // Service fields
                entity.getServicePackageCode(),
                entity.getServicePackageName(),
                entity.getServicePrice(),
                entity.getTotalPrice(),
                entity.getServiceStartDate(),
                entity.getServiceCompletionDate(),
                entity.getServiceNotes(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getStatusDisplayName(),
                entity.getOrderDate(),
                entity.getAdditionalNotes(),
                entity.getTransportNotes(),
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Helper do pobierania nazwy klienta
     */
    private static String getClientName(com.samarama.bicycle.api.model.IncompleteUser client) {
        if (client instanceof com.samarama.bicycle.api.model.User) {
            com.samarama.bicycle.api.model.User user = (com.samarama.bicycle.api.model.User) client;
            if (user.getFirstName() != null || user.getLastName() != null) {
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                return (firstName + " " + lastName).trim();
            }
        }
        return null;
    }

    /**
     * Sprawdza czy zamówienie można modyfikować/usuwać
     */
    public boolean isEditable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    /**
     * Sprawdza czy zamówienie można anulować
     */
    public boolean isCancellable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    /**
     * Sprawdza czy to zamówienie serwisowe
     */
    public boolean isServiceOrder() {
        return "SERVICE".equals(orderType);
    }

    /**
     * Sprawdza czy to czysto transportowe
     */
    public boolean isTransportOnly() {
        return "TRANSPORT".equals(orderType);
    }

    /**
     * Zwraca informację o kliencie
     */
    public String getClientDisplayInfo() {
        StringBuilder info = new StringBuilder();

        if (clientName != null && !clientName.trim().isEmpty()) {
            info.append(clientName);
        }

        if (clientEmail != null) {
            if (info.length() > 0) {
                info.append(" (").append(clientEmail).append(")");
            } else {
                info.append(clientEmail);
            }
        }

        return info.toString();
    }

    /**
     * Zwraca opis roweru
     */
    public String getBicycleDescription() {
        if (bicycleBrand != null || bicycleModel != null) {
            String brand = bicycleBrand != null ? bicycleBrand : "";
            String model = bicycleModel != null ? bicycleModel : "";
            return (brand + " " + model).trim();
        }
        return "Rower ID: " + bicycleId;
    }
}