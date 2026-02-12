package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data

public class FirstTimeCustomer {
    private Long customerId;
    private LocalDateTime firstPurchaseDate;
    private String customerName;
    private Long orderCount;
    private Long staffId;
    private BigDecimal totalWeightPurchasedKg;
    private String serviceType;
    private String staffName;

    public FirstTimeCustomer(Long customerId,
                            LocalDateTime firstPurchaseDate,
                            String customerName,
                            Long orderCount,
                             Long staffId,
                            BigDecimal totalWeightPurchasedKg,
                            String serviceType,
                            String staffName) {
        this.customerId = customerId;
        this.firstPurchaseDate = firstPurchaseDate;
        this.customerName = customerName;
        this.orderCount = orderCount;
        this.staffId = staffId;
        this.totalWeightPurchasedKg = totalWeightPurchasedKg;
        this.serviceType = serviceType;
        this.staffName = staffName;
    }
}
