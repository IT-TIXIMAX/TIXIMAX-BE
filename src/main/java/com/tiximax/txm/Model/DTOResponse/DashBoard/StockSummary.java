package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class StockSummary {
    private long warehouses;
    private Double weight;
    private Double netWeight;

    public StockSummary(long warehouses, Double weight, Double netWeight) {
        this.warehouses = warehouses;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }
}
