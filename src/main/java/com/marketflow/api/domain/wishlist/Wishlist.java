package com.marketflow.api.domain.wishlist;

import com.marketflow.api.domain.common.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "wishlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wishlist extends AuditableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishlistItem> items = new ArrayList<>();

    public static Wishlist of(UUID userId) {
        Wishlist wishlist = new Wishlist();
        wishlist.userId = userId;
        return wishlist;
    }

    public void add(UUID productId) {
        if (items.stream().noneMatch(i -> i.getProductId().equals(productId))) {
            items.add(WishlistItem.of(this, productId));
        }
    }

    public void remove(UUID productId) {
        items.removeIf(i -> i.getProductId().equals(productId));
    }

    public List<WishlistItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
