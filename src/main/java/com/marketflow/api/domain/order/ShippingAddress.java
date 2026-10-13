package com.marketflow.api.domain.order;

/**
 * Value object persisted as JSONB in {@code orders.shipping_address}. Immutable snapshot taken at checkout.
 */
public record ShippingAddress(
        String recipientName,
        String phone,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String postalCode,
        String country
) {
}
