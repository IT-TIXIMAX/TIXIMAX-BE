package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Data
@Getter
@Setter

public class RoutePaymentSummary {
    private String routeName;
    private BigDecimal totalRevenue;

    public RoutePaymentSummary(String routeName, BigDecimal totalRevenue) {
        this.routeName = routeName;
        this.totalRevenue = totalRevenue;
    }
}
