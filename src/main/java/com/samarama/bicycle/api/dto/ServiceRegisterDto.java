package com.samarama.bicycle.api.dto;

import lombok.Data;

@Data
public class ServiceRegisterDto {
    String name;
    String phoneNumber;
    String email;
    String serviceName;
}
