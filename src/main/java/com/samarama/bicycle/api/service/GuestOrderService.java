package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.ServiceOrTransportOrderDto;
import org.springframework.http.ResponseEntity;

public interface GuestOrderService {
    /**
     * Process a complete guest order, creating temporary user, bicycles and service order
     * @param orderDto complete order data from guest
     * @return response with operation result
     */
    ResponseEntity<?> processGuestOrder(ServiceOrTransportOrderDto orderDto);
}