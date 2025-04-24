package com.samarama.bicycle.api.controller;

import com.samarama.bicycle.api.dto.GuestServiceOrderDto;
import com.samarama.bicycle.api.service.GuestOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/guest-orders")
public class GuestOrderController {

    private final GuestOrderService guestOrderService;

    public GuestOrderController(GuestOrderService guestOrderService) {
        this.guestOrderService = guestOrderService;
    }

    @PostMapping
    public ResponseEntity<?> createGuestOrder(@Valid @RequestBody GuestServiceOrderDto orderDto) {
        return guestOrderService.processGuestOrder(orderDto);
    }
}