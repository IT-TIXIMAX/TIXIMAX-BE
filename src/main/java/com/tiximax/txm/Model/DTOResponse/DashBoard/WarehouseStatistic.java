package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStatistic {
     private Long totalCodes;        
    private Double totalWeight;     
    private Long totalCustomers;
}
