package com.tiximax.txm.Model.DTOResponse.OrderLink;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Enums.OrderLinkStatus;

import lombok.Data;
@Data
public class OrderLinksShipForeign {
    private Long linkId;
    private String productName;
    private String image;
    private OrderLinkStatus status;
    private String shipmentcode;

    public OrderLinksShipForeign(OrderLinks link) {
        this.linkId = link.getLinkId();
        this.productName = link.getProductName();
        this.shipmentcode = link.getShipmentCode();
        this.status = link.getStatus();
        this.image = link.getPurchaseImage();
    }
}
