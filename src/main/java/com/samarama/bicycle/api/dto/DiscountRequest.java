package com.samarama.bicycle.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountRequest(String coupon, BigDecimal currentTransportPrice, LocalDate orderDate) {
}
