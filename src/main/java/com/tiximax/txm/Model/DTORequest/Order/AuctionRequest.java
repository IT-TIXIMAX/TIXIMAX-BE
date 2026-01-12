package com.tiximax.txm.Model.DTORequest.Order;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter

public class AuctionRequest {

    private BigDecimal purchaseTotal;

    private String image;

    private String note;

    private List<String> packingCode;

    private String shipmentCode;

    private List<String> trackingCode;

    private BigDecimal shipWeb;
    
    private BigDecimal purchaseFee;

}
