package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.CoordinateDto;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import com.samarama.bicycle.api.service.MapService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class MapServiceImpl implements MapService {
    private static final Logger logger = Logger.getLogger(MapServiceImpl.class.getName());

    private final BikeServiceRepository bikeServiceRepository;

    public MapServiceImpl(BikeServiceRepository bikeServiceRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
    }

    @Override
    public List<CoordinateDto> getServicePins() {
        logger.info("Fetching coordinates of all bike services");

        List<BikeService> services = bikeServiceRepository.findAll();

        List<CoordinateDto> coordinates = services.stream()
                .filter(service -> service.getLatitude() != null && service.getLongitude() != null)
                .map(CoordinateDto::fromBikeService)
                .collect(Collectors.toList());

        logger.info("Found " + coordinates.size() + " services with coordinates");

        for (CoordinateDto coord : coordinates) {
            logger.info("Service ID: " + coord.serviceId() +
                    ", Name: " + coord.name() +
                    ", Position: [" + coord.latitude() + ", " + coord.longitude() + "]");
        }

        return coordinates;
    }
}