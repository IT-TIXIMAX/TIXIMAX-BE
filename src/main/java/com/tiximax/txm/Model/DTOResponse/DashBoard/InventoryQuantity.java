package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class InventoryQuantity {
    private Double exportedCode;
    private double exportedWeightKg;  
    private double remainingCode;
    private double remainingWeightKg;
}
