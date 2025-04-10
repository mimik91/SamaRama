package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.service.BicycleEnumerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{type}")
    public ResponseEntity<?> updateEnumeration(
            @PathVariable String type,
            @RequestBody List<String> values) {
        enumerationService.saveEnumeration(type, values);
        return ResponseEntity.ok().build();
    }
}