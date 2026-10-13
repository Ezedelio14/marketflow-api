package com.marketflow.api.application.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Shipment dispatch data")
public record ShipOrderRequest(
        @Schema(description = "Carrier name", example = "Correios") @NotBlank @Size(max = 100) String carrier,
        @Schema(description = "Carrier tracking code", example = "BR123456789BR") @NotBlank @Size(max = 100) String trackingNumber
) {
}
