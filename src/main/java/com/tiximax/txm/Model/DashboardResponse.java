package com.tiximax.txm.Model;

import java.math.BigDecimal;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
@Getter
@Setter
public class DashboardResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalPurchase;
    private BigDecimal totalShip; 
    private long newCustomers;
    private long totalLinks;
    private Double totalWeight;
    private Double totalNetWeight;
}
