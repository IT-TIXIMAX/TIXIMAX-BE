package com.tiximax.txm.Model.DTORequest.Order;

import lombok.Data;
import java.math.BigDecimal;

@Data

public class OrderLinkPatch {
    private Long orderLinkId;
    private String productLink;
    private String productName;
    private Integer quantity;
    private BigDecimal priceWeb;
    private BigDecimal shipWeb;
    private BigDecimal purchaseFee;
    private BigDecimal extraCharge;
    private String purchaseImage;
    private String website;
    private String note;
    private String classify;
    private String groupTag;
}
