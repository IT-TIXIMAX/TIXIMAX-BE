package com.tiximax.txm.Model.DTOResponse.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderSummaryDTO {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private Boolean checkRequired;
    private LocalDateTime pinnedAt;
}
