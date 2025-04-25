package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.ServiceOrderDto;
import com.samarama.bicycle.api.dto.ServiceOrderResponseDto;
import com.samarama.bicycle.api.model.ServiceOrder;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ServiceOrderService {
    /**
     * Get service orders for the current user
     * @param userEmail email of the current user
     * @return list of service order DTOs
     */
    List<ServiceOrderResponseDto> getUserServiceOrders(String userEmail);

    /**
     * Get service orders for a specific bicycle
     * @param bicycleId ID of the bicycle
     * @param userEmail email of the current user
     * @return list of service order DTOs
     */
    List<ServiceOrderResponseDto> getBicycleServiceOrders(Long bicycleId, String userEmail);

    /**
     * Get a service order by ID
     * @param orderId ID of the service order
     * @param userEmail email of the current user
     * @return the service order DTO or not found
     */
    ResponseEntity<ServiceOrderResponseDto> getServiceOrderById(Long orderId, String userEmail);

    /**
     * Create a new service order
     * @param serviceOrderDto service order data
     * @param userEmail email of the current user
     * @return response with the result
     */
    ResponseEntity<?> createServiceOrder(ServiceOrderDto serviceOrderDto, String userEmail);

    /**
     * Cancel a service order
     * @param orderId ID of the service order
     * @param userEmail email of the current user
     * @return response with the result
     */
    ResponseEntity<?> cancelServiceOrder(Long orderId, String userEmail);

    /**
     * Get price for a service package
     * @param servicePackageCode the service package code (legacy method)
     * @return the price for the service package
     */
    ResponseEntity<?> getServicePackagePrice(String servicePackageCode);

    long countServiceOrders();

    List<ServiceOrderResponseDto> getAllServiceOrders();
}