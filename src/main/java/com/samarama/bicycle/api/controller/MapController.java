package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.CoordinateDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/map")
public class MapController {
    private static final Logger logger = Logger.getLogger(MapController.class.getName());

    private final BikeServiceRepository bikeServiceRepository;

    public MapController(BikeServiceRepository bikeServiceRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
    }

    @GetMapping("/service-pins")
    public ResponseEntity<List<CoordinateDto>> getServicePins() {
        logger.info("Pobieranie koordynatów wszystkich serwisów rowerowych");

        List<BikeService> services = bikeServiceRepository.findAll();

        List<CoordinateDto> coordinates = services.stream()
                .filter(service -> service.getLatitude() != null && service.getLongitude() != null)
                .map(CoordinateDto::fromBikeService)
                .collect(Collectors.toList());

        logger.info("Znaleziono " + coordinates.size() + " serwisów z koordynatami");

        for (CoordinateDto coord : coordinates) {
            logger.info("Serwis ID: " + coord.serviceId() +
                    ", Nazwa: " + coord.name() +
                    ", Pozycja: [" + coord.latitude() + ", " + coord.longitude() + "]");
        }

        return ResponseEntity.ok(coordinates);
    }
}