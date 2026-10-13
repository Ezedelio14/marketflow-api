package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.inventory.InventoryService;
import com.marketflow.api.application.inventory.dto.AdjustInventoryRequest;
import com.marketflow.api.application.inventory.dto.InventoryResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@RequestMapping(path = "/api/v1/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Inventory", description = "Stock levels protected by optimistic locking (@Version)")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Get variant stock", operationId = "getInventory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock snapshot",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "403", description = "Variant belongs to another seller",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InventoryResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                 @Parameter(description = "Variant id", required = true) @PathVariable UUID variantId) {
        return inventoryService.get(user.toActor(), variantId);
    }

    @PutMapping(path = "/variants/{variantId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set on-hand quantity",
            description = "Optimistic locking with automatic retry; never goes below the reserved units.",
            operationId = "adjustInventory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Variant belongs to another seller",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent modification persisted after retries",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Quantity lower than reserved units",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InventoryResponse adjust(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                    @Parameter(description = "Variant id", required = true) @PathVariable UUID variantId,
                                    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New quantity", required = true)
                                    @Valid @RequestBody AdjustInventoryRequest request) {
        return inventoryService.adjust(user.toActor(), variantId, request.quantity());
    }
}
