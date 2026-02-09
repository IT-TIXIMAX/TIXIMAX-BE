package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter

public class Purchases {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(nullable = true)
    private String purchaseCode;

    private String purchaseImage;

    private LocalDateTime purchaseTime;

    private BigDecimal finalPriceOrder;

    private BigDecimal exchangeRate = BigDecimal.valueOf(0);

    private String note;

    @Column(name = "is_purchased")
    private Boolean purchased;

    private String invoice;

    @ManyToOne
    @JoinColumn(name="staff_id", nullable = false)
    @JsonIgnore
    Staff staff;

    @ManyToOne
    @JoinColumn(name="order_id", nullable = false)
    @JsonIgnore
    Orders orders;

    @OneToMany(mappedBy = "purchase", fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    Set<OrderLinks> orderLinks;

    @OneToMany(mappedBy = "purchase", fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    Set<Warehouse> warehouses;

}
