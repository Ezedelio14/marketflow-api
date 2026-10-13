package com.marketflow.api.application.catalog;

import com.marketflow.api.application.catalog.dto.BrandResponse;
import com.marketflow.api.application.catalog.dto.CategoryResponse;
import com.marketflow.api.application.catalog.dto.CreateBrandRequest;
import com.marketflow.api.application.catalog.dto.CreateCategoryRequest;
import com.marketflow.api.application.common.SlugGenerator;
import com.marketflow.api.domain.catalog.Brand;
import com.marketflow.api.domain.catalog.BrandRepository;
import com.marketflow.api.domain.catalog.Category;
import com.marketflow.api.domain.catalog.CategoryRepository;
import com.marketflow.api.domain.catalog.CategoryStatus;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Categories and brands (catalog taxonomy), managed by administrators.
 */
@Service
@RequiredArgsConstructor
public class TaxonomyService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category parent = request.parentId() == null ? null : categoryRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.parentId()));
        String slug = resolveSlug(request.slug(), request.name(), categoryRepository::existsBySlug);
        return CategoryResponse.from(categoryRepository.save(Category.create(parent, request.name(), slug, request.description())));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        String slug = resolveSlug(request.slug(), request.name(), brandRepository::existsBySlug);
        return BrandResponse.from(brandRepository.save(Brand.create(request.name(), slug, request.description(), request.logoUrl())));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        return brandRepository.findAll(Sort.by("name")).stream().map(BrandResponse::from).toList();
    }

    private static String resolveSlug(String requested, String name, java.util.function.Predicate<String> exists) {
        if (requested == null || requested.isBlank()) {
            return SlugGenerator.unique(name, exists);
        }
        String slug = SlugGenerator.slugify(requested);
        if (exists.test(slug)) {
            throw new ConflictException("SLUG_IN_USE", "Slug already in use: " + slug);
        }
        return slug;
    }
}
