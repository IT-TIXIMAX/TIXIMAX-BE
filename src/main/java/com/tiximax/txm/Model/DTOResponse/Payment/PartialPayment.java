package com.tiximax.txm.Model.DTOResponse.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tiximax.txm.Entity.PartialShipment;
import com.tiximax.txm.Enums.PaymentStatus;
import lombok.Data;

@Data
public class PartialPayment {

    private Long partialShipmentId;
    private BigDecimal partialAmount;
    private LocalDateTime shipmentDate;
    private String note;
    private Long orderId;
    private String orderCode;
    private Long paymentId;
    private String paymentCode;
    private BigDecimal paymentAmount;
    private PaymentStatus paymentStatus;
    private String staffCode;
    private String staffName;

    public PartialPayment(PartialShipment ps) {
    this.partialShipmentId = ps.getId();
    this.partialAmount = ps.getPartialAmount();
    this.shipmentDate = ps.getShipmentDate();
    this.note = ps.getNote();

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

    // Staff
    if (ps.getStaff() != null) {
        this.staffCode = ps.getStaff().getStaffCode();
        this.staffName = ps.getStaff().getName();
    }
}
}
