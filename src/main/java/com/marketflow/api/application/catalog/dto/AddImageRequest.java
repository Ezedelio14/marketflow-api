package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Image already uploaded to object storage")
public record AddImageRequest(
        @Schema(description = "Public URL", example = "https://cdn.marketflow.dev/p/phone-x1/front.jpg") @NotBlank @URL @Size(max = 1024) String url,
        @Schema(description = "Object storage key", example = "products/phone-x1/front.jpg") @Size(max = 255) String storageKey,
        @Schema(description = "Alternative text for accessibility", example = "Phone X1 front view") @Size(max = 255) String altText
) {
}
