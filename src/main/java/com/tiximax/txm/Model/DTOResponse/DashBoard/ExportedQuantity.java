package com.tiximax.txm.Model.DTOResponse.DashBoard;

import java.time.LocalDate;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class ExportedQuantity {
    private LocalDate date;
    private Long totalCode;
    private Double totalWeight;
    private Long totalCustomers;
}
