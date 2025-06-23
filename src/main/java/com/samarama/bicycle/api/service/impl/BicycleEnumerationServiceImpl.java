package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.BicycleEnumeration;
import com.samarama.bicycle.api.repository.BicycleEnumerationRepository;
import com.samarama.bicycle.api.service.BicycleEnumerationService;
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

    // Stałe dla statusów zamówień
    public static final String ORDER_STATUS = "ORDER_STATUS";

    public BicycleEnumerationServiceImpl(BicycleEnumerationRepository enumerationRepository) {
        this.enumerationRepository = enumerationRepository;
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

}