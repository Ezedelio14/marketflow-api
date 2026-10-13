package com.marketflow.api.application.catalog;

import com.marketflow.api.application.catalog.dto.ProductImageResponse;
import com.marketflow.api.application.catalog.dto.ProductResponse;
import com.marketflow.api.application.catalog.dto.ProductSummaryResponse;
import com.marketflow.api.application.catalog.dto.ProductVariantResponse;
import com.marketflow.api.domain.catalog.Product;
import com.marketflow.api.domain.catalog.ProductImage;
import com.marketflow.api.domain.catalog.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ProductMapper {

    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerName", source = "seller.businessName")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    ProductResponse toResponse(Product product);

    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "brandId", source = "brand.id")
    ProductSummaryResponse toSummary(Product product);

    ProductVariantResponse toResponse(ProductVariant variant);

    ProductImageResponse toResponse(ProductImage image);
}
