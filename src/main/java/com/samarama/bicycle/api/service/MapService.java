package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.CoordinateDto;

import java.util.List;

public interface MapService {
    /**
     * Get coordinates of all bike services for map display
     * @return list of coordinate data for map pins
     */
    List<CoordinateDto> getServicePins();
}