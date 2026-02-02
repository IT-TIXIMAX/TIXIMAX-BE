package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class ExportedQuantity {
    private Long totalCode;
    private Double totalWeight;
    private Long totalCustomers;
}
