package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.checkout.CheckoutFacade;
import com.marketflow.api.application.checkout.dto.CheckoutQuoteResponse;
import com.marketflow.api.application.checkout.dto.CheckoutRequest;
import com.marketflow.api.application.checkout.dto.CheckoutResponse;
import com.marketflow.api.infrastructure.security.AuthenticatedUser;
import com.marketflow.api.infrastructure.web.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyRole('CUSTOMER','SELLER','ADMIN')")
@Tag(name = "Checkout", description = "Quote and place orders, reserve stock, create payment intents")
public class CheckoutController {

    private static final String CHECKOUT_EXAMPLE = """
            {
              "shippingAddress": {
                "recipientName": "Jane Doe",
                "phone": "+55 11 98888-7777",
                "street": "Av. Paulista",
                "number": "1578",
                "complement": "Apt 42",
                "district": "Bela Vista",
                "city": "São Paulo",
                "state": "SP",
                "postalCode": "01310-200",
                "country": "BR"
              },
              "shippingMethod": "STANDARD",
              "couponCode": "WELCOME10"
            }
            """;

    private final CheckoutFacade checkoutFacade;

    @PostMapping(path = "/quote", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Preview checkout totals",
            description = """
                    Prices the authenticated user's active cart with **current** catalog prices, applies the coupon
                    (Strategy: PERCENTAGE / FIXED), shipping (Strategy: STANDARD / EXPRESS / PICKUP) and tax.
                    Nothing is reserved, charged or persisted.""",
            operationId = "quoteCheckout")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price breakdown",
                    content = @Content(schema = @Schema(implementation = CheckoutQuoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Empty cart, unavailable variant or coupon not applicable",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CheckoutQuoteResponse quote(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Shipping address, shipping method and optional coupon",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CheckoutRequest.class),
                            examples = @ExampleObject(name = "standard", value = CHECKOUT_EXAMPLE)))
            @Valid @RequestBody CheckoutRequest request) {
        return checkoutFacade.quote(user.toActor(), request);
    }

    @Idempotent
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Place order from the active cart",
            description = """
                    Facade orchestrating the whole checkout:
                    1. validates and prices the cart (Strategy for shipping and coupons);
                    2. issues the order (Builder) in `PENDING_PAYMENT`;
                    3. reserves stock with a TTL — optimistic locking first, falling back to `PESSIMISTIC_WRITE`
                       under contention, so overselling is impossible;
                    4. creates a Stripe **PaymentIntent** with order/user metadata and returns its `clientSecret`.

                    Confirm the payment client-side with Stripe.js / mobile SDK. The order becomes `PAID` when the
                    `payment_intent.succeeded` webhook arrives. Unpaid orders are cancelled automatically when
                    `reservationExpiresAt` passes.

                    **Idempotent**: send a unique `Idempotency-Key`; retries with the same key replay the first
                    response with header `Idempotent-Replayed: true` instead of creating a second order.""",
            operationId = "checkout")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed, stock reserved and payment intent created",
                    headers = {
                            @Header(name = "Location", description = "URI of the created order"),
                            @Header(name = "Idempotent-Replayed", description = "Present with value `true` when this is a replayed response")
                    },
                    content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or missing Idempotency-Key",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Same Idempotency-Key still in progress, or stock under heavy contention",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Empty cart, insufficient stock, unavailable variant or invalid coupon",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Payment provider unavailable (order cancelled, stock released, cart restored)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CheckoutResponse> checkout(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", required = true,
                    description = "Unique key per checkout attempt (e.g. a UUID v4). Max 255 chars.",
                    schema = @Schema(type = "string", example = "6f9b8c3e-2a41-4d7a-9a0e-5b1f7c2d9e11"))
            @org.springframework.web.bind.annotation.RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Shipping address, shipping method and optional coupon",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CheckoutRequest.class),
                            examples = @ExampleObject(name = "standard", value = CHECKOUT_EXAMPLE)))
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutFacade.checkout(user.toActor(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/orders/" + response.orderId()))
                .body(response);
    }
}
