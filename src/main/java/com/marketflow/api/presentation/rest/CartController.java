package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.cart.CartService;
import com.marketflow.api.application.cart.dto.AddCartItemRequest;
import com.marketflow.api.application.cart.dto.CartResponse;
import com.marketflow.api.application.cart.dto.UpdateCartItemRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping(path = "/api/v1/cart", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cart", description = "Shopping cart: PostgreSQL source of truth with a write-through Redis cache")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get my cart", description = "Served from Redis when cached.", operationId = "getCart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current cart",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CartResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping(path = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add item", description = "Adds (or increments) a variant; checks availability softly.",
            operationId = "addCartItem")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated cart",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Variant unavailable or insufficient stock",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CartResponse addItem(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Variant and quantity", required = true)
                                @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(user.id(), request);
    }

    @PatchMapping(path = "/items/{variantId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change item quantity", operationId = "updateCartItem")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated cart",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Item not in cart",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CartResponse updateItem(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                   @Parameter(description = "Variant id", required = true) @PathVariable UUID variantId,
                                   @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New quantity", required = true)
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(user.id(), variantId, request.quantity());
    }

    @DeleteMapping("/items/{variantId}")
    @Operation(summary = "Remove item", operationId = "removeCartItem")
    @ApiResponse(responseCode = "200", description = "Updated cart",
            content = @Content(schema = @Schema(implementation = CartResponse.class)))
    public CartResponse removeItem(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                   @Parameter(description = "Variant id", required = true) @PathVariable UUID variantId) {
        return cartService.removeItem(user.id(), variantId);
    }

    @DeleteMapping
    @Operation(summary = "Empty cart", operationId = "clearCart")
    @ApiResponse(responseCode = "204", description = "Cart emptied")
    public ResponseEntity<Void> clear(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        cartService.clear(user.id());
        return ResponseEntity.noContent().build();
    }
}
