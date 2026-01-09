package com.tiximax.txm.Model.DTOResponse.DraftDomestic;

import java.util.List;

import com.tiximax.txm.Entity.DraftDomestic;

import lombok.Data;
@Data
public class DraftDomesticResponse {
    private Long id;
    private String customerName;
    private String phoneNumber;
    private String address;
    private List<String> shippingList;
    private double weight;
    private String VNPostTrackingCode;
   
    public DraftDomesticResponse(DraftDomestic draftDomestic) {
        this.id = draftDomestic.getId();
        this.phoneNumber = draftDomestic.getPhoneNumber();
        this.address = draftDomestic.getAddress();
        this.shippingList = draftDomestic.getShippingList();
        this.weight = draftDomestic.getWeight();
        this.VNPostTrackingCode = draftDomestic.getVNPostTrackingCode();
        this.customerName = draftDomestic.getCustomer().getName();
    }
}
