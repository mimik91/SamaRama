package com.samarama.bicycle.api.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record OrderSummaryRequest(
        @NotEmpty(message = "Order IDs list cannot be empty")
        List<Long> orderIds
) {}
