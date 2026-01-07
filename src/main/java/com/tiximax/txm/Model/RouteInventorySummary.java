package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class RouteInventorySummary {
    private double totalWeight;
    private double totalNetWeight;

    public RouteInventorySummary(double totalWeight, double totalNetWeight) {
        this.totalWeight = totalWeight;
        this.totalNetWeight = totalNetWeight;
    }
}
