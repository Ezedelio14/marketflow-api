package com.marketflow.api.application.checkout;

import com.marketflow.api.application.cart.CartService;
import com.marketflow.api.application.checkout.dto.CheckoutLineResponse;
import com.marketflow.api.application.checkout.dto.CheckoutQuoteResponse;
import com.marketflow.api.application.checkout.dto.CheckoutRequest;
import com.marketflow.api.application.checkout.dto.CheckoutResponse;
import com.marketflow.api.application.common.Actor;
import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.inventory.InventoryService;
import com.marketflow.api.application.inventory.LockStrategy;
import com.marketflow.api.application.order.OrderCancellationService;
import com.marketflow.api.application.order.OrderMapper;
import com.marketflow.api.application.payment.PaymentGateway;
import com.marketflow.api.application.payment.PaymentGateway.PaymentIntentCommand;
import com.marketflow.api.application.payment.PaymentGateway.PaymentIntentResult;
import com.marketflow.api.domain.cart.Cart;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.PaymentProviderException;
import com.marketflow.api.domain.coupon.CouponRepository;
import com.marketflow.api.domain.inventory.InventoryReservation;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.OrderItem;
import com.marketflow.api.domain.order.OrderRepository;
import com.marketflow.api.domain.order.ShippingAddress;
import com.marketflow.api.domain.payment.Payment;
import com.marketflow.api.domain.payment.PaymentRepository;
import com.marketflow.api.config.MarketflowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Facade pattern: the single entry point of the checkout use case. It orchestrates
 * <ol>
 *   <li><b>validation &amp; pricing</b> of the cart ({@link CheckoutPricing}: Strategy for shipping and coupons),</li>
 *   <li><b>order issuing</b> through {@link Order.Builder} (Builder pattern) in {@code PENDING_PAYMENT},</li>
 *   <li><b>stock reservation</b> with TTL — optimistic first, falling back to {@code PESSIMISTIC_WRITE} under
 *       contention,</li>
 *   <li><b>payment initialisation</b> at the provider (Stripe PaymentIntent) <i>outside</i> the DB transaction,
 *       so row locks are never held during a network call,</li>
 *   <li><b>compensation</b> (cancel order, release stock, reopen cart) when the provider call fails.</li>
 * </ol>
 */
@Slf4j
@Service
public class CheckoutFacade {

    private final CartService cartService;
    private final CheckoutPricing pricing;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final OrderCancellationService cancellationService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderMapper orderMapper;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;
    private final MarketflowProperties properties;
    private final Clock clock;
    private final TransactionTemplate tx;

