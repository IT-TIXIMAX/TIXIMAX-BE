package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AvgDeliveryResponse {
    private LocalDate month;
    private String routeName;
    private BigDecimal avgDeliveryHours;

    public AvgDeliveryResponse(LocalDate month,
                               String routeName,
                               BigDecimal avgDeliveryHours) {
        this.month = month;
        this.routeName = routeName;
        this.avgDeliveryHours = avgDeliveryHours;
    }
}