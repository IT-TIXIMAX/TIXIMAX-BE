package com.tiximax.txm.Model.DTOResponse.Purchase;

import com.tiximax.txm.Entity.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Getter
@Setter

public class PurchaseDetail {

    private Long purchaseId;
    private String purchaseCode;
    private String purchaseImage;
    private LocalDateTime purchaseTime;
    private String note;
    private Orders orders;
    private Set<OrderLinks> orderLinks;
    private Set<Warehouse> warehouses;
    private Staff staff;
    private Customer customer;

    public PurchaseDetail(Purchases purchases) {

        this.purchaseId = purchases.getPurchaseId();
        this.purchaseCode = purchases.getPurchaseCode();
        this.purchaseImage = purchases.getPurchaseImage();
        this.purchaseTime = purchases.getPurchaseTime();
        this.note = purchases.getNote();
        this.orders = purchases.getOrders();
        this.orderLinks = purchases.getOrderLinks();
        this.warehouses = purchases.getWarehouses();
        this.staff = purchases.getOrders().getStaff();
        this.customer = purchases.getOrders().getCustomer();

    }
}
