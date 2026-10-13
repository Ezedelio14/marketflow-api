package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.coupon.CouponService;
import com.marketflow.api.application.coupon.dto.CouponResponse;
import com.marketflow.api.application.coupon.dto.CreateCouponRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(path = "/api/v1/coupons", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Coupons", description = "Discount coupons — PERCENTAGE and FIXED strategies (admin)")
public class CouponController {

    private final CouponService couponService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create coupon", operationId = "createCoupon")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coupon created",
                    content = @Content(schema = @Schema(implementation = CouponResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Code already exists",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid coupon rules",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CouponResponse create(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Coupon definition", required = true)
                                 @Valid @RequestBody CreateCouponRequest request) {
        return couponService.create(request);
    }

    @GetMapping
    @Operation(summary = "List coupons", operationId = "listCoupons")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of coupons"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<CouponResponse> list(@ParameterObject @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return couponService.list(pageable);
    }

    @PostMapping("/{couponId}/deactivate")
    @Operation(summary = "Deactivate coupon", operationId = "deactivateCoupon")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon deactivated",
                    content = @Content(schema = @Schema(implementation = CouponResponse.class))),
            @ApiResponse(responseCode = "404", description = "Coupon not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CouponResponse deactivate(@Parameter(description = "Coupon id", required = true) @PathVariable UUID couponId) {
        return couponService.deactivate(couponId);
    }
}
