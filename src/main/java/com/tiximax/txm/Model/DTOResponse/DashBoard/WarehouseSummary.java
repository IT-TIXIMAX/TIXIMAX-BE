package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class WarehouseSummary {
    private WarehouseStatistic inStock;   
    private WarehouseStatistic exportByVnPost;
    private WarehouseStatistic exportByOther;
    private WarehouseStatistic UNPAID_SHIPPING;
    private WarehouseStatistic PAID_SHIPPING;
}
