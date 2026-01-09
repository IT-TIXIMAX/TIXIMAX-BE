package com.tiximax.txm.Model.DTOResponse.Warehouse;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.tiximax.txm.Enums.OrderLinkStatus;

@Getter
@Setter


public class WarehouseSummary {

    private Long warehouseId;
    private String customerCode;
    private String trackingCode;
    private String destination;
    private String orderCode;
    private Double weight;
    private Double netWeight;
    private Double dim;
    private String image;
    private String imageCheck;
    private LocalDateTime createdAt;

    public WarehouseSummary(Long warehouseId, String trackingCode, String orderCode,String customerCode ,String destination, Double weight, Double netWeight, Double dim, String image, String imageCheck, LocalDateTime createdAt,  OrderLinkStatus orderLinkStatus) {
        this.warehouseId = warehouseId;
        this.trackingCode = trackingCode;
        this.orderCode = orderCode;
        this.customerCode = customerCode;
        this.destination = destination;
        this.weight = weight;
        this.netWeight = netWeight;
        this.dim = dim;
        this.image = image;
        this.imageCheck = imageCheck;
        this.createdAt = createdAt;
    }

}
