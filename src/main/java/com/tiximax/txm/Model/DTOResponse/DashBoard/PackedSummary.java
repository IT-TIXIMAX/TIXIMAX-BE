package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class PackedSummary {
    private long packing;
    private long warehouses;
    private long orders;
    private Double weight;
    private Double netWeight;

    public PackedSummary(long packing, long warehouses, long orders, Double weight, Double netWeight) {
        this.packing = packing;
        this.warehouses = warehouses;
        this.orders = orders;
        this.weight = weight != null ? weight : 0.0;
        this.netWeight = netWeight != null ? netWeight : 0.0;
    }
}
