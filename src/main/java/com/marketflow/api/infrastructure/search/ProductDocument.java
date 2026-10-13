package com.marketflow.api.infrastructure.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.util.List;

/**
 * Denormalised read model of a published product, optimised for storefront search.
 * The index is created on startup by {@link SearchIndexInitializer} (not by the repository) so the API boots
 * even when Elasticsearch is temporarily unavailable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products", createIndex = false)
public class ProductDocument {

    @Id
    private String id;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Text)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String brandId;

    @Field(type = FieldType.Text)
    private String brandName;

    @Field(type = FieldType.Keyword)
    private String sellerId;

    @Field(type = FieldType.Text)
    private String sellerName;

    @Field(type = FieldType.Double)
    private Double minPrice;

    @Field(type = FieldType.Double)
    private Double maxPrice;

    @Field(type = FieldType.Boolean)
    private boolean inStock;

    /** Variant names and SKUs flattened for full-text relevance. */
    @Field(type = FieldType.Text)
    private String variantText;

    @Field(type = FieldType.Nested)
    private List<VariantDocument> variants;

    @Field(type = FieldType.Keyword, index = false)
    private String imageUrl;

    @Field(type = FieldType.Long)
    private Long createdAtEpochMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantDocument {

        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Keyword)
        private String sku;

        @MultiField(mainField = @Field(type = FieldType.Text),
                otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
        private String name;

        @Field(type = FieldType.Double)
        private Double price;

        @Field(type = FieldType.Integer)
        private int available;
    }
}
