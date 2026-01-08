package com.tiximax.txm.Model.DTOResponse.DraftDomestic;

import java.util.List;

public class AvailableAddDarfDomestic {
 public String customerCode;
    public String customerName;
    public String phoneNumber;
    public String address;
    public List<String> shipmentCode;
    public AvailableAddDarfDomestic() {}
    public AvailableAddDarfDomestic(String customerCode, String customerName, String phoneNumber,
                              String address,
                              List<String> shipmentCode) {
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.shipmentCode = shipmentCode;
    }
}
