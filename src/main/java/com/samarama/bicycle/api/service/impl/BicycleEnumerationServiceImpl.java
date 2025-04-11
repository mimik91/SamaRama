package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.BicycleEnumeration;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.repository.BicycleEnumerationRepository;
import com.samarama.bicycle.api.service.BicycleEnumerationService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BicycleEnumerationServiceImpl implements BicycleEnumerationService {

    private final BicycleEnumerationRepository enumerationRepository;

    // Stałe dla standardowych typów
    public static final String BRAND = "BRAND";
    public static final String BIKE_TYPE = "BIKE_TYPE";
    public static final String FRAME_MATERIAL = "FRAME_MATERIAL";

    // Stałe dla pakietów serwisowych
    public static final String SERVICE_PACKAGE = "SERVICE_PACKAGE";
    public static final String SERVICE_PACKAGE_BASIC = "SERVICE_PACKAGE_BASIC";
    public static final String SERVICE_PACKAGE_EXTENDED = "SERVICE_PACKAGE_EXTENDED";
    public static final String SERVICE_PACKAGE_FULL = "SERVICE_PACKAGE_FULL";

    // Stałe dla statusów zamówień
    public static final String ORDER_STATUS = "ORDER_STATUS";

    public BicycleEnumerationServiceImpl(BicycleEnumerationRepository enumerationRepository) {
        this.enumerationRepository = enumerationRepository;
    }

    @PostConstruct
    public void init() {
        initializeDefaultEnumerations();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getAllEnumerations() {
        List<BicycleEnumeration> enumerations = enumerationRepository.findAll();
        return enumerations.stream()
                .collect(Collectors.toMap(
                        BicycleEnumeration::getType,
                        enumeration -> enumeration.getValues().stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)  // Sortowanie alfabetyczne
                                .collect(Collectors.toList())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEnumerationValues(String type) {
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

        // Typy pakietów serwisowych
        if (!enumerationRepository.existsByType(SERVICE_PACKAGE)) {
            List<String> servicePackages = Arrays.asList(
                    "BASIC", "EXTENDED", "FULL"
            );
            saveEnumeration(SERVICE_PACKAGE, servicePackages);
        }

        // Pakiet podstawowy - szczegóły
        if (!enumerationRepository.existsByType(SERVICE_PACKAGE_BASIC)) {
            List<String> features = Arrays.asList(
                    "Ocena stanu technicznego roweru",
                    "Regulacja hamulców",
                    "Regulacja przerzutek",
                    "Smarowanie łańcucha",
                    "Sprawdzenie ciśnienia w ogumieniu",
                    "Sprawdzenie poprawności skręcenia roweru",
                    "Kontrola luzu sterów",
                    "Kontrola połączeń śrubowych",
                    "Sprawdzenie linek, pancerzy",
                    "Sprawdzenie stanu opon",
                    "Kasowanie luzów i regulacja elementów ruchomych"
            );
            saveEnumeration(SERVICE_PACKAGE_BASIC, features);
        }

        // Pakiet rozszerzony - szczegóły
        if (!enumerationRepository.existsByType(SERVICE_PACKAGE_EXTENDED)) {
            List<String> features = Arrays.asList(
                    "Wszystkie elementy przeglądu podstawowego",
                    "Czyszczenie i smarowanie łańcucha, kasety",
                    "Wymiana smaru w sterach, piastach, suporcie",
                    "Kontrola kół",
                    "Kontrola działania amortyzatora",
                    "W cenie wymiana klocków, linek, pancerzy, dętek, opon, łańcucha, kasety lub wolnobiegu. Do ceny należy doliczyć koszt części, które wymagają wymiany."
            );
            saveEnumeration(SERVICE_PACKAGE_EXTENDED, features);
        }

        // Pakiet pełny - szczegóły
        if (!enumerationRepository.existsByType(SERVICE_PACKAGE_FULL)) {
            List<String> features = Arrays.asList(
                    "Wszystkie elementy przeglądu rozszerzonego",
                    "Mycie roweru",
                    "Czyszczenie i konserwacja przerzutek",
                    "Czyszczenie i smarowanie łańcucha, kasety, korby",
                    "Wymiana smaru w sterach, piastach, suporcie",
                    "Wymiana linek i pancerzy",
                    "Kontrola luzu łożysk suportu, steru, piast",
                    "Sprawdzenie połączeń gwintowych",
                    "Zewnętrzna konserwacja goleni amortyzatora",
                    "Centrowanie kół",
                    "Linki i pancerze oraz mycie roweru są wliczone w cenę przeglądu"
            );
            saveEnumeration(SERVICE_PACKAGE_FULL, features);
        }

        // Statusy zamówień
        if (!enumerationRepository.existsByType(ORDER_STATUS)) {
            List<String> orderStatuses = Arrays.asList(
                    "PENDING", "CONFIRMED", "PICKED_UP", "IN_SERVICE", "COMPLETED", "DELIVERED", "CANCELLED"
            );
            saveEnumeration(ORDER_STATUS, orderStatuses);
        }
    }
}