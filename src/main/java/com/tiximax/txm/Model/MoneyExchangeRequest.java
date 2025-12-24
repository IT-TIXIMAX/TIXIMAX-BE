package com.tiximax.txm.Model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
@Data
public class MoneyExchangeRequest {
    
    private BigDecimal exchangeRate;

    private BigDecimal moneyExChange;

    private BigDecimal fee;

    private String note;
    
}
