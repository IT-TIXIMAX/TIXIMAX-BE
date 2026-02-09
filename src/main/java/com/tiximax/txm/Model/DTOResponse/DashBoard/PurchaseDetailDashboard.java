package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseDetailDashboard {
    private String CustomerCode;
    private String CustomerName;
    private String StaffCode;
    private String StaffName;
    private Long TotalOrderLinks;
    private Long WaitingBuy;
    private Long Bought;
    private Long HasShipmentCode;
    private Long LackShipmentCode;
    public PurchaseDetailDashboard(
            String customerCode,
            String customerName,
            String staffCode,
            String staffName,
            Long totalOrderLinks,
            Long waitingBuy,
            Long bought,
            Long hasShipmentCode,
            Long lackShipmentCode
    ) {
        CustomerCode = customerCode;
        CustomerName = customerName;
        StaffCode = staffCode;
        StaffName = staffName;
        TotalOrderLinks = totalOrderLinks;
        WaitingBuy = waitingBuy;
        Bought = bought;
        HasShipmentCode = hasShipmentCode;
        LackShipmentCode = lackShipmentCode;
    }
}
