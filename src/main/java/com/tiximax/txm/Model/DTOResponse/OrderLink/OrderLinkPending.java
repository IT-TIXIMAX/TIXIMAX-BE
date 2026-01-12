package com.tiximax.txm.Model.DTOResponse.OrderLink;

import java.math.BigDecimal;

import com.tiximax.txm.Entity.OrderLinks;
import com.tiximax.txm.Enums.OrderLinkStatus;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class OrderLinkPending {
    private Long linkId;
    private String productName;
    private Integer quantity;
    private String shipmentCode;
    private BigDecimal shipWeb;
    private String website;
    private String classify;
    private String purchaseImage;
    private String trackingCode;
    private OrderLinkStatus status;

    public OrderLinkPending(OrderLinks link) {
        this.linkId = link.getLinkId();
        this.productName = link.getProductName();
        this.shipWeb = link.getShipWeb();
        this.status = link.getStatus();
        this.quantity = link.getQuantity();
        this.shipmentCode = link.getShipmentCode();
        this.website = link.getWebsite();
        this.trackingCode = link.getTrackingCode();
        this.classify = link.getClassify();
        this.purchaseImage = link.getPurchaseImage();
    }
}
