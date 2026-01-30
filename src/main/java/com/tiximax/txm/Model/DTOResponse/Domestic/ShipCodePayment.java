package com.tiximax.txm.Model.DTOResponse.Domestic;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
@Getter
@Setter
public class ShipCodePayment {
    private String shipCode;
    private String customerId ;
    private String customerName;
    private boolean payment;
    private List<WarehouseShip> warehouseShips;
    private BigDecimal totalPriceShip;
}
