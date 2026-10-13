package com.marketflow.api.application.order;

import com.marketflow.api.application.order.dto.OrderItemResponse;
import com.marketflow.api.application.order.dto.OrderResponse;
import com.marketflow.api.application.order.dto.OrderStatusHistoryResponse;
import com.marketflow.api.application.order.dto.OrderSummaryResponse;
import com.marketflow.api.application.order.dto.ShippingAddressDto;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.OrderItem;
import com.marketflow.api.domain.order.OrderStatusHistory;
import com.marketflow.api.domain.order.ShippingAddress;
import com.marketflow.api.domain.order.state.OrderStateMachine;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OrderMapper {

    @Mapping(target = "allowedActions", expression = "java(stateMachine.allowedActions(order))")
    OrderResponse toResponse(Order order, @Context OrderStateMachine stateMachine);

    OrderSummaryResponse toSummary(Order order);

    OrderItemResponse toResponse(OrderItem item);

    OrderStatusHistoryResponse toResponse(OrderStatusHistory history);

    ShippingAddressDto toDto(ShippingAddress address);

    ShippingAddress toDomain(ShippingAddressDto dto);
}
