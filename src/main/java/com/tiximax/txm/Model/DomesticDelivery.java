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
    public List<String> shipmemtCode;
}
