package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.model.BicycleEnumeration;
import java.util.List;
import java.util.Map;

public interface BicycleEnumerationService {

    /**
     * Get all bicycle enumerations (brands, types, materials)
     * @return map of enumeration types and their values
     */
    Map<String, List<String>> getAllEnumerations();

    /**
     * Get values for specific enumeration type
     * @param type enumeration type (e.g. "BRAND", "BIKE_TYPE", "FRAME_MATERIAL", "SERVICE_PACKAGE")
     * @return list of values or empty list if not found
     */
    List<String> getEnumerationValues(String type);

    /**
     * Add or update an enumeration with values
     * @param type enumeration type
     * @param values list of values
     * @return updated enumeration
     * @throws UnsupportedOperationException if trying to update SERVICE_PACKAGE directly
     */
    BicycleEnumeration saveEnumeration(String type, List<String> values);

    /**
     * Initialize default enumerations if they don't exist
     */
    void initializeDefaultEnumerations();
}