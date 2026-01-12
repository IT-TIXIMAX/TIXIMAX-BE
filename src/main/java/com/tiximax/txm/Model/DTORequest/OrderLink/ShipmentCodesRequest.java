package com.tiximax.txm.Model.DTORequest.OrderLink;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ShipmentCodesRequest {
    private List<String> selectedShipmentCodes;

    public List<String> getSelectedShipmentCodes() {
        return selectedShipmentCodes;
    }
    public void SelectedShipmentCodes(List<String> selectedShipmentCodes) {
        this.selectedShipmentCodes = selectedShipmentCodes;
    }
}

