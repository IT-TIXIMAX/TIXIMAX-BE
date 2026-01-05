package com.tiximax.txm.Model;

import java.util.List;

import lombok.Data;

@Data
public class DomesticDelivery {
    public String customerCode;
    public String customerName;
    public String phoneNumber;
    public String address;
    public String staffName;
    public String status;
    public List<String> shipmentCode;
    public DomesticDelivery() {}

    public DomesticDelivery(String customerCode, String customerName, String phoneNumber,
                              String address, String staffName, String status,
                              List<String> shipmentCode) {
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.staffName = staffName;
        this.status = status;
        this.shipmentCode = shipmentCode;
    }
}
