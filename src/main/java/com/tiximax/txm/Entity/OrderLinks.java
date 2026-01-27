package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiximax.txm.Enums.OrderLinkStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;


@Entity
@Getter
@Setter

public class OrderLinks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    @Column(nullable = true)
    private String productLink;

    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    private BigDecimal priceWeb;

    private BigDecimal shipWeb;

    private BigDecimal totalWeb;

    private BigDecimal purchaseFee;

    private BigDecimal extraCharge;

    private BigDecimal finalPriceVnd;

    @Column(nullable = false)
    private String trackingCode;

    private String classify;

    private String purchaseImage;

    private String website;

    private String shipmentCode;

    @Enumerated(EnumType.STRING)
    private OrderLinkStatus status;

    private String note;

    private String groupTag;

    @ManyToOne
    @JoinColumn(name="order_id", nullable = false)
    @JsonIgnore
    Orders orders;

    @ManyToOne
    @JoinColumn(name="purchase_id", nullable = true)
    @JsonIgnore
    Purchases purchase;

    @ManyToOne
    @JoinColumn(name="product_type_id", nullable = true)
    @JsonIgnore
    ProductType productType;

    @ManyToOne
    @JoinColumn(name="warehouse_id", nullable = true)
    @JsonIgnore
    Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "partial_shipment_id", nullable = true)
    @JsonIgnore
    private PartialShipment partialShipment;
}
