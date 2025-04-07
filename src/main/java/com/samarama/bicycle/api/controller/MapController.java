package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.CoordinateDto;
import com.samarama.bicycle.api.service.MapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/map")
public class MapController {
    private static final Logger logger = Logger.getLogger(MapController.class.getName());

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/service-pins")
    public ResponseEntity<List<CoordinateDto>> getServicePins() {
        logger.info("Received request to fetch bike service pins");
        List<CoordinateDto> coordinates = mapService.getServicePins();
        return ResponseEntity.ok(coordinates);
    }
}