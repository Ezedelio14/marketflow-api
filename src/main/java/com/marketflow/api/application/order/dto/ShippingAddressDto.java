package com.marketflow.api.application.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Delivery address (stored as a JSONB snapshot on the order)")
public record ShippingAddressDto(
        @Schema(description = "Recipient full name", example = "Jane Doe") @NotBlank @Size(max = 120) String recipientName,
        @Schema(description = "Contact phone", example = "+55 11 98888-7777") @NotBlank @Size(max = 32) String phone,
        @Schema(description = "Street", example = "Av. Paulista") @NotBlank @Size(max = 200) String street,
        @Schema(description = "Number", example = "1578") @NotBlank @Size(max = 20) String number,
        @Schema(description = "Complement", example = "Apt 42", nullable = true) @Size(max = 100) String complement,
        @Schema(description = "District / neighbourhood", example = "Bela Vista") @Size(max = 100) String district,
        @Schema(description = "City", example = "São Paulo") @NotBlank @Size(max = 100) String city,
        @Schema(description = "State / province", example = "SP") @NotBlank @Size(max = 50) String state,
        @Schema(description = "Postal code", example = "01310-200") @NotBlank @Size(max = 20) String postalCode,
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "BR") @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String country
) {
}
