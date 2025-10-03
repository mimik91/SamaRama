package com.samarama.bicycle.api.dto.mapDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceDetailDto {
    private String id;
    private String name;
    private AddressDetailDto address;
    private ContactDto contact;
    private List<ServiceDto> services;
    private boolean verified;
    private BigDecimal transportCost;
    private boolean transportAvailable;
    private RatingDto rating;
    private List<String> photos;
    private Map<String, Object> metadata;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressDetailDto {
        private String street;
        private String building;
        private String flat;
        private String city;
        private String zipCode;
        private double latitude;
        private double longitude;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContactDto {
        private String phone;
        private String email;
        private String website;
        private String facebook;
        private String instagram;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceDto {
        private String name;
        private String description;
        private Double price;
        private String category;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RatingDto {
        private double average;
        private int count;
        private Map<Integer, Integer> distribution; // 1-5 stars -> count
    }
}