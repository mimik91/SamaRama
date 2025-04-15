package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.BicycleEnumeration;
import com.samarama.bicycle.api.repository.BicycleEnumerationRepository;
import com.samarama.bicycle.api.service.BicycleEnumerationService;
import com.samarama.bicycle.api.service.ServicePackageService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BicycleEnumerationServiceImpl implements BicycleEnumerationService {

    private final BicycleEnumerationRepository enumerationRepository;
    private final ServicePackageService servicePackageService;

    // Stałe dla standardowych typów
    public static final String BRAND = "BRAND";
    public static final String BIKE_TYPE = "BIKE_TYPE";
    public static final String FRAME_MATERIAL = "FRAME_MATERIAL";
    public static final String SERVICE_PACKAGE = "SERVICE_PACKAGE";

    // Stałe dla statusów zamówień
    public static final String ORDER_STATUS = "ORDER_STATUS";

    public BicycleEnumerationServiceImpl(BicycleEnumerationRepository enumerationRepository,
                                         ServicePackageService servicePackageService) {
        this.enumerationRepository = enumerationRepository;
        this.servicePackageService = servicePackageService;
    }

    @PostConstruct
    public void init() {
        initializeDefaultEnumerations();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getAllEnumerations() {
        List<BicycleEnumeration> enumerations = enumerationRepository.findAll();

        Map<String, List<String>> result = enumerations.stream()
                .collect(Collectors.toMap(
                        BicycleEnumeration::getType,
                        enumeration -> enumeration.getValues().stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)  // Sortowanie alfabetyczne
                                .collect(Collectors.toList())
                ));

        // Add service package codes from the ServicePackageService
        if (!result.containsKey(SERVICE_PACKAGE)) {
            var servicePackages = servicePackageService.getActiveServicePackages();
            if (!servicePackages.isEmpty()) {
                List<String> packageCodes = servicePackages.stream()
                        .map(pkg -> pkg.getCode())
                        .collect(Collectors.toList());
                result.put(SERVICE_PACKAGE, packageCodes);
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEnumerationValues(String type) {
        // Special case for SERVICE_PACKAGE
        if (SERVICE_PACKAGE.equals(type)) {
            var servicePackages = servicePackageService.getActiveServicePackages();
            return servicePackages.stream()
                    .map(pkg -> pkg.getCode())
                    .collect(Collectors.toList());
        }

        return enumerationRepository.findByType(type)
                .map(enumeration -> {
                    List<String> sortedValues = new ArrayList<>(enumeration.getValues());
                    // Sortowanie alfabetyczne z uwzględnieniem przypadków
                    sortedValues.sort(String.CASE_INSENSITIVE_ORDER);
                    return sortedValues;
                })
                .orElse(new ArrayList<>());
    }

    @Override
    @Transactional
    public BicycleEnumeration saveEnumeration(String type, List<String> values) {
        // Special case for SERVICE_PACKAGE - don't allow direct updates
        if (SERVICE_PACKAGE.equals(type)) {
            throw new UnsupportedOperationException(
                    "Service packages cannot be updated directly through the enumeration API. " +
                            "Please use the ServicePackageController to manage service packages."
            );
        }

        BicycleEnumeration enumeration = enumerationRepository.findByType(type)
                .orElse(new BicycleEnumeration(type, new ArrayList<>()));

        enumeration.setValues(values);
        return enumerationRepository.save(enumeration);
    }

    @Override
    @Transactional
    public void initializeDefaultEnumerations() {
        // Marki rowerów
        if (!enumerationRepository.existsByType(BRAND)) {
            List<String> brands = Arrays.asList(
                    "Trek", "Specialized", "Giant", "Cannondale", "Scott",
                    "Merida", "Cube", "Canyon", "Bianchi", "BMC",
                    "Kross", "Orbea", "Ghost", "Fuji", "GT",
                    "Pinarello", "Cervelo", "Focus", "Felt", "Lapierre",
                    "Romet", "Kellys", "Unibike", "Kona", "Marin",
                    "Santa Cruz", "Norco", "Commencal", "YT", "Devinci"
            );
            saveEnumeration(BRAND, brands);
        }

        // Typy rowerów
        if (!enumerationRepository.existsByType(BIKE_TYPE)) {
            List<String> bikeTypes = Arrays.asList(
                    "Górski (MTB)", "Szosowy", "Gravel", "Miejski",
                    "Trekkingowy", "BMX", "Dziecięcy", "Elektryczny",
                    "Fatbike", "Crossowy", "Fitness", "Składany"
            );
            saveEnumeration(BIKE_TYPE, bikeTypes);
        }

        // Materiały ram
        if (!enumerationRepository.existsByType(FRAME_MATERIAL)) {
            List<String> frameMaterials = Arrays.asList(
                    "Aluminium", "Karbon (carbon)", "Stal", "Tytan",
                    "Chrom-molibden", "Magnez", "Kompozyt"
            );
            saveEnumeration(FRAME_MATERIAL, frameMaterials);
        }

        // Statusy zamówień
        if (!enumerationRepository.existsByType(ORDER_STATUS)) {
            List<String> orderStatuses = Arrays.asList(
                    "PENDING", "CONFIRMED", "PICKED_UP", "IN_SERVICE", "COMPLETED", "DELIVERED", "CANCELLED"
            );
            saveEnumeration(ORDER_STATUS, orderStatuses);
        }

        // Inicjalizacja pakietów serwisowych - używamy teraz ServicePackageService
        servicePackageService.initializeDefaultServicePackages();
    }
}