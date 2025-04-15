package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.service.BicycleEnumerationService;
import com.samarama.bicycle.api.service.ServicePackageService;
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
    private final ServicePackageService servicePackageService;

    public BicycleEnumerationController(BicycleEnumerationService enumerationService,
                                        ServicePackageService servicePackageService) {
        this.enumerationService = enumerationService;
        this.servicePackageService = servicePackageService;
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
        var packages = servicePackageService.getActiveServicePackages();
        for (var pkg : packages) {
            metadata.put(pkg.getCode(), pkg.getPrice().toString());
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