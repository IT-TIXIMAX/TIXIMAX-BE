package com.tiximax.txm.Model.DTOResponse.Order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetail {

    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private Boolean checkRequired;
    private Set<OrderLinks> orderLinks;
    private Customer customer;
    private Staff staff;
    private Set<Warehouse> warehouses;
    private Set<Payment> payments;
    private Set<Purchases> purchases;
    private Set<OrderProcessLog> orderProcessLogs;
    private Set<ShipmentTracking> shipmentTrackings;
    private Route route;
    private Destination destination;
    private Feedback feedback;

    public OrderDetail(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.checkRequired = order.getCheckRequired();
        this.orderLinks = order.getOrderLinks();
        this.customer = order.getCustomer();
        this.staff = order.getStaff();
        this.warehouses = order.getOrderLinks().stream()
                .map(OrderLinks::getWarehouse)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.payments = order.getPayments();
        this.orderProcessLogs = order.getOrderProcessLogs();
        this.shipmentTrackings = order.getShipmentTrackings();
        this.route = order.getRoute();
        this.destination = order.getDestination();
        this.feedback = order.getFeedback();
        this.purchases = order.getPurchases().stream()
                .map(p -> {
                    Purchases copy = new Purchases();
                    copy.setPurchaseId(p.getPurchaseId());
                    copy.setPurchaseCode(p.getPurchaseCode());
                    copy.setPurchaseImage(p.getPurchaseImage());
                    copy.setPurchaseTime(p.getPurchaseTime());
                    copy.setFinalPriceOrder(p.getFinalPriceOrder());
                    copy.setNote(p.getNote());
                    copy.setOrderLinks(p.getOrderLinks());
                    return copy;
                })
                .collect(Collectors.toSet());
    }
}