package com.marketflow.api.presentation.rest;

import com.marketflow.api.application.catalog.TaxonomyService;
import com.marketflow.api.application.catalog.dto.BrandResponse;
import com.marketflow.api.application.catalog.dto.CategoryResponse;
import com.marketflow.api.application.catalog.dto.CreateBrandRequest;
import com.marketflow.api.application.catalog.dto.CreateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Taxonomy", description = "Categories and brands")
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    @GetMapping("/categories")
    @SecurityRequirements
    @Operation(summary = "List active categories", operationId = "listCategories")
    @ApiResponse(responseCode = "200", description = "Categories ordered by name",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    public List<CategoryResponse> categories() {
        return taxonomyService.listActiveCategories();
    }

    @PostMapping(path = "/categories", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category (admin)", operationId = "createCategory")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Parent category not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Slug already in use",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CategoryResponse createCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category data", required = true)
            @Valid @RequestBody CreateCategoryRequest request) {
        return taxonomyService.createCategory(request);
    }

    @GetMapping("/brands")
    @SecurityRequirements
    @Operation(summary = "List brands", operationId = "listBrands")
    @ApiResponse(responseCode = "200", description = "Brands ordered by name",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BrandResponse.class))))
    public List<BrandResponse> brands() {
        return taxonomyService.listBrands();
    }

    @PostMapping(path = "/brands", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create brand (admin)", operationId = "createBrand")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Brand created",
                    content = @Content(schema = @Schema(implementation = BrandResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Slug already in use",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public BrandResponse createBrand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Brand data", required = true)
            @Valid @RequestBody CreateBrandRequest request) {
        return taxonomyService.createBrand(request);
    }
}
