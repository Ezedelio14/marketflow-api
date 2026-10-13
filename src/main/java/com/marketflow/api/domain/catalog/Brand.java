package com.marketflow.api.domain.catalog;

import com.marketflow.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "brands")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    public static Brand create(String name, String slug, String description, String logoUrl) {
        Brand brand = new Brand();
        brand.name = name;
        brand.slug = slug;
        brand.description = description;
        brand.logoUrl = logoUrl;
        return brand;
    }
}
