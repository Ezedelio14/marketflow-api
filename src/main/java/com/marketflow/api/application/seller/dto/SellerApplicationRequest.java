package com.marketflow.api.application.seller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to become a marketplace seller")
public record SellerApplicationRequest(
        @Schema(description = "Public store name", example = "Tech Store") @NotBlank @Size(max = 255) String businessName,
        @Schema(description = "Registered legal name", example = "Tech Store Comercio LTDA") @NotBlank @Size(max = 255) String legalName,
        @Schema(description = "Tax id (CNPJ/VAT)", example = "12.345.678/0001-90") @NotBlank @Size(max = 32) String taxNumber,
        @Schema(description = "Commercial e-mail", example = "contact@techstore.com") @NotBlank @Email String email,
        @Schema(description = "Commercial phone", example = "+55 11 99999-0000") @Size(max = 32) String phone
) {
}
