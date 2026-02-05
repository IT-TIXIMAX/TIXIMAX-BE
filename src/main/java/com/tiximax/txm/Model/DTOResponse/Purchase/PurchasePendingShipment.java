package com.tiximax.txm.Model.DTOResponse.Purchase;

import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Orders;
import com.tiximax.txm.Entity.Purchases;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Model.DTOResponse.OrderLink.OrderLinkPending;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter

public class PurchasePendingShipment {

    private Long purchaseId;
    private String purchaseCode;
    private LocalDateTime purchaseTime;
    private String purchaseImage;
    private BigDecimal finalPriceOrder;

    private Long orderId;
    private String orderCode;
    private String staffName;
    private String note;

    private List<OrderLinkPending> pendingLinks = new ArrayList<>();

    public PurchasePendingShipment(
            Long purchaseId,
            String purchaseCode,
            LocalDateTime purchaseTime,
            String purchaseImage,
            BigDecimal finalPriceOrder,
            Long orderId,
            String orderCode,
            String staffName,
            String note
    ) {
        this.purchaseId = purchaseId;
        this.purchaseCode = purchaseCode;
        this.purchaseTime = purchaseTime;
        this.purchaseImage = purchaseImage;
        this.finalPriceOrder = finalPriceOrder;
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.staffName = staffName;
        this.note = note;
    }
    public PurchasePendingShipment(
        Purchases purchase,
        List<OrderLinkPending> pendingLinks
) {
    this.purchaseId = purchase.getPurchaseId();
    this.purchaseCode = purchase.getPurchaseCode();
    this.purchaseTime = purchase.getPurchaseTime();
    this.purchaseImage = purchase.getPurchaseImage();
    this.finalPriceOrder = purchase.getFinalPriceOrder();
    this.staffName = purchase.getStaff().getName();
    Orders order = purchase.getOrders();
    if (order != null) {
        this.orderId = order.getOrderId();
        this.orderCode = order.getOrderCode();
    }

    this.note = purchase.getNote();
    this.pendingLinks = pendingLinks;
}
}