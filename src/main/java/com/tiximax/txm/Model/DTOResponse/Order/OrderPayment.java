package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.*;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class OrderPayment {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private BigDecimal PaymentAfterAuction;
    private String note;
    private Customer customer;
    private String paymentCode;
    private Long paymentId;
    private BigDecimal totalNetWeight;
    private BigDecimal leftoverMoney;
    private String QRCode;

    public OrderPayment(Orders order) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
        this.orderType = order.getOrderType();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.exchangeRate = order.getExchangeRate();
        this.PaymentAfterAuction = order.getPaymentAfterAuction();
        this.finalPriceOrder = order.getFinalPriceOrder();
        this.customer = order.getCustomer();
        this.paymentCode = null;
        this.totalNetWeight = order.getFinalPriceOrder();
        this.leftoverMoney = order.getLeftoverMoney();
    }

}