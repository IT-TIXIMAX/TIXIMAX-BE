package com.tiximax.txm.Model.DTORequest.Purchase;


import java.math.BigDecimal;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class UpdateShipmentRequest {
    private String shipmentCode;    // bắt buộc
    private BigDecimal shipFee;     // có thể null
}
