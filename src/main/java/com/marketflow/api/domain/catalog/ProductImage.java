package com.marketflow.api.domain.catalog;

import com.marketflow.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "alt_text")
    private String altText;

    static ProductImage create(Product product, String url, String storageKey, int position, String altText) {
        ProductImage image = new ProductImage();
        image.product = product;
        image.url = url;
        image.storageKey = storageKey;
        image.position = position;
        image.altText = altText;
        return image;
    }
}
