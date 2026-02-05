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
    private String customerCode;
    private String customerName;
    private String staffName;
    private String staffCode;
    private Long purchaseId;

     public OrderLinkPending(
            Long linkId,
            String productName,
            Integer quantity,
            String shipmentCode,
            BigDecimal shipWeb,
            String website,
            String classify,
            String purchaseImage,
            String trackingCode,
            OrderLinkStatus status,  
            String customerCode,
            String customerName,
            String staffName,
            String staffCode,
            Long purchaseId
    ) {
        this.linkId = linkId;
        this.productName = productName;
        this.quantity = quantity;
        this.shipmentCode = shipmentCode;
        this.shipWeb = shipWeb;
        this.website = website;
        this.classify = classify;
        this.purchaseImage = purchaseImage;
        this.trackingCode = trackingCode;
        this.status = status;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.staffCode = staffCode;
        this.staffName = staffName;
        this.purchaseId = purchaseId;
    }
    public OrderLinkPending(OrderLinks ol) {
    this.linkId = ol.getLinkId();
    this.productName = ol.getProductName();
    this.quantity = ol.getQuantity();
    this.shipmentCode = ol.getShipmentCode();
    this.shipWeb = ol.getShipWeb();
    this.website = ol.getWebsite();
    this.classify = ol.getClassify();
    this.purchaseImage = ol.getPurchaseImage();
    this.trackingCode = ol.getTrackingCode();
    this.status = ol.getStatus();
    this.customerCode = ol.getOrders().getCustomer().getCustomerCode();
    this.customerName = ol.getOrders().getCustomer().getName();
    this.staffName = ol.getOrders().getStaff().getName();
    this.staffCode = ol.getOrders().getStaff().getStaffCode();
    this.purchaseId =
            ol.getPurchase() != null
                    ? ol.getPurchase().getPurchaseId()
                    : null;
}
}

