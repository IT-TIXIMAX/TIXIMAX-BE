package com.tiximax.txm.Model.DTOResponse.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tiximax.txm.Entity.Payment;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;

import lombok.Data;
@Data
public class PaymentAuctionResponse {
    private Long paymentId;
    private String paymentCode;
    private String content;
    private PaymentType paymentType;
    private BigDecimal amount;
    private BigDecimal collectedAmount;
    private PaymentStatus status;
    private String qrCode;
    private Integer depositPercent;
    private LocalDateTime actionAt;

    private Long customerId;
    private String customerName;
    private String customerCode;

        public PaymentAuctionResponse(Payment payment) {
        this.paymentId = payment.getPaymentId();
        this.paymentCode = payment.getPaymentCode();
        this.content = payment.getContent();
        this.paymentType = payment.getPaymentType();
        this.amount = payment.getAmount();
        this.collectedAmount = payment.getCollectedAmount();
        this.status = payment.getStatus();
        this.qrCode = payment.getQrCode();
        this.depositPercent = payment.getDepositPercent();
        this.actionAt = payment.getActionAt();

        if (payment.getCustomer() != null) {
            this.customerId = payment.getCustomer().getAccountId();
            this.customerName = payment.getCustomer().getName();
            this.customerCode = payment.getCustomer().getCustomerCode();
        }
    }
    }