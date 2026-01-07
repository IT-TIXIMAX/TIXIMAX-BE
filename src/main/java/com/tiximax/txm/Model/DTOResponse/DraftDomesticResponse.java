package com.tiximax.txm.Model.DTOResponse;

import java.util.List;

import com.tiximax.txm.Entity.DraftDomestic;

import lombok.Data;
@Data
public class DraftDomesticResponse {
    private String customerName;
    private String phoneNumber;
    private String address;
    private List<String> shippingList;
    private String weight;
    private String VNPostTrackingCode;
   
    public DraftDomesticResponse(DraftDomestic draftDomestic) {
        this.phoneNumber = draftDomestic.getPhoneNumber();
        this.address = draftDomestic.getAddress();
        this.shippingList = draftDomestic.getShippingList();
        this.weight = draftDomestic.getWeight();
        this.VNPostTrackingCode = draftDomestic.getVNPostTrackingCode();
        this.customerName = draftDomestic.getCustomer().getName();
    }
}
