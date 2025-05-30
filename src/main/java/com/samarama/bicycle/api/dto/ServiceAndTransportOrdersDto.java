package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.TransportOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO dla unified view zamówień serwisowych i transportowych
 * Umożliwia wyświetlanie obu typów zamówień w jednej tabeli/liście
 */
public record ServiceAndTransportOrdersDto(
        Long id,
        String orderType, // "SERVICE" lub "TRANSPORT"
        Long bicycleId,
        String bicycleBrand,
        String bicycleModel,
        String clientEmail,
        String clientPhone,
        String clientName, // firstName + lastName dla User, null dla IncompleteUser
        LocalDate pickupDate,
        String pickupAddress,
        String deliveryAddress, // "SERWIS" dla zamówień serwisowych, rzeczywisty adres dla transportu
        BigDecimal price,
        LocalDateTime orderDate,
        String additionalNotes,
        String status,
        String serviceNotes,
        String servicePackageCode,
        String servicePackageName,
        String lastModifiedBy,
        LocalDateTime lastModifiedDate
) {
    /**
     * Tworzy DTO z zamówienia serwisowego
     */
    public static ServiceAndTransportOrdersDto fromServiceOrder(ServiceOrder entity) {
        String clientName = null;
        if (entity.getClient() instanceof com.samarama.bicycle.api.model.User) {
            com.samarama.bicycle.api.model.User user = (com.samarama.bicycle.api.model.User) entity.getClient();
            if (user.getFirstName() != null || user.getLastName() != null) {
                clientName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                        (user.getLastName() != null ? user.getLastName() : "");
                clientName = clientName.trim();
            }
        }

        return new ServiceAndTransportOrdersDto(
                entity.getId(),
                "SERVICE",
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getClient() != null ? entity.getClient().getEmail() : null,
                entity.getClient() != null ? entity.getClient().getPhoneNumber() : null,
                clientName,
                entity.getPickupDate(),
                entity.getPickupAddress(),
                "SERWIS", // Dla zamówień serwisowych zawsze "SERWIS"
                entity.getPrice(),
                entity.getOrderDate(),
                entity.getAdditionalNotes(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getServiceNotes(),
                entity.getServicePackageCode(),
                entity.getServicePackage() != null ? entity.getServicePackage().getName() : null,
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Tworzy DTO z zamówienia transportowego
     */
    public static ServiceAndTransportOrdersDto fromTransportOrder(TransportOrder entity) {
        String clientName = null;
        if (entity.getClient() instanceof com.samarama.bicycle.api.model.User) {
            com.samarama.bicycle.api.model.User user = (com.samarama.bicycle.api.model.User) entity.getClient();
            if (user.getFirstName() != null || user.getLastName() != null) {
                clientName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                        (user.getLastName() != null ? user.getLastName() : "");
                clientName = clientName.trim();
            }
        }

        return new ServiceAndTransportOrdersDto(
                entity.getId(),
                "TRANSPORT",
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getClient() != null ? entity.getClient().getEmail() : null,
                entity.getClient() != null ? entity.getClient().getPhoneNumber() : null,
                clientName,
                entity.getPickupDate(),
                entity.getPickupAddress(),
                entity.getDeliveryAddress(),
                entity.getPrice(),
                entity.getOrderDate(),
                entity.getAdditionalNotes(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getServiceNotes(),
                null, // Transport nie ma pakietu serwisowego
                null, // Transport nie ma pakietu serwisowego
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Sprawdza czy zamówienie można modyfikować/usuwać (tylko PENDING i CONFIRMED)
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
     * Zwraca czytelną nazwę typu zamówienia
     */
    public String getOrderTypeDisplayName() {
        return switch (orderType) {
            case "SERVICE" -> "Serwis";
            case "TRANSPORT" -> "Transport";
            default -> orderType;
        };
    }

    /**
     * Zwraca czytelną nazwę statusu
     */
    public String getStatusDisplayName() {
        if (status == null) return "Nieznany";

        return switch (status) {
            case "PENDING" -> "Oczekujące";
            case "CONFIRMED" -> "Potwierdzone";
            case "PICKED_UP" -> "Odebrane";
            case "IN_SERVICE" -> "W serwisie";
            case "IN_TRANSPORT" -> "W transporcie";
            case "COMPLETED" -> "Zakończone";
            case "DELIVERED" -> "Dostarczone";
            case "CANCELLED" -> "Anulowane";
            default -> status;
        };
    }

    /**
     * Zwraca informację o kliencie (imię + email lub sam email)
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
}