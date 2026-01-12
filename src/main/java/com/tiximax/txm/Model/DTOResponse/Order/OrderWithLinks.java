package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

public class OrderWithLinks {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private Boolean checkRequired;
    private List<OrderLinks> orderLinks;
    private LocalDateTime pinnedAt;

    public OrderWithLinks(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.checkRequired = order.getCheckRequired();
        this.orderLinks = new ArrayList<>(order.getOrderLinks());
        this.pinnedAt = order.getPinnedAt();
    }

    public void setOrderLinks(List<OrderLinks> orderLinks) {
        this.orderLinks = orderLinks != null ? new ArrayList<>(orderLinks) : null;
    }
}