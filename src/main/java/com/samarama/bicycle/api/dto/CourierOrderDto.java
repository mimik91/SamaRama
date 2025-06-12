// src/main/java/com/samarama/bicycle/api/dto/CourierOrderDto.java
package com.samarama.bicycle.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dla zamówień w panelu kuriera
 */
@Data
@NoArgsConstructor
public class CourierOrderDto {

    private Long id;
    private String status; // CONFIRMED lub ON_THE_WAY_BACK
    private String orderDate;
    private String pickupDate;
    private String pickupTimeWindow;
    private String pickupAddress;
    private String deliveryAddress;
    private String bikeBrand;
    private String bikeModel;
    private String clientEmail;
    private String clientPhone;

}