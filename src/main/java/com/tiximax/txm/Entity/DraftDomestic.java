package com.tiximax.txm.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tiximax.txm.Enums.Carrier;
import com.tiximax.txm.Enums.DraftDomesticStatus;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DraftDomestic{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_domestic_id")
    private Long id;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private String address;
    @ElementCollection
    private List<String> shippingList ;
    @Column(nullable = false)
    private String shipCode;
    @Column(nullable = true)
    private Double weight;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Carrier carrier;

    @Column(nullable = false)
    private boolean payment = false;

    @Column(nullable = true)
    private String noteTracking;

    @Column(nullable = true)
    private String VNPostTrackingCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftDomesticStatus status;

    @ManyToOne
    @JoinColumn(name="customer_id", nullable = false)
    @JsonManagedReference
    Customer customer;
    
    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
        this.status = DraftDomesticStatus.DRAFT;
    }
}
}


