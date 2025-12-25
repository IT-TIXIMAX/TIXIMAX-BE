package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class RouteWeightSummary {
    private String routeName;
    private BigDecimal totalWeight;
    private BigDecimal totalNetWeight;

    public RouteWeightSummary(String routeName, BigDecimal totalWeight, BigDecimal totalNetWeight) {
        this.routeName = routeName;
        this.totalWeight = totalWeight != null ? totalWeight : BigDecimal.ZERO;
        this.totalNetWeight = totalNetWeight != null ? totalNetWeight : BigDecimal.ZERO;
    }
}
