package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.TransportOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO dla szczegółów zamówienia serwisowego/transportowego
 * Zawiera wszystkie informacje potrzebne do wyświetlenia pełnych szczegółów zamówienia
 */
public record ServiceOrderDetailsResponseDto(
        Long id,
        String orderType, // "TRANSPORT" lub "SERVICE"
        Long bicycleId,
        LocalDate pickupDate,
        String pickupAddress,
        String targetServiceName,
        String deliveryAddress,
        String serviceNotes,
        String additionalNotes,
        String transportNotes,
        BigDecimal totalPrice,
        String status,
        String statusDisplayName,
        LocalDateTime orderDate,
        String bicycleDescription
) {
    /**
     * Tworzy DTO z ServiceOrder
     */
    public static ServiceOrderDetailsResponseDto fromServiceOrder(ServiceOrder entity) {
        String servicePackageDescription = null;

        return new ServiceOrderDetailsResponseDto(
                entity.getId(),
                "SERVICE",
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getPickupDate(),
                entity.getFullPickupAddress(),
                entity.getTargetService() != null ? entity.getTargetService().getName() : "SERWIS WŁASNY",
                entity.getFullDeliveryAddress(),
                entity.getServiceNotes(),
                entity.getAdditionalNotes(),
                entity.getTransportNotes(),
                entity.getTotalPrice(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getStatusDisplayName(),
                entity.getOrderDate(),
                buildBicycleDescription(entity)
        );
    }

    /**
     * Tworzy DTO z TransportOrder
     */
    public static ServiceOrderDetailsResponseDto fromTransportOrder(TransportOrder entity) {
        return new ServiceOrderDetailsResponseDto(
                entity.getId(),
                "TRANSPORT",
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getPickupDate(),
                entity.getFullPickupAddress(),
                entity.getTargetService() != null ? entity.getTargetService().getName() : null,
                entity.getFullDeliveryAddress(),
                "Transport", // Brak pakietu serwisowego
                entity.getAdditionalNotes(),
                entity.getTransportNotes(),
                entity.getTotalPrice(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getStatusDisplayName(),
                entity.getOrderDate(),
                buildBicycleDescription(entity)
        );
    }

    /**
     * Buduje opis roweru z dostępnych danych
     */
    private static String buildBicycleDescription(TransportOrder entity) {
        StringBuilder description = new StringBuilder();

        if (entity.getBicycle() != null) {
            if (entity.getBicycle().getBrand() != null && !entity.getBicycle().getBrand().trim().isEmpty()) {
                description.append(entity.getBicycle().getBrand());
            }

            if (entity.getBicycle().getModel() != null && !entity.getBicycle().getModel().trim().isEmpty()) {
                if (description.length() > 0) {
                    description.append(" ");
                }
                description.append(entity.getBicycle().getModel());
            }

            if (entity.getBicycle().getType() != null && !entity.getBicycle().getType().trim().isEmpty()) {
                if (description.length() > 0) {
                    description.append(" (");
                    description.append(entity.getBicycle().getType());
                    description.append(")");
                } else {
                    description.append(entity.getBicycle().getType());
                }
            }
        }

        // Jeśli nie ma danych o rowerze, użyj ID
        if (description.length() == 0) {
            Long bicycleId = entity.getBicycle() != null ? entity.getBicycle().getId() : null;
            if (bicycleId != null) {
                description.append("Rower ID: ").append(bicycleId);
            } else {
                description.append("Nieznany rower");
            }
        }

        return description.toString();
    }
}