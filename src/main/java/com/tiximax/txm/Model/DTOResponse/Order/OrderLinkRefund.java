package com.tiximax.txm.Model.DTOResponse.Order;

import com.tiximax.txm.Enums.OrderLinkStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Data
@Getter
@Setter

public class OrderLinkRefund {
    private Long linkId;
    private String productLink;
    private String productName;
    private Integer quantity;
    private BigDecimal priceWeb;
    private BigDecimal shipWeb;
    private BigDecimal totalWeb;
    private BigDecimal purchaseFee;
    private BigDecimal extraCharge;
    private BigDecimal finalPriceVnd;
    private String trackingCode;
    private String classify;
    private String purchaseImage;
    private String website;
    private String shipmentCode;
    private OrderLinkStatus status;
    private String note;
    private String groupTag;

    public OrderLinkRefund(
            Long linkId,
            String productLink,
            String productName,
            Integer quantity,
            BigDecimal priceWeb,
            BigDecimal shipWeb,
            BigDecimal totalWeb,
            BigDecimal purchaseFee,
            BigDecimal extraCharge,
            BigDecimal finalPriceVnd,
            String trackingCode,
            String classify,
            String purchaseImage,
            String website,
            String shipmentCode,
            OrderLinkStatus status,
            String note,
            String groupTag) {

        this.linkId = linkId;
        this.productLink = productLink;
        this.productName = productName;
        this.quantity = quantity;
        this.priceWeb = priceWeb;
        this.shipWeb = shipWeb;
        this.totalWeb = totalWeb;
        this.purchaseFee = purchaseFee;
        this.extraCharge = extraCharge;
        this.finalPriceVnd = finalPriceVnd;
        this.trackingCode = trackingCode;
        this.classify = classify;
        this.purchaseImage = purchaseImage;
        this.website = website;
        this.shipmentCode = shipmentCode;
        this.status = status;
        this.note = note;
        this.groupTag = groupTag;
    }
}
