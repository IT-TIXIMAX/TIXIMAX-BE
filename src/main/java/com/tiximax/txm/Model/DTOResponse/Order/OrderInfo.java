package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Enums.OrderStatus;
import com.tiximax.txm.Enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@AllArgsConstructor@Data
@Getter
@Setter

public class OrderInfo {
    private Long orderId;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus status;
    private String customerCode;
    private String customerName;
    private String staffName;
    private BigDecimal exchangeRate;
    private BigDecimal finalPriceOrder;
    private LocalDateTime createdAt;
}
