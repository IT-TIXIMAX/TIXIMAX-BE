package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WarehouseOverdueResponse {
    private Long warehouseId;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private String staffName;
    private String routeName;
    private String status;
    private LocalDate domesticArrivalDate;
    private Long daysInWarehouse;
    private Double netWeight;

    public WarehouseOverdueResponse(Long warehouseId,
                                    Long orderId,
                                    Long customerId,
                                    String customerName,
                                    String staffName,
                                    String routeName,
                                    String status,
                                    LocalDate domesticArrivalDate,
                                    Long daysInWarehouse,
                                    Double netWeight) {
        this.warehouseId = warehouseId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.staffName = staffName;
        this.routeName = routeName;
        this.status = status;
        this.domesticArrivalDate = domesticArrivalDate;
        this.daysInWarehouse = daysInWarehouse;
        this.netWeight = netWeight;
    }
}