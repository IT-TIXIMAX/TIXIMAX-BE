package com.tiximax.txm.Model.DTOResponse.DraftDomestic;

import java.util.List;

public class AvailableAddDarfDomestic {
 public String customerCode;
    public String customerName;
    public String phoneNumber;
    public String address;
    public String routeName;
    public List<String> shipmentCode;
    public AvailableAddDarfDomestic() {}
    public AvailableAddDarfDomestic(String customerCode, String customerName, String phoneNumber,
                              String address,String routeName,
                              List<String> shipmentCode) {
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.routeName = routeName;
        this.address = address;
        this.routeName = routeName;
        this.shipmentCode = shipmentCode;
    }
}
