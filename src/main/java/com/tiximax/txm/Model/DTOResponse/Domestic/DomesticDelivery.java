package com.tiximax.txm.Model.DTOResponse.Domestic;

import java.util.List;

import lombok.Data;

@Data
public class DomesticDelivery {
    public String customerCode;
    public String customerName;
    public String phoneNumber;
    public String address;
    public String staffName;
    public String staffCode;
    public String status;
    public List<String> shipmentCode;
    
    public DomesticDelivery(String customerCode, String customerName, String phoneNumber,
                              String address, String staffName,String staffCode ,String status,
                              List<String> shipmentCode) {
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.staffName = staffName;
        this.staffCode = staffCode;
        this.status = status;
        this.shipmentCode = shipmentCode;
    }
}
