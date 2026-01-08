package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.ExpenseStatus;
import com.tiximax.txm.Enums.PaymentMethod;
import com.tiximax.txm.Enums.VatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter

public class ExpenseRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_request_id")
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    private String note;

    private String invoiceImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private String bankInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VatStatus vatStatus;

    private String vatInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status;

    private String cancelReason;

    private String transferImage;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    @JsonIgnore
    private Staff requester;

    @ManyToOne
    @JoinColumn(name = "approver_id", nullable = true)
    @JsonIgnore
    private Staff approver;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String department;
}
