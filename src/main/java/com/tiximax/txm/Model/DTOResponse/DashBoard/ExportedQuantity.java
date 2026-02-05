package com.tiximax.txm.Model.DTOResponse.DashBoard;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

     public void setTotalWeight(Double totalWeight) {
        if (totalWeight == null) {
            this.totalWeight = null;
            return;
        }

        this.totalWeight = BigDecimal.valueOf(totalWeight)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
