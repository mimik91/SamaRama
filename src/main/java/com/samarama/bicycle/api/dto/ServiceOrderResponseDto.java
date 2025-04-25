package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.IncompleteBike;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning service order information to the client.
 * Contains only the necessary data and excludes sensitive or internal details.
 */
public record ServiceOrderResponseDto(
        Long id,
        Long bicycleId,
        String bicycleBrand,
        String bicycleModel,
        String servicePackageCode,
        String servicePackageName,
        LocalDate pickupDate,
        String pickupAddress,
        BigDecimal price,
        LocalDateTime orderDate,
        String additionalNotes,
        String status,
        String serviceNotes
) {
    public static ServiceOrderResponseDto fromEntity(ServiceOrder entity) {
        return new ServiceOrderResponseDto(
                entity.getId(),
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getServicePackageCode(),
                entity.getServicePackage() != null ? entity.getServicePackage().getName() : null,
                entity.getPickupDate(),
                entity.getPickupAddress(),
                entity.getPrice(),
                entity.getOrderDate(),
                entity.getAdditionalNotes(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getServiceNotes()
        );
    }
}