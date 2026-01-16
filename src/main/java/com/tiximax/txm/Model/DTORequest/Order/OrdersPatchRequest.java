package com.tiximax.txm.Model.DTORequest.Order;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data

public class OrdersPatchRequest {
    private BigDecimal priceShip;
    private BigDecimal exchangeRate;
    private Boolean checkRequired;
    private List<OrderLinkPatch> orderLinks;
}
