package com.tiximax.txm.Model.DTOResponse.Warehouse;

import java.math.BigDecimal;

import com.tiximax.txm.Enums.OrderLinkStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class WareHouseOrderLink {
    private Long warehouseId;
    private Double length;
    private Double width;
    private Double height;
    private Double weight;
    private Double dim;
    private Double netWeight;
    private Long linkId;
    private String productLink;
    private String productName;
    private Integer quantity;
    private BigDecimal priceWeb;
    private BigDecimal shipWeb;
    private BigDecimal totalWeb;
    private BigDecimal FinalPriceShip;
    private BigDecimal purchaseFee;
    private BigDecimal extraCharge;
    private BigDecimal finalPriceVnd;
    private String trackingCode;
    private String classify;
    private String purchaseImage;
    private String website;
    private String shipmentCode;
    @Enumerated(EnumType.STRING)
    private OrderLinkStatus status;
    private String note;
    private String groupTag;
}
