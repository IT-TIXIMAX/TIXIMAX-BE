package com.tiximax.txm.Model.DTOResponse.Customer;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class CustomerInactive {
    private Long accountId;
    private String customerCode;
    private String customerName;
    private String phone;
    private String email;
    private Integer totalOrders;
    private Double totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal balance;
    private LocalDateTime lastOrderDate;
    private Long daysSinceLastOrder;

    public CustomerInactive(
            Long accountId,
            String customerCode,
            String customerName,
            String phone,
            String email,
            Integer totalOrders,
            Double totalWeight,
            BigDecimal totalAmount,
            BigDecimal balance,
            LocalDateTime lastOrderDate,
            Long daysSinceLastOrder) {
        this.accountId = accountId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.totalOrders = totalOrders;
        this.totalWeight = totalWeight;
        this.totalAmount = totalAmount;
        this.balance = balance;
        this.lastOrderDate = lastOrderDate;
        this.daysSinceLastOrder = daysSinceLastOrder;
    }
}
