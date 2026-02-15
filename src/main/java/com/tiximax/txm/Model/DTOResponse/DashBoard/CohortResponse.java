package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data

public class CohortResponse {
    private LocalDateTime firstMonth;
    private LocalDateTime orderMonth;
    private Integer monthIndex;
    private Long activeCustomers;

    public CohortResponse(LocalDateTime firstMonth,
                          LocalDateTime orderMonth,
                            Integer monthIndex,
                            Long activeCustomers){
        this.firstMonth = firstMonth;
        this.orderMonth = orderMonth;
        this.monthIndex = monthIndex;
        this.activeCustomers = activeCustomers;
    }
}
