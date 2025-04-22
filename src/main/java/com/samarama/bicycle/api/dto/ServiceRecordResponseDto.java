package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceRecord;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordResponseDto(
        Long id,
        Long bicycleId,
        String name,
        String description,
        LocalDate serviceDate,
        BigDecimal price
) {
    public static ServiceRecordResponseDto fromEntity(ServiceRecord entity) {
        return new ServiceRecordResponseDto(
                entity.getId(),
                entity.getBicycle().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getServiceDate(),
                entity.getPrice()
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