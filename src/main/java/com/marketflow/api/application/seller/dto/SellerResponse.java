package com.marketflow.api.application.seller.dto;

import com.marketflow.api.domain.seller.Seller;
import com.marketflow.api.domain.seller.SellerStatus;
import com.marketflow.api.domain.seller.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Seller profile")
public record SellerResponse(
        @Schema(description = "Seller id") UUID id,
        @Schema(description = "Owner user id") UUID userId,
        @Schema(description = "Public store name", example = "Tech Store") String businessName,
        @Schema(description = "Legal name") String legalName,
        @Schema(description = "Tax id") String taxNumber,
        @Schema(description = "Commercial e-mail") String email,
        @Schema(description = "Commercial phone") String phone,
        @Schema(description = "Operational status", example = "ACTIVE") SellerStatus status,
        @Schema(description = "KYC verification status", example = "APPROVED") VerificationStatus verificationStatus,
        @Schema(description = "Creation instant") Instant createdAt
) {
    public static SellerResponse from(Seller s) {
        return new SellerResponse(s.getId(), s.getUserId(), s.getBusinessName(), s.getLegalName(), s.getTaxNumber(),
                s.getEmail(), s.getPhone(), s.getStatus(), s.getVerificationStatus(), s.getCreatedAt());
    }
}
