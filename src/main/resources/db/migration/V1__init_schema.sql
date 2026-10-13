-- MarketFlow e-commerce: initial schema. Every table uses UUID primary keys.
-- Enumerations are stored as VARCHAR (mapped with @Enumerated(STRING)) to keep migrations simple.

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE sellers (
    id                  UUID PRIMARY KEY,
    user_id             UUID         NOT NULL UNIQUE REFERENCES users (id),
    business_name       VARCHAR(255) NOT NULL,
    legal_name          VARCHAR(255) NOT NULL,
    tax_number          VARCHAR(32)  NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(32),
    status              VARCHAR(20)  NOT NULL,
    verification_status VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL
);

CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    parent_id   UUID REFERENCES categories (id),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    status      VARCHAR(20)  NOT NULL
);

CREATE TABLE brands (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    logo_url    VARCHAR(255)
);

CREATE TABLE products (
    id          UUID PRIMARY KEY,
    seller_id   UUID         NOT NULL REFERENCES sellers (id),
    category_id UUID         NOT NULL REFERENCES categories (id),
    brand_id    UUID REFERENCES brands (id),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_products_seller ON products (seller_id);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_status ON products (status);

CREATE TABLE product_variants (
    id               UUID PRIMARY KEY,
    product_id       UUID           NOT NULL REFERENCES products (id),
    sku              VARCHAR(64)    NOT NULL,
    name             VARCHAR(255)   NOT NULL,
    price            NUMERIC(19, 2) NOT NULL CHECK (price > 0),
    compare_at_price NUMERIC(19, 2),
    weight           NUMERIC(10, 3),
    status           VARCHAR(20)    NOT NULL,
    CONSTRAINT uk_product_variants_sku UNIQUE (sku)
);
CREATE INDEX idx_product_variants_product ON product_variants (product_id);

CREATE TABLE product_images (
    id          UUID PRIMARY KEY,
    product_id  UUID          NOT NULL REFERENCES products (id),
    url         VARCHAR(1024) NOT NULL,
    storage_key VARCHAR(255),
    position    INTEGER       NOT NULL,
    alt_text    VARCHAR(255)
);

CREATE TABLE inventories (
    id                 UUID PRIMARY KEY,
    product_variant_id UUID        NOT NULL REFERENCES product_variants (id),
    quantity           INTEGER     NOT NULL CHECK (quantity >= 0),
    reserved_quantity  INTEGER     NOT NULL CHECK (reserved_quantity >= 0),
    available_quantity INTEGER     NOT NULL CHECK (available_quantity >= 0),
    version            BIGINT      NOT NULL DEFAULT 0,
    updated_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inventories_variant UNIQUE (product_variant_id),
    CONSTRAINT ck_inventories_balance CHECK (available_quantity = quantity - reserved_quantity)
);

CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    user_id          UUID           NOT NULL REFERENCES users (id),
    order_number     VARCHAR(32)    NOT NULL,
    status           VARCHAR(30)    NOT NULL,
    subtotal         NUMERIC(19, 2) NOT NULL,
    discount         NUMERIC(19, 2) NOT NULL,
    shipping_cost    NUMERIC(19, 2) NOT NULL,
    tax              NUMERIC(19, 2) NOT NULL,
    total            NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    coupon_code      VARCHAR(50),
    shipping_address JSONB          NOT NULL,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_orders_order_number UNIQUE (order_number)
);
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id                 UUID PRIMARY KEY,
    order_id           UUID           NOT NULL REFERENCES orders (id),
    product_variant_id UUID           NOT NULL REFERENCES product_variants (id),
    seller_id          UUID           NOT NULL REFERENCES sellers (id),
    product_name       VARCHAR(255)   NOT NULL,
    sku                VARCHAR(64)    NOT NULL,
    quantity           INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price         NUMERIC(19, 2) NOT NULL,
    subtotal           NUMERIC(19, 2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_seller ON order_items (seller_id);

CREATE TABLE order_status_history (
    id          UUID PRIMARY KEY,
    order_id    UUID        NOT NULL REFERENCES orders (id),
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    reason      VARCHAR(255),
    changed_by  VARCHAR(100),
    changed_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_status_history_order ON order_status_history (order_id);

CREATE TABLE inventory_reservations (
    id           UUID PRIMARY KEY,
    inventory_id UUID        NOT NULL REFERENCES inventories (id),
    order_id     UUID        NOT NULL REFERENCES orders (id),
    quantity     INTEGER     NOT NULL CHECK (quantity > 0),
    status       VARCHAR(20) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_reservations_order ON inventory_reservations (order_id);
CREATE INDEX idx_reservations_status_expires ON inventory_reservations (status, expires_at);

CREATE TABLE carts (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
-- At most one active cart per user
CREATE UNIQUE INDEX uk_carts_active_user ON carts (user_id) WHERE status = 'ACTIVE';

CREATE TABLE cart_items (
    id                 UUID PRIMARY KEY,
    cart_id            UUID           NOT NULL REFERENCES carts (id),
    product_variant_id UUID           NOT NULL REFERENCES product_variants (id),
    quantity           INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price         NUMERIC(19, 2) NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_cart_items_variant UNIQUE (cart_id, product_variant_id)
);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    order_id            UUID           NOT NULL REFERENCES orders (id),
    provider            VARCHAR(30)    NOT NULL,
    provider_payment_id VARCHAR(100),
    amount              NUMERIC(19, 2) NOT NULL,
    currency            VARCHAR(3)     NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    failure_reason      VARCHAR(255),
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_payments_order ON payments (order_id);
CREATE UNIQUE INDEX idx_payments_provider_payment ON payments (provider_payment_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    provider     VARCHAR(30)  NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE shipments (
    id              UUID PRIMARY KEY,
    order_id        UUID         NOT NULL REFERENCES orders (id),
    carrier         VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    shipped_at      TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ
);
CREATE INDEX idx_shipments_order ON shipments (order_id);

CREATE TABLE reviews (
    id            UUID PRIMARY KEY,
    product_id    UUID         NOT NULL REFERENCES products (id),
    user_id       UUID         NOT NULL REFERENCES users (id),
    order_item_id UUID         NOT NULL UNIQUE REFERENCES order_items (id),
    rating        INTEGER      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title         VARCHAR(255),
    comment       TEXT,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_reviews_product ON reviews (product_id);

CREATE TABLE wishlists (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL UNIQUE REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE wishlist_items (
    id          UUID PRIMARY KEY,
    wishlist_id UUID        NOT NULL REFERENCES wishlists (id),
    product_id  UUID        NOT NULL REFERENCES products (id),
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_wishlist_items_product UNIQUE (wishlist_id, product_id)
);

CREATE TABLE coupons (
    id               UUID PRIMARY KEY,
    code             VARCHAR(50)    NOT NULL UNIQUE,
    type             VARCHAR(20)    NOT NULL,
    value            NUMERIC(19, 2) NOT NULL,
    minimum_amount   NUMERIC(19, 2),
    maximum_discount NUMERIC(19, 2),
    usage_limit      INTEGER,
    used_count       INTEGER        NOT NULL DEFAULT 0,
    starts_at        TIMESTAMPTZ    NOT NULL,
    expires_at       TIMESTAMPTZ,
    status           VARCHAR(20)    NOT NULL
);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id),
    type       VARCHAR(30)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
