package com.marketflow.api.application.order;

import com.marketflow.api.application.common.Actor;
import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.order.dto.OrderResponse;
import com.marketflow.api.application.order.dto.OrderSummaryResponse;
import com.marketflow.api.application.order.dto.ShipOrderRequest;
import com.marketflow.api.application.payment.PaymentService;
import com.marketflow.api.application.seller.SellerService;
import com.marketflow.api.domain.common.exception.ForbiddenOperationException;
import com.marketflow.api.domain.common.exception.InvalidStateTransitionException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.OrderFilter;
import com.marketflow.api.domain.order.OrderRepository;
import com.marketflow.api.domain.order.OrderSpecifications;
import com.marketflow.api.domain.order.state.OrderAction;
import com.marketflow.api.domain.order.state.OrderStateMachine;
import com.marketflow.api.domain.shipment.Shipment;
import com.marketflow.api.domain.shipment.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Order queries and lifecycle commands. Every status change is delegated to {@link OrderStateMachine}.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderStateMachine stateMachine;
    private final OrderCancellationService cancellationService;
    private final PaymentService paymentService;
    private final SellerService sellerService;
    private final OrderMapper mapper;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;

    @Transactional(readOnly = true)
    public OrderResponse get(Actor actor, UUID orderId) {
        Order order = load(orderId);
        ensureCanView(actor, order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(Actor actor, OrderFilter filter, Pageable pageable) {
        OrderFilter scoped = switch (actor.role()) {
            case ADMIN -> filter;
            case SELLER -> filter.restrictToSeller(sellerService.requireSellerOf(actor).getId());
            case CUSTOMER -> filter.restrictToUser(actor.userId());
        };
        return PageResponse.from(orderRepository.findAll(OrderSpecifications.withFilter(scoped), pageable), mapper::toSummary);
    }

    @Transactional
    public OrderResponse cancel(Actor actor, UUID orderId, String reason) {
        Order order = load(orderId);
        ensureOwnerOrAdmin(actor, order);
        cancellationService.cancel(order, reason == null || reason.isBlank() ? "Cancelled by customer" : reason, actor.label(), true);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse requestRefund(Actor actor, UUID orderId, String reason) {
        Order order = load(orderId);
        ensureOwnerOrAdmin(actor, order);
        stateMachine.fire(order, OrderAction.REQUEST_REFUND, reason, actor.label());
        return toResponse(order);
    }

    /** Admin approval: issues the refund at the provider; the order becomes REFUNDED once it is confirmed. */
    @Transactional
    public OrderResponse approveRefund(Actor actor, UUID orderId) {
        Order order = load(orderId);
        if (!stateMachine.canFire(order, OrderAction.REFUND)) {
            throw new InvalidStateTransitionException(order.getStatus().name(), OrderAction.REFUND.name());
        }
        paymentService.refund(order, actor.label());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse startProcessing(Actor actor, UUID orderId) {
        Order order = load(orderId);
        ensureFulfiller(actor, order);
        stateMachine.fire(order, OrderAction.START_PROCESSING, "Seller started fulfilment", actor.label());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse ship(Actor actor, UUID orderId, ShipOrderRequest request) {
        Order order = load(orderId);
        ensureFulfiller(actor, order);
        stateMachine.fire(order, OrderAction.SHIP, "Shipped via " + request.carrier(), actor.label());
        Shipment shipment = shipmentRepository.save(Shipment.dispatch(order.getId(), request.carrier(), request.trackingNumber()));
        eventPublisher.publish(eventFactory.orderShipped(order, shipment));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse deliver(Actor actor, UUID orderId) {
        Order order = load(orderId);
        ensureFulfiller(actor, order);
        stateMachine.fire(order, OrderAction.DELIVER, "Delivered to customer", actor.label());
        shipmentRepository.findFirstByOrderIdOrderByShippedAtDesc(orderId).ifPresent(Shipment::markDelivered);
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        orderRepository.flush();
        return mapper.toResponse(order, stateMachine);
    }

    private Order load(UUID orderId) {
        return orderRepository.findWithItemsById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private void ensureCanView(Actor actor, Order order) {
        if (actor.isAdmin() || order.belongsTo(actor.userId())) return;
        if (actor.isSeller() && order.containsItemsOf(sellerService.requireSellerOf(actor).getId())) return;
        throw new ResourceNotFoundException("Order", order.getId()); // do not leak existence
    }

    private void ensureOwnerOrAdmin(Actor actor, Order order) {
        if (!actor.isAdmin() && !order.belongsTo(actor.userId())) {
            throw new ResourceNotFoundException("Order", order.getId());
        }
    }

    private void ensureFulfiller(Actor actor, Order order) {
        if (actor.isAdmin()) return;
        if (actor.isSeller() && order.containsItemsOf(sellerService.requireSellerOf(actor).getId())) return;
        throw new ForbiddenOperationException("Only the seller of this order or an admin can fulfil it");
    }
}
