package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

@Data
public class WarehouseTimeResponse {
    private String country;
    private Long totalDelivered;
    private Double avgDaysInDomesticWarehouse;
    private Double avgDaysInForeignWarehouse;

    public WarehouseTimeResponse(String country,
                                 Long totalDelivered,
                                 Double avgDaysInDomesticWarehouse,
                                 Double avgDaysInForeignWarehouse) {
        this.country = country;
        this.totalDelivered = totalDelivered;
        this.avgDaysInDomesticWarehouse = avgDaysInDomesticWarehouse;
        this.avgDaysInForeignWarehouse = avgDaysInForeignWarehouse;
    }
}