package com.marketflow.api.application.checkout;

import com.marketflow.api.application.pricing.TaxCalculator;
import com.marketflow.api.application.pricing.discount.DiscountCalculator;
import com.marketflow.api.application.pricing.shipping.ShippingCalculator;
import com.marketflow.api.application.pricing.shipping.ShippingMethod;
import com.marketflow.api.application.pricing.shipping.ShippingQuoteContext;
import com.marketflow.api.domain.cart.Cart;
import com.marketflow.api.domain.cart.CartItem;
import com.marketflow.api.domain.catalog.ProductVariant;
import com.marketflow.api.domain.catalog.ProductVariantRepository;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponRepository;
import com.marketflow.api.domain.order.ShippingAddress;
import com.marketflow.api.config.MarketflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prices a cart with current catalog prices, coupon (Strategy), shipping (Strategy) and tax.
 */
@Component
@RequiredArgsConstructor
public class CheckoutPricing {

    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;
    private final DiscountCalculator discountCalculator;
    private final ShippingCalculator shippingCalculator;
    private final TaxCalculator taxCalculator;
    private final MarketflowProperties properties;
    private final Clock clock;

    public CheckoutQuote price(Cart cart, ShippingMethod method, ShippingAddress address, String couponCode) {
        if (cart.isEmpty()) {
            throw new BusinessRuleException("EMPTY_CART", "Cannot checkout an empty cart");
        }
        Map<UUID, ProductVariant> variants = variantRepository.findAllWithProductByIdIn(
                        cart.getItems().stream().map(CartItem::getProductVariantId).toList())
                .stream().collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        CheckoutQuote.CheckoutQuoteBuilder quote = CheckoutQuote.builder()
                .currency(properties.currency())
                .shippingMethod(method);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal weight = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            ProductVariant variant = variants.get(item.getProductVariantId());
            if (variant == null) {
                throw new ResourceNotFoundException("ProductVariant", item.getProductVariantId());
            }
            if (!variant.isPurchasable()) {
                throw new BusinessRuleException("VARIANT_NOT_AVAILABLE", "Variant %s is no longer available".formatted(variant.getSku()));
            }
            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            quote.line(new CheckoutQuote.Line(variant, item.getQuantity(), variant.getPrice(), lineTotal));
            subtotal = subtotal.add(lineTotal);
            weight = weight.add(variant.getWeight().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                    .orElseThrow(() -> new BusinessRuleException("COUPON_NOT_FOUND", "Coupon not found: " + couponCode));
            discount = discountCalculator.calculate(coupon, subtotal, Instant.now(clock));
            quote.coupon(coupon);
        }
        BigDecimal shipping = shippingCalculator.calculate(method, new ShippingQuoteContext(subtotal, weight, address));
        BigDecimal tax = taxCalculator.calculate(subtotal.subtract(discount));

        return quote
                .subtotal(subtotal)
                .totalWeight(weight)
                .discount(discount)
                .shippingCost(shipping)
                .tax(tax)
                .total(subtotal.subtract(discount).add(shipping).add(tax))
                .build();
    }
}
