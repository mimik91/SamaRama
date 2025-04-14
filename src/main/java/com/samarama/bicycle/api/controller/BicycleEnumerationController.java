package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.service.BicycleEnumerationService;
import com.samarama.bicycle.api.service.impl.BicycleEnumerationServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/enumerations")
public class BicycleEnumerationController {

    private final BicycleEnumerationService enumerationService;

    public BicycleEnumerationController(BicycleEnumerationService enumerationService) {
        this.enumerationService = enumerationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<String>>> getAllEnumerations() {
        Map<String, List<String>> enumerations = enumerationService.getAllEnumerations();
        return ResponseEntity.ok(enumerations);
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<String>> getEnumerationByType(@PathVariable String type) {
        List<String> values = enumerationService.getEnumerationValues(type);
        return ResponseEntity.ok(values);
    }

    @GetMapping("/{type}/metadata")
    public ResponseEntity<Map<String, Object>> getEnumerationMetadata(@PathVariable String type) {
        Map<String, Object> metadata = new HashMap<>();

        if(type.equals("SERVICE_PACKAGE_PRICES")){
            List<String> basicPricesList = enumerationService.getEnumerationValues(BicycleEnumerationServiceImpl.SERVICE_PACKAGE_BASIC_PRICE);
            List<String> extendedPricesList = enumerationService.getEnumerationValues(BicycleEnumerationServiceImpl.SERVICE_PACKAGE_EXTENDED_PRICE);
            List<String> fullPricesList = enumerationService.getEnumerationValues(BicycleEnumerationServiceImpl.SERVICE_PACKAGE_FULL_PRICE);

            if (!basicPricesList.isEmpty()) metadata.put("BASIC", basicPricesList.get(0));
            if (!extendedPricesList.isEmpty()) metadata.put("EXTENDED", extendedPricesList.get(0));
            if (!fullPricesList.isEmpty()) metadata.put("FULL", fullPricesList.get(0));

        } else {
            List<String> prices = enumerationService.getEnumerationValues(type + "_PRICE");
            List<String> names = enumerationService.getEnumerationValues(type + "_NAME");
            List<String> descriptions = enumerationService.getEnumerationValues(type + "_DESC");

            if (!prices.isEmpty()) metadata.put("price", prices.get(0));
            if (!names.isEmpty()) metadata.put("name", names.get(0));
            if (!descriptions.isEmpty()) metadata.put("description", descriptions.get(0));
        }
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/{type}")
    public ResponseEntity<?> updateEnumeration(
            @PathVariable String type,
            @RequestBody List<String> values) {
        enumerationService.saveEnumeration(type, values);
        return ResponseEntity.ok().build();
    }
}