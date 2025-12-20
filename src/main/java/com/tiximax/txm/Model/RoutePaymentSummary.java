package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Data
@Getter
@Setter

public class RoutePaymentSummary {
    private Long routeId;
    private BigDecimal totalPaidAmount;

    public RoutePaymentSummary(Long routeId, BigDecimal totalPaidAmount) {
        this.routeId = routeId;
        this.totalPaidAmount = totalPaidAmount;
    }
}
