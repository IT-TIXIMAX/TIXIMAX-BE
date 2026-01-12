package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyStatsOrder {
    private int month;
    private long totalOrders;
    private long totalLinks;

    public MonthlyStatsOrder(int month) {
        this.month = month;
        this.totalOrders = 0;
        this.totalLinks = 0;
    }
}