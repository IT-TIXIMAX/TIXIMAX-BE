package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter

public class RefundResponse {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private BigDecimal leftoverMoney;
    private String customerName;
    private String staffName;

    public RefundResponse(
            Long orderId,
            String orderCode,
            OrderType orderType,
            OrderStatus status,
            LocalDateTime createdAt,
            BigDecimal exchangeRate,
            BigDecimal finalPriceOrder,
            BigDecimal leftoverMoney,
            String customerName,
            String staffName) {

        this.orderId = orderId;
        this.orderCode = orderCode;
        this.orderType = orderType;
        this.status = status;
        this.createdAt = createdAt;
        this.exchangeRate = exchangeRate;
        this.finalPriceOrder = finalPriceOrder;
        this.leftoverMoney = leftoverMoney;
        this.customerName = customerName;
        this.staffName = staffName;
    }
}
