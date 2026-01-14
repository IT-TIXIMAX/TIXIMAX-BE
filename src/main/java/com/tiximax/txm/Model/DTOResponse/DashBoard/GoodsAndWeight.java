package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter

public class GoodsAndWeight {
    private BigDecimal totalGoods;
    private Double totalNetWeight;

    public GoodsAndWeight(BigDecimal totalGoods, Double totalNetWeight){
        this.totalGoods = totalGoods;
        this.totalNetWeight = totalNetWeight;
    }
}
