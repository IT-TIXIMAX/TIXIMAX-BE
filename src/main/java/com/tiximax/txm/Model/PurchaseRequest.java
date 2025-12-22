package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter

public class PurchaseRequest {

    private BigDecimal purchaseTotal;

    private String image;

    private String note;

    private List<String> packingCode;

    private String shipmentCode;

    private BigDecimal exchangeRate;

    private List<String> trackingCode;

}
