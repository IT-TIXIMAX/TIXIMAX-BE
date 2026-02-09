package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class PurchaseSummary {
    private Long totalOrderlinks;
    private Long waitingBuy;
    private Long bought;
    private Long hasShipmentCode ;
    private Long lackShipmentCode;
    
     public PurchaseSummary(
            Long totalOrderlinks,
            Long waitingBuy,
            Long bought,
            Long hasShipmentCode,
            Long lackShipmentCode
    ) {
        this.totalOrderlinks = totalOrderlinks;
        this.waitingBuy = waitingBuy;
        this.bought = bought;
        this.hasShipmentCode = hasShipmentCode;
        this.lackShipmentCode = lackShipmentCode;
    }
}
 
