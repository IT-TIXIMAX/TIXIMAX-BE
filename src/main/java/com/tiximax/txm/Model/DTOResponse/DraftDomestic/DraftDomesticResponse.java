package com.tiximax.txm.Model.DTOResponse.DraftDomestic;

import java.util.List;

import com.tiximax.txm.Entity.DraftDomestic;
import com.tiximax.txm.Entity.DraftDomesticShipment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class DraftDomesticResponse {

    private Long id;
    private String customerName;
    private String phoneNumber;
    private String address;
    private String shipCode;
    private String VNPostTrackingCode;
    private String staffCode;
    private Boolean payment;
    private String weight;

    // set sau
    private List<String> shippingList;

    // 🔥 CONSTRUCTOR DÙNG CHO JPQL
    public DraftDomesticResponse(
            Long id,
            String customerName,
            String phoneNumber,
            String address,
            String shipCode,
            String VNPostTrackingCode,
            String staffCode,
            Boolean payment,
            String weight
    ) {
        this.id = id;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.shipCode = shipCode;
        this.VNPostTrackingCode = VNPostTrackingCode;
        this.staffCode = staffCode;
        this.payment = payment;
        this.weight = weight;
    }
    public DraftDomesticResponse(DraftDomestic draft) {
    this.id = draft.getId();
    this.customerName = draft.getCustomer().getName();
    this.phoneNumber = draft.getPhoneNumber();
    this.address = draft.getAddress();
    this.shipCode = draft.getShipCode();
    this.VNPostTrackingCode = draft.getVNPostTrackingCode();
    this.staffCode =
            draft.getStaff() != null
                    ? draft.getStaff().getStaffCode()
                    : null;
    this.payment = draft.isPayment();
    this.weight = draft.getWeight().toString();

    if (draft.getShipments() != null) {
        this.shippingList = draft.getShipments()
                .stream()
                .map(DraftDomesticShipment::getShipmentCode)
                .toList();
    }
}
}
