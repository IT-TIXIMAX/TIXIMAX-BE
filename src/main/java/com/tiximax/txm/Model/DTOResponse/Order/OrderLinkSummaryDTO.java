package com.tiximax.txm.Model.DTOResponse.Order;

import java.math.BigDecimal;

import com.tiximax.txm.Enums.OrderLinkStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderLinkSummaryDTO {
    private Long linkId;
    private String productName;
    private Integer quantity;
    private String shipmentCode;
    private BigDecimal shipWeb;
    private String website;
    private String classify;
    private String purchaseImage;
    private String trackingCode;
    private OrderLinkStatus status;
    private String groupTag;
    private Long orderId;
}

