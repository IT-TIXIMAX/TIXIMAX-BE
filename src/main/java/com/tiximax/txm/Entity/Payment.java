package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.PaymentPurpose;
import com.tiximax.txm.Enums.PaymentStatus;
import com.tiximax.txm.Enums.PaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter

public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(nullable = false)
    private String paymentCode;

    private String content;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal collectedAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = true)
    private String qrCode;

    private Integer depositPercent;

    private LocalDateTime actionAt;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentPurpose purpose;

    @Column(nullable = false)
    private Boolean isMergedPayment;

    private Double collectWeight;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable = false)
    @JsonIgnore
    Customer customer;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;
    
    @ManyToOne
    @JoinColumn(name="order_id", nullable = true)
    @JsonIgnore
    Orders orders;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "payment_orders",
            joinColumns = @JoinColumn(name = "payment_id"),
            inverseJoinColumns = @JoinColumn(name = "order_id")
    )
    @JsonIgnore
    private Set<Orders> relatedOrders;

   @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<PartialShipment> partialShipments = new HashSet<>();
}
