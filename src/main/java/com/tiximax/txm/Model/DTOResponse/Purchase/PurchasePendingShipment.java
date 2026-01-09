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
    private String orderCode;
    private Long orderId;
    private String staffName;
    private List<OrderLinkPending> pendingLinks;
    private Orders orders;
    private Customer customer;
    private Staff staff;
    private String note;

    public PurchasePendingShipment(Purchases purchase, List<OrderLinkPending> links) {
        this.purchaseId = purchase.getPurchaseId();
        this.purchaseCode = purchase.getPurchaseCode();
        this.purchaseImage = purchase.getPurchaseImage();
        this.purchaseTime = purchase.getPurchaseTime();
        this.finalPriceOrder = purchase.getFinalPriceOrder();
        this.orderCode = purchase.getOrders().getOrderCode();
        this.orderId = purchase.getOrders().getOrderId();
        this.staffName = purchase.getStaff().getName();
        this.pendingLinks = links;
        this.orders = purchase.getOrders();
        this.customer = purchase.getOrders().getCustomer();
        this.staff = purchase.getOrders().getStaff();
        this.note = purchase.getNote();
    }
}
