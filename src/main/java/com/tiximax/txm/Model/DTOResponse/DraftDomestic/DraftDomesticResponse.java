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
    private String shipCode;
    private Double weight;
    private String VNPostTrackingCode;
    private String staffCode;
   
    public DraftDomesticResponse(DraftDomestic draftDomestic) {
        this.id = draftDomestic.getId();
        this.phoneNumber = draftDomestic.getPhoneNumber();
        this.address = draftDomestic.getAddress();
        this.shippingList = draftDomestic.getShippingList();
        this.shipCode = draftDomestic.getShipCode();
        this.weight = draftDomestic.getWeight();
        this.VNPostTrackingCode = draftDomestic.getVNPostTrackingCode();
        this.customerName = draftDomestic.getCustomer().getName();
        this.staffCode = draftDomestic.getStaff().getStaffCode();
    }
}
