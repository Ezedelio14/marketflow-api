package com.marketflow.api.application.inventory.dto;

import com.marketflow.api.domain.inventory.Inventory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Stock snapshot of a variant")
public record InventoryResponse(
        @Schema(description = "Inventory id") UUID id,
        @Schema(description = "Variant id") UUID productVariantId,
        @Schema(description = "SKU", example = "PHN-X1-128-BLK") String sku,
        @Schema(description = "Units on hand", example = "250") int quantity,
        @Schema(description = "Units held by pending orders", example = "12") int reservedQuantity,
        @Schema(description = "Units that can still be sold", example = "238") int availableQuantity,
        @Schema(description = "Optimistic-locking version", example = "17") Long version,
        @Schema(description = "Last change instant") Instant updatedAt
) {
    public static InventoryResponse from(Inventory i) {
        return new InventoryResponse(i.getId(), i.getVariant().getId(), i.getVariant().getSku(), i.getQuantity(),
                i.getReservedQuantity(), i.getAvailableQuantity(), i.getVersion(), i.getUpdatedAt());
    }
}
