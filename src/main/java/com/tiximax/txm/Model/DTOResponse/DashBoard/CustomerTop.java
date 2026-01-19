package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class CustomerTop {
    private Long accountId;
    private String customerCode;
    private String customerName;
    private String phone;
    private String email;
    private Integer totalOrders;
    private Double totalWeight;
    private BigDecimal totalAmount;
    private BigDecimal balance;

    public CustomerTop(
            Long accountId,
            String customerCode,
            String customerName,
            String phone,
            String email,
            Integer totalOrders,
            Double totalWeight,
            BigDecimal totalAmount,
            BigDecimal balance) {
        this.accountId = accountId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.totalOrders = totalOrders;
        this.totalWeight = totalWeight;
        this.totalAmount = totalAmount;
        this.balance = balance;
    }
}
