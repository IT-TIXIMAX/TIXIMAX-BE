package com.tiximax.txm.Model.DTOResponse.Domestic;

import java.math.BigDecimal;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class WarehouseShip {
    private String shipmentCode;
    private Double netWeight;
    private Double weight;
    private Double height;
    private Double length;
    private Double width;
    private BigDecimal priceship;
     public WarehouseShip(
            String shipmentCode,
            Double netWeight,
            Double weight,
            Double height,
            Double length,
            Double width,
            BigDecimal pricePerKg
    ) {
        this.shipmentCode = shipmentCode;
        this.netWeight = netWeight;
        this.weight = weight;
        this.height = height;
        this.length = length;
        this.width = width;

        this.priceship =
            pricePerKg.multiply(BigDecimal.valueOf(
                netWeight == null ? 0 : netWeight
            ));
    }
}

