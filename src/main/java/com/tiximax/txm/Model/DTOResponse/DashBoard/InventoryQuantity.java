package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class InventoryQuantity {
    private Long exportedCode;
    private Double exportedWeightKg;  
    private Long remainingCode;
    private Double remainingWeightKg;

    
}