    public CheckoutFacade(CartService cartService, CheckoutPricing pricing, InventoryService inventoryService,
                          OrderRepository orderRepository, PaymentRepository paymentRepository,
                          CouponRepository couponRepository, OrderCancellationService cancellationService,
                          OrderNumberGenerator orderNumberGenerator, OrderMapper orderMapper,
                          PaymentGateway paymentGateway, EventPublisher eventPublisher,
                          DomainEventFactory eventFactory, MarketflowProperties properties, Clock clock,
                          PlatformTransactionManager transactionManager) {
        this.cartService = cartService;
        this.pricing = pricing;
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.cancellationService = cancellationService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.orderMapper = orderMapper;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
        this.properties = properties;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** Side-effect free price preview. */
    public CheckoutQuoteResponse quote(Actor actor, CheckoutRequest request) {
        TransactionTemplate readOnly = new TransactionTemplate(tx.getTransactionManager());
        readOnly.setReadOnly(true);
        return readOnly.execute(status -> {
            Cart cart = cartService.activeCart(actor.userId());
            CheckoutQuote quote = pricing.price(cart, request.shippingMethod(), toAddress(request), request.couponCode());
            return toQuoteResponse(quote);
        });
    }

    public CheckoutResponse checkout(Actor actor, CheckoutRequest request) {
        PlacedOrder placed = placeOrderWithLockFallback(actor, request);

        PaymentIntentResult intent;
        try {
            intent = paymentGateway.createPaymentIntent(new PaymentIntentCommand(
                    placed.orderId(), placed.orderNumber(), actor.userId(), actor.email(), placed.quote().total(),
                    placed.quote().currency(), "checkout-" + placed.orderId(),
                    Map.of("order_id", placed.orderId().toString(),
                            "order_number", placed.orderNumber(),
                            "user_id", actor.userId().toString())));
        } catch (RuntimeException e) {
            compensate(actor, placed.orderId());
            throw e instanceof PaymentProviderException ppe ? ppe
                    : new PaymentProviderException("Could not initialise payment", e);
        }

        Payment payment = tx.execute(status -> paymentRepository.save(Payment.initiate(placed.orderId(),
                paymentGateway.providerName(), intent.providerPaymentId(), placed.quote().total(), placed.quote().currency())));

        cartService.evict(actor.userId());
        log.info("Checkout completed: order {} total {} {}", placed.orderNumber(), placed.quote().total(), placed.quote().currency());

        CheckoutQuote q = placed.quote();
        return new CheckoutResponse(placed.orderId(), placed.orderNumber(), placed.status(), q.subtotal(), q.discount(),
                q.shippingCost(), q.tax(), q.total(), q.currency(),
                new CheckoutResponse.PaymentInfo(payment.getId(), payment.getProvider(), intent.providerPaymentId(),
                        intent.clientSecret(), intent.status()),
                placed.reservationExpiresAt());
    }

    /**
     * Runs the transactional part with optimistic locking first; when concurrent checkouts keep colliding on the
     * same inventory rows it retries with {@code PESSIMISTIC_WRITE}, serialising the hot rows.
     */
    private PlacedOrder placeOrderWithLockFallback(Actor actor, CheckoutRequest request) {
        MarketflowProperties.Checkout cfg = properties.checkout();
        for (int attempt = 1; ; attempt++) {
            LockStrategy strategy = attempt <= cfg.optimisticAttempts() ? LockStrategy.OPTIMISTIC : LockStrategy.PESSIMISTIC;
            try {
                return tx.execute(status -> placeOrder(actor, request, strategy));
            } catch (ObjectOptimisticLockingFailureException | PessimisticLockingFailureException e) {
                if (attempt >= cfg.maxAttempts()) {
                    throw new ConflictException("CHECKOUT_CONTENTION", "Stock is under heavy contention, please retry");
                }
                log.warn("Checkout attempt {} with {} locking failed ({}), retrying", attempt, strategy, e.getClass().getSimpleName());
            }
        }
    }

    private PlacedOrder placeOrder(Actor actor, CheckoutRequest request, LockStrategy strategy) {
        Cart cart = cartService.activeCart(actor.userId());
        ShippingAddress address = toAddress(request);
        CheckoutQuote quote = pricing.price(cart, request.shippingMethod(), address, request.couponCode());

        if (quote.coupon() != null && couponRepository.incrementUsage(quote.coupon().getId()) == 0) {
            throw new BusinessRuleException("COUPON_EXHAUSTED", "Coupon usage limit reached");
        }

        Order.Builder builder = Order.builder()
                .userId(actor.userId())
                .orderNumber(orderNumberGenerator.next())
                .currency(quote.currency())
                .shippingAddress(address)
                .couponCode(quote.coupon() == null ? null : quote.coupon().getCode())
                .discount(quote.discount())
                .shippingCost(quote.shippingCost())
                .tax(quote.tax());
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutQuote.Line line : quote.lines()) {
            builder.item(OrderItem.of(line.variant().getId(), line.variant().getProduct().getSeller().getId(),
                    line.variant().getProduct().getName(), line.variant().getSku(), line.quantity(), line.unitPrice()));
            quantities.merge(line.variant().getId(), line.quantity(), Integer::sum);
        }
        Order order = orderRepository.save(builder.build());

        Instant expiresAt = Instant.now(clock).plus(properties.checkout().reservationTtl());
        List<InventoryReservation> reservations = inventoryService.reserve(order.getId(), quantities, expiresAt, strategy);

        cart.checkout();

        eventPublisher.publish(eventFactory.orderCreated(order));
        eventPublisher.publish(eventFactory.inventoryReserved(order.getId(), reservations, expiresAt));
        return new PlacedOrder(order.getId(), order.getOrderNumber(), order.getStatus(), quote, expiresAt);
    }

    private void compensate(Actor actor, UUID orderId) {
        try {
            tx.executeWithoutResult(status -> {
                Order order = orderRepository.findWithItemsById(orderId).orElseThrow();
                cancellationService.cancel(order, "Payment initialisation failed", "checkout", false);
                cartService.reopenLastCheckedOut(actor.userId());
            });
        } catch (RuntimeException ex) {
            log.error("Compensation failed for order {} — the reservation TTL job will clean it up", orderId, ex);
        }
    }

    private ShippingAddress toAddress(CheckoutRequest request) {
        return orderMapper.toDomain(request.shippingAddress());
    }

    private static CheckoutQuoteResponse toQuoteResponse(CheckoutQuote q) {
        List<CheckoutLineResponse> lines = q.lines().stream()
                .map(l -> new CheckoutLineResponse(l.variant().getId(), l.variant().getProduct().getName(),
                        l.variant().getSku(), l.quantity(), l.unitPrice(), l.lineTotal()))
                .toList();
        return new CheckoutQuoteResponse(lines, q.subtotal(), q.discount(), q.shippingCost(), q.tax(), q.total(),
                q.currency(), q.shippingMethod(), q.coupon() == null ? null : q.coupon().getCode());
    }

    private record PlacedOrder(UUID orderId, String orderNumber, com.marketflow.api.domain.order.OrderStatus status,
                               CheckoutQuote quote, Instant reservationExpiresAt) {
    }
}
