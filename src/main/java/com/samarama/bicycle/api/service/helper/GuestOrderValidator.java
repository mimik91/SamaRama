package com.samarama.bicycle.api.service.helper;

import com.samarama.bicycle.api.dto.ServiceOrTransportOrderDto;
import com.samarama.bicycle.api.repository.ServicePackageRepository;
import com.samarama.bicycle.api.service.ServiceSlotService;
import com.samarama.bicycle.api.service.impl.CityValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class GuestOrderValidator {

    private final ServicePackageRepository servicePackageRepository;
    private final ServiceSlotService serviceSlotService;
    private final CityValidator cityValidator;

    public GuestOrderValidator(
            ServicePackageRepository servicePackageRepository,
            ServiceSlotService serviceSlotService,
            CityValidator cityValidator) {
        this.servicePackageRepository = servicePackageRepository;
        this.serviceSlotService = serviceSlotService;
        this.cityValidator = cityValidator;
    }

    public List<String> validateGuestOrder(ServiceOrTransportOrderDto orderDto) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        if (orderDto.bicycles() == null || orderDto.bicycles().isEmpty()) {
            errors.add("Brak rowerów w zamówieniu");
        }

        if (orderDto.servicePackageId() == null || !servicePackageRepository.existsById(orderDto.servicePackageId())) {
            errors.add("Nieprawidłowy pakiet serwisowy");
        }

        if (orderDto.city() == null || !cityValidator.isValidCity(orderDto.city())) {
            errors.add("Nieprawidłowe miasto");
        }

        if (orderDto.pickupDate() == null || orderDto.pickupDate().isBefore(LocalDate.now())) {
            errors.add("Nieprawidłowa data odbioru");
        }

        // Slot validation
        if (orderDto.pickupDate() != null && orderDto.bicycles() != null) {
            int bikesCount = orderDto.bicycles().size();

            if (!serviceSlotService.isWithinMaxBikesPerOrder(orderDto.pickupDate(), bikesCount)) {
                int maxPerOrder = serviceSlotService.getMaxBikesPerOrder(orderDto.pickupDate());
                errors.add("Przekroczono maksymalną liczbę rowerów na jedno zamówienie (" + maxPerOrder + ")");
            }

            if (!serviceSlotService.areSlotsAvailable(orderDto.pickupDate(), bikesCount)) {
                errors.add("Brak wystarczającej liczby wolnych miejsc na wybrany dzień");
            }
        }

        return errors;
    }
}