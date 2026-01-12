package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaleStats {

    private Long saleId;

    private String saleName;

    private long totalOrders;

    private BigDecimal totalRevenue;

    private long uniqueCustomers;

    private long newCustomers;

    private BigDecimal averageOrderValue;

}