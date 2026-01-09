package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class StaffPerformanceKPI {
    private String staffCode;

    private String name;

    private BigDecimal totalGoods;

    private Double totalNetWeight;
}
