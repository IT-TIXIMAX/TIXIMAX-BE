package com.tiximax.txm.Model.DTORequest.OrderLink;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ShipmentCode {
    private String ShipmentCode;

    public String getShipmentCode() {
        return ShipmentCode;
    }
}

