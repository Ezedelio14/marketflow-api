package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.order.OrderService;
import com.marketflow.api.application.order.dto.CancelOrderRequest;
import com.marketflow.api.application.order.dto.OrderFilterParams;
import com.marketflow.api.application.order.dto.OrderResponse;
import com.marketflow.api.application.order.dto.OrderSummaryResponse;
import com.marketflow.api.application.order.dto.RefundRequest;
import com.marketflow.api.application.order.dto.ShipOrderRequest;
import com.marketflow.api.infrastructure.security.AuthenticatedUser;
import com.marketflow.api.infrastructure.web.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Orders", description = """
        Order queries and lifecycle driven by the State pattern:
        `PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED`, `PENDING_PAYMENT → CANCELLED`,
        `PAID → REFUND_REQUESTED → REFUNDED`. Invalid transitions return **409 INVALID_STATE_TRANSITION**;
        every transition is recorded in `statusHistory`.""")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "List orders",
            description = """
                    Paginated search built with JPA Specifications. Scope is enforced by role:
                    customers see their own orders, sellers see orders containing their items, admins see all
                    (and may filter by `userId` / `sellerId`).""",
            operationId = "listOrders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or sort property",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<OrderSummaryResponse> list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @ParameterObject OrderFilterParams filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.list(user.toActor(), filter.toFilter(), pageable);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get order details",
            description = "Full order with items, status history and the actions currently allowed by the state machine.",
            operationId = "getOrder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found or not visible to the caller",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true, example = "8b0c7c7e-3f7a-4b44-9a0e-1f2d3c4b5a69")
            @PathVariable UUID orderId) {
        return orderService.get(user.toActor(), orderId);
    }

    @Idempotent
    @PostMapping(path = "/{orderId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER','SELLER','ADMIN')")
    @Operation(
            summary = "Cancel an unpaid order",
            description = """
                    `PENDING_PAYMENT → CANCELLED`. Releases reserved stock, gives the coupon usage back and cancels
                    the Stripe PaymentIntent. Allowed for the order owner or an admin.""",
            operationId = "cancelOrder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing Idempotency-Key",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order is not PENDING_PAYMENT (invalid state transition)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true) @PathVariable UUID orderId,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", required = true,
                    description = "Unique key for this mutation", schema = @Schema(type = "string"))
            @org.springframework.web.bind.annotation.RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional cancellation reason", required = true,
                    content = @Content(schema = @Schema(implementation = CancelOrderRequest.class)))
            @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancel(user.toActor(), orderId, request.reason());
    }

    @Idempotent
    @PostMapping(path = "/{orderId}/refund-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER','SELLER','ADMIN')")
    @Operation(
            summary = "Request a refund",
            description = "`PAID → REFUND_REQUESTED`. The refund is executed after admin approval.",
            operationId = "requestRefund")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund requested",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or missing Idempotency-Key",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order is not PAID",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse requestRefund(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true) @PathVariable UUID orderId,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", required = true,
                    description = "Unique key for this mutation", schema = @Schema(type = "string"))
            @org.springframework.web.bind.annotation.RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refund reason", required = true,
                    content = @Content(schema = @Schema(implementation = RefundRequest.class)))
            @Valid @RequestBody RefundRequest request) {
        return orderService.requestRefund(user.toActor(), orderId, request.reason());
    }

    @Idempotent
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Approve and execute a refund (admin)",
            description = """
                    Issues the refund at Stripe (with an idempotency key). The order moves
                    `REFUND_REQUESTED → REFUNDED` immediately when Stripe confirms synchronously, otherwise when the
                    `charge.refunded` webhook arrives.""",
            operationId = "approveRefund")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund issued",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order is not REFUND_REQUESTED",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Payment provider error",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse approveRefund(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true) @PathVariable UUID orderId,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", required = true,
                    description = "Unique key for this mutation", schema = @Schema(type = "string"))
            @org.springframework.web.bind.annotation.RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return orderService.approveRefund(user.toActor(), orderId);
    }

    @PostMapping("/{orderId}/processing")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(summary = "Start fulfilment", description = "`PAID → PROCESSING`. Seller of an item in the order or admin.",
            operationId = "startOrderProcessing")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order is being processed",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not sell items of this order",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order is not PAID",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse startProcessing(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true) @PathVariable UUID orderId) {
        return orderService.startProcessing(user.toActor(), orderId);
    }

    @PostMapping(path = "/{orderId}/ship", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(summary = "Ship order",
            description = "`PROCESSING → SHIPPED`. Creates a shipment with carrier/tracking and publishes `order.shipped`.",
            operationId = "shipOrder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order shipped",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not sell items of this order",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order is not PROCESSING",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse ship(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true) @PathVariable UUID orderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Carrier and tracking number", required = true,
                    content = @Content(schema = @Schema(implementation = ShipOrderRequest.class)))
            @Valid @RequestBody ShipOrderRequest request) {
        return orderService.ship(user.toActor(), orderId, request);
    }

    @PostMapping("/{orderId}/deliver")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(summary = "Confirm delivery", description = "`SHIPPED → DELIVERED`.", operationId = "deliverOrder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order delivered",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not sell items of this order",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order is not SHIPPED",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OrderResponse deliver(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Order id", required = true) @PathVariable UUID orderId) {
        return orderService.deliver(user.toActor(), orderId);
    }
}
