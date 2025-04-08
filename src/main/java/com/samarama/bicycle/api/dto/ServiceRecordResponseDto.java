package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.ServiceRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordResponseDto(
        Long id,
        Long bicycleId,
        String name,
        String description,
        LocalDate serviceDate,
        BigDecimal price,
        BikeServiceDto service
) {
    public static ServiceRecordResponseDto fromEntity(ServiceRecord entity) {
        BikeServiceDto serviceDto = entity.getService() != null
                ? new BikeServiceDto(
                entity.getService().getId(),
                entity.getService().getName(),
                entity.getService().getEmail(),
                entity.getService().getPhoneNumber()
        )
                : null;

        return new ServiceRecordResponseDto(
                entity.getId(),
                entity.getBicycle().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getServiceDate(),
                entity.getPrice(),
                serviceDto
        );
    }

    // Uproszczone DTO dla usługi serwisowej
    public record BikeServiceDto(
            Long id,
            String name,
            String email,
            String phoneNumber
    ) {}
}