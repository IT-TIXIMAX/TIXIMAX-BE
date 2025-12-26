package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Getter
@Setter

public class EffectiveRateResponse {
    private String routeName;
    private BigDecimal exchangeRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
}
