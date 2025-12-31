package com.tiximax.txm.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Entity.Staff;
import com.tiximax.txm.Enums.PaymentStatus;

import lombok.Data;
@Data
public class PartialPayment {
private Long partialShipmentId;

    private Long orderId;
    private String orderCode;

    private BigDecimal partialAmount;
    private LocalDateTime shipmentDate;

    private String paymentCode;
    private Long paymentId;
    private BigDecimal paymentAmount;
    private PaymentStatus paymentStatus;

    private String note;
    private Staff staff;

    public PartialPayment(PartialShipment ps) {
        this.partialShipmentId = ps.getId();
        this.partialAmount = ps.getPartialAmount();
        this.shipmentDate = ps.getShipmentDate();
        this.note = ps.getNote();
        this.staff = ps.getStaff();

        if (ps.getOrders() != null) {
            this.orderId = ps.getOrders().getOrderId();
            this.orderCode = ps.getOrders().getOrderCode();
        }

        if (ps.getPayment() != null) {
            this.paymentId = ps.getPayment().getPaymentId();
            this.paymentCode = ps.getPayment().getPaymentCode();
            this.paymentAmount = ps.getPayment().getAmount();
            this.paymentStatus = ps.getPayment().getStatus();
        }
    }
}
