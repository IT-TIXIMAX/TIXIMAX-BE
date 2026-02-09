package com.tiximax.txm.Model.DTOResponse.Payment;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data

public class DailyPaymentRevenue {
    private LocalDate date;
    private BigDecimal revenue;

    public DailyPaymentRevenue(LocalDate date, BigDecimal revenue) {
        this.date = date;
        this.revenue = revenue;
    }
}