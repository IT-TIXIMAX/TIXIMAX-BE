package com.tiximax.txm.Model.DTORequest.Route;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class RouteRequest {

    private String name;

    private String shipTime;

    private BigDecimal unitBuyingPrice;

    private BigDecimal unitDepositPrice;

    private BigDecimal exchangeRate;

    private String note;

    private BigDecimal differenceRate;

    private boolean isUpdateAuto = true;

}
