package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.seller.SellerService;
import com.marketflow.api.application.seller.dto.SellerApplicationRequest;
import com.marketflow.api.application.seller.dto.SellerResponse;
import com.marketflow.api.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sellers", description = "Seller onboarding (KYC) and verification")
public class SellerController {

    private final SellerService sellerService;

    @PostMapping(path = "/sellers", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Apply to become a seller",
            description = "Creates a PENDING seller profile. After admin approval the user gets the SELLER role (log in again to refresh the token).",
            operationId = "applySeller")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Profile already exists or tax number in use",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SellerResponse apply(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Business data", required = true)
                                @Valid @RequestBody SellerApplicationRequest request) {
        return sellerService.apply(user.toActor(), request);
    }

    @GetMapping("/sellers/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "My seller profile", operationId = "mySellerProfile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller profile",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "404", description = "No seller profile",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SellerResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return sellerService.me(user.toActor());
    }

    @PostMapping("/admin/sellers/{sellerId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve seller (admin)", operationId = "approveSeller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller approved",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Seller not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SellerResponse approve(@Parameter(description = "Seller id", required = true) @PathVariable UUID sellerId) {
        return sellerService.approve(sellerId);
    }

    @PostMapping("/admin/sellers/{sellerId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject seller (admin)", operationId = "rejectSeller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller rejected",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Seller not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SellerResponse reject(@Parameter(description = "Seller id", required = true) @PathVariable UUID sellerId) {
        return sellerService.reject(sellerId);
    }
}
