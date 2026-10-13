package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.catalog.ProductService;
import com.marketflow.api.application.catalog.dto.ProductResponse;
import com.marketflow.api.application.catalog.search.ProductSearchCriteria;
import com.marketflow.api.application.catalog.search.ProductSearchHit;
import com.marketflow.api.application.catalog.search.ProductSearchPort;
import com.marketflow.api.application.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirements
@RequestMapping(path = "/api/v1/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Catalog", description = "Public storefront: full-text search (Elasticsearch) and product pages")
public class CatalogController {

    private final ProductSearchPort searchPort;
    private final ProductService productService;

    @GetMapping("/search")
    @Operation(
            summary = "Search products",
            description = """
                    Full-text search across name, brand, category, variant names/SKUs and description (fuzzy,
                    boosted). Dynamic filters: categories, brands, seller, price range (any variant within range),
                    variant attributes (e.g. `black`, `XL`) and stock. Sort by relevance, price or newest.
                    The index is updated asynchronously through the `product.indexed` Kafka topic.""",
            operationId = "searchProducts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of matching products"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<ProductSearchHit> search(@ParameterObject ProductSearchCriteria criteria) {
        return searchPort.search(criteria);
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Get a published product", description = "Product page data read from PostgreSQL (source of truth).",
            operationId = "getCatalogProduct")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "No published product with this slug",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse getBySlug(@Parameter(description = "Product slug", example = "phone-x1", required = true)
                                     @PathVariable String slug) {
        return productService.getPublishedBySlug(slug);
    }
}
