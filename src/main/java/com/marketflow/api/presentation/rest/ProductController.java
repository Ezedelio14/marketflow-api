package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.catalog.ProductService;
import com.marketflow.api.application.catalog.dto.AddImageRequest;
import com.marketflow.api.application.catalog.dto.CreateProductRequest;
import com.marketflow.api.application.catalog.dto.CreateVariantRequest;
import com.marketflow.api.application.catalog.dto.ProductResponse;
import com.marketflow.api.application.catalog.dto.ProductSummaryResponse;
import com.marketflow.api.application.catalog.dto.UpdateProductRequest;
import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.domain.catalog.ProductFilter;
import com.marketflow.api.domain.catalog.ProductStatus;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@RequestMapping(path = "/api/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Products", description = "Seller catalog management. Changes are pushed to Elasticsearch via `product.indexed`.")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product (DRAFT)", description = "Optionally creates variants with initial stock.",
            operationId = "createProduct")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a seller",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Category, brand or seller profile not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Duplicated SKU",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Seller not approved or category inactive",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse create(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Product data", required = true)
            @Valid @RequestBody CreateProductRequest request) {
        return productService.create(user.toActor(), request);
    }

    @GetMapping
    @Operation(summary = "List products (back-office)",
            description = "Filtered with JPA Specifications. Sellers only see their own products.",
            operationId = "listProducts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of products"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or sort",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not a seller or admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<ProductSummaryResponse> list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Text contained in name or description", example = "phone") @RequestParam(required = false) String q,
            @Parameter(description = "Lifecycle status") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Seller id (admin only)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Category id (includes direct sub-categories)") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Brand id") @RequestParam(required = false) UUID brandId,
            @Parameter(description = "Minimum variant price", example = "10.00") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum variant price", example = "999.99") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Exact SKU", example = "PHN-X1-128-BLK") @RequestParam(required = false) String sku,
            @ParameterObject @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ProductFilter filter = new ProductFilter(q, status, sellerId, categoryId, brandId, minPrice, maxPrice, sku);
        return productService.list(user.toActor(), filter, pageable);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product", operationId = "getProduct")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "403", description = "Product belongs to another seller",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                               @Parameter(description = "Product id", required = true) @PathVariable UUID productId) {
        return productService.get(user.toActor(), productId);
    }

    @PatchMapping(path = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update product details", operationId = "updateProduct")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "403", description = "Product belongs to another seller",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product, category or brand not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Product is archived",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                  @Parameter(description = "Product id", required = true) @PathVariable UUID productId,
                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to change", required = true)
                                  @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(user.toActor(), productId, request);
    }

    @PostMapping(path = "/{productId}/variants", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add variant", description = "Creates the variant and its inventory row.", operationId = "addVariant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Variant added",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Duplicated SKU",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse addVariant(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                      @Parameter(description = "Product id", required = true) @PathVariable UUID productId,
                                      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Variant data", required = true)
                                      @Valid @RequestBody CreateVariantRequest request) {
        return productService.addVariant(user.toActor(), productId, request);
    }

    @PostMapping(path = "/{productId}/images", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach image", description = "Registers an image already uploaded to object storage.",
            operationId = "addProductImage")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image attached",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse addImage(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                    @Parameter(description = "Product id", required = true) @PathVariable UUID productId,
                                    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Image data", required = true)
                                    @Valid @RequestBody AddImageRequest request) {
        return productService.addImage(user.toActor(), productId, request);
    }

    @PostMapping("/{productId}/publish")
    @Operation(summary = "Publish product", description = "`DRAFT → PUBLISHED`; requires at least one active variant.",
            operationId = "publishProduct")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product published",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "No active variant or product archived",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse publish(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                   @Parameter(description = "Product id", required = true) @PathVariable UUID productId) {
        return productService.publish(user.toActor(), productId);
    }

    @PostMapping("/{productId}/archive")
    @Operation(summary = "Archive product", description = "Removes it from the storefront and search index.",
            operationId = "archiveProduct")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product archived",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse archive(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
                                   @Parameter(description = "Product id", required = true) @PathVariable UUID productId) {
        return productService.archive(user.toActor(), productId);
    }
}
