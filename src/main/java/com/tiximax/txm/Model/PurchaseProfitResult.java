package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Data
@Getter
@Setter

public class PurchaseProfitResult {
    private String title;
    private BigDecimal profit;

    public PurchaseProfitResult(String title, BigDecimal profit) {
        this.title = title;
        this.profit = profit;
    }
}
