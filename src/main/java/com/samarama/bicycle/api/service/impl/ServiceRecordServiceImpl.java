package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceRecordDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.ServiceRecord;
import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.repository.BicycleRepository;
import com.samarama.bicycle.api.repository.ServiceRecordRepository;
import com.samarama.bicycle.api.repository.UserRepository;
import com.samarama.bicycle.api.service.ServiceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServiceRecordServiceImpl implements ServiceRecordService {
    private final ServiceRecordRepository serviceRecordRepository;
    private final BicycleRepository bicycleRepository;
    private final UserRepository userRepository;

    public ServiceRecordServiceImpl(ServiceRecordRepository serviceRecordRepository,
                                    BicycleRepository bicycleRepository,
                                    UserRepository userRepository) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.bicycleRepository = bicycleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<List<ServiceRecord>> getBicycleServiceRecords(Long bicycleId, String userEmail) {
        try {
            // Historia serwisowa istnieje tylko dla kompletnych rowerów (Bicycle)
            Optional<Bicycle> bicycleOpt = bicycleRepository.findById(bicycleId);
            if (bicycleOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Bicycle bicycle = bicycleOpt.get();

            // Sprawdź, czy użytkownik ma odpowiednie uprawnienia
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isService = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals("ROLE_SERVICE"));

            // Jeśli nie jest serwisem, sprawdź czy użytkownik jest właścicielem roweru
            if (!isService) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (bicycle.getOwner() == null || !bicycle.getOwner().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).build();
                }
            }

            List<ServiceRecord> records = serviceRecordRepository.findByBicycle(bicycle);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> addServiceRecord(ServiceRecordDto serviceRecordDto, String userEmail) {
        // Validate service date is not more than a month in the past
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        if (serviceRecordDto.serviceDate().isBefore(oneMonthAgo)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Service date cannot be more than a month ago"));
        }

        // Historia serwisowa istnieje tylko dla kompletnych rowerów (Bicycle)
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(serviceRecordDto.bicycleId());
        if (bicycleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bicycle not found or not complete"));
        }

        Bicycle bicycle = bicycleOpt.get();

        // Sprawdź czy rower ma numer ramy
        if (bicycle.getFrameNumber() == null || bicycle.getFrameNumber().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bicycle must have a frame number to add service records"));
        }

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setBicycle(bicycle);
        serviceRecord.setName(serviceRecordDto.name());
        serviceRecord.setDescription(serviceRecordDto.description());
        serviceRecord.setServiceDate(serviceRecordDto.serviceDate());
        serviceRecord.setPrice(serviceRecordDto.price());


        serviceRecordRepository.save(serviceRecord);
        return ResponseEntity.ok(Map.of("message", "Service record added successfully"));
    }
}