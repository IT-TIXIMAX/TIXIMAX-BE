package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

@Data
public class MonthlyStatsCustomer {
    private int month;
    private long newCustomers;

    public MonthlyStatsCustomer(int month) {
        this.month = month;
        this.newCustomers = 0;
    }
}
