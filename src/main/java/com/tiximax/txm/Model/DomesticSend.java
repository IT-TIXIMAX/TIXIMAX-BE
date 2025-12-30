package com.tiximax.txm.Model;

import java.util.List;

import com.tiximax.txm.Entity.Domestic;

public class DomesticSend {
    public String customerName;
    public String customerCode;
    public String phoneNumber;
    public String toAddress;
    public List<String> shippingList;
    public String staffName;
    public String staffCode;

    public DomesticSend(Domestic domestic) {
        this.customerName = domestic.getToAddress().getCustomer().getName();
        this.customerCode = domestic.getToAddress().getCustomer().getCustomerCode();
        this.phoneNumber = domestic.getToAddress().getCustomer().getPhone();
        this.toAddress = domestic.getToAddress().getAddressName();
        this.shippingList = domestic.getShippingList();
        this.staffName = domestic.getToAddress().getCustomer().getName();
        this.staffCode = domestic.getStaff().getStaffCode();
    }

}
