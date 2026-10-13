package com.marketflow.api.application.cart;

import com.marketflow.api.application.cart.dto.AddCartItemRequest;
import com.marketflow.api.application.cart.dto.CartItemResponse;
import com.marketflow.api.application.cart.dto.CartResponse;
import com.marketflow.api.domain.cart.Cart;
import com.marketflow.api.domain.cart.CartRepository;
import com.marketflow.api.domain.cart.CartStatus;
import com.marketflow.api.domain.catalog.ProductVariant;
import com.marketflow.api.domain.catalog.ProductVariantRepository;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.InsufficientStockException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cart use cases with a write-through Redis cache: reads hit Redis first, every mutation updates PostgreSQL
 * and then refreshes the cached snapshot.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final CartCache cartCache;

    @Transactional
    public CartResponse getCart(UUID userId) {
        return cartCache.get(userId).orElseGet(() -> cacheAndReturn(activeCart(userId)));
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        ProductVariant variant = variantRepository.findAllWithProductByIdIn(java.util.List.of(request.productVariantId()))
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.productVariantId()));
        if (!variant.isPurchasable()) {
            throw new BusinessRuleException("VARIANT_NOT_AVAILABLE", "Variant is not available for sale");
        }
        Cart cart = activeCart(userId);
        int alreadyInCart = cart.getItems().stream()
                .filter(i -> i.getProductVariantId().equals(variant.getId()))
                .mapToInt(com.marketflow.api.domain.cart.CartItem::getQuantity).sum();
        // Soft check only: the hard guarantee happens when stock is reserved at checkout.
        int available = inventoryRepository.findByVariant_Id(variant.getId()).map(i -> i.getAvailableQuantity()).orElse(0);
        int wanted = alreadyInCart + request.quantity();
        if (wanted > available) {
            throw new InsufficientStockException(variant.getId(), wanted, available);
        }
        cart.addItem(variant.getId(), request.quantity(), variant.getPrice());
        return cacheAndReturn(cart);
    }

    @Transactional
    public CartResponse updateItem(UUID userId, UUID variantId, int quantity) {
        Cart cart = activeCart(userId);
        cart.updateQuantity(variantId, quantity);
        return cacheAndReturn(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID variantId) {
        Cart cart = activeCart(userId);
        cart.removeItem(variantId);
        return cacheAndReturn(cart);
    }

    @Transactional
    public void clear(UUID userId) {
        activeCart(userId).clear();
        cartCache.evict(userId);
    }

    /** Loads the active cart entity (creating it lazily). Used by the checkout facade. */
    @Transactional
    public Cart activeCart(UUID userId) {
        return cartRepository.findFirstByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(Cart.openFor(userId)));
    }

    /** Checkout compensation: gives the customer their cart back when no newer active cart exists. */
    @Transactional
    public void reopenLastCheckedOut(UUID userId) {
        if (cartRepository.findFirstByUserIdAndStatus(userId, CartStatus.ACTIVE).isPresent()) {
            return;
        }
        cartRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, CartStatus.CHECKED_OUT).ifPresent(Cart::reopen);
        cartCache.evict(userId);
    }

    public void evict(UUID userId) {
        cartCache.evict(userId);
    }

    private CartResponse cacheAndReturn(Cart cart) {
        cartRepository.flush();
        CartResponse response = toResponse(cart);
        cartCache.put(response);
        return response;
    }

    private CartResponse toResponse(Cart cart) {
        Map<UUID, ProductVariant> variants = variantRepository.findAllWithProductByIdIn(
                        cart.getItems().stream().map(i -> i.getProductVariantId()).toList())
                .stream().collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        var items = cart.getItems().stream().map(i -> {
            ProductVariant v = variants.get(i.getProductVariantId());
            return new CartItemResponse(i.getProductVariantId(), v.getProduct().getId(), v.getProduct().getName(),
                    v.getName(), v.getSku(), i.getQuantity(), i.getUnitPrice(), i.lineTotal());
        }).toList();
        int count = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(cart.getId(), cart.getUserId(), items, count, cart.subtotal(), cart.getUpdatedAt());
    }
}
