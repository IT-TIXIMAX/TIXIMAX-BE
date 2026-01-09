package com.tiximax.txm.Model.DTOResponse.OrderLink;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Entity.Warehouse;
import com.tiximax.txm.Enums.OrderLinkStatus;

import jakarta.persistence.Column;
import lombok.Data;

@Data

public class OrderLinksShip {

    private Long linkId;
    private String productName;
    private String image;
    private OrderLinkStatus status;
    private String packingCode;
    private String shipmentCode;
    private Double length;
    private Double width;

    private Double height;

    private Double dim;

    private Double weight;

    public OrderLinksShip(OrderLinks link, Warehouse warehouse,  String packingCode) {
        this.linkId = link.getLinkId();
        this.productName = link.getProductName();
        this.shipmentCode = link.getShipmentCode();
        this.status = link.getStatus();
        this.image = (warehouse != null) ? warehouse.getImage() : null;        
        this.length = (warehouse != null) ? warehouse.getLength() : null;
        this.width = (warehouse != null) ? warehouse.getWidth() : null;
        this.height =(warehouse != null) ? warehouse.getHeight() : null;
        this.dim = (warehouse != null) ? warehouse.getDim() : null;
        this.weight = (warehouse != null) ? warehouse.getWeight() : null;
        this.packingCode = packingCode;
    }
}


