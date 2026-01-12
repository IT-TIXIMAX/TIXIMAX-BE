package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter

public class OrdersPendingShipment {
    private BigDecimal finalPriceOrder;
    private String orderCode;
    private Long orderId;
    private String staffName;
    private List<OrderLinkPending> pendingLinks;
    private Customer customer;
    private String note;

    public OrdersPendingShipment(Orders orders, List<OrderLinkPending> links) {
        this.finalPriceOrder = orders.getFinalPriceOrder();
        this.orderCode = orders.getOrderCode();
        this.orderId = orders.getOrderId();
        this.staffName = orders.getStaff().getName();
        this.pendingLinks = links;
        this.customer = orders.getCustomer();
        this.note = orders.getNote();
    }
}
