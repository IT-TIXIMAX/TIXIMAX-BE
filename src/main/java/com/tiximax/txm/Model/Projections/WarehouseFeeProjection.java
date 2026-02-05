package com.tiximax.txm.Model.Projections;

import java.math.BigDecimal;

public interface WarehouseFeeProjection {

    String getTrackingCode();
    String getShipCode();
    Long getRouteId();
    BigDecimal getMinWeight();
    BigDecimal getPriceShip();

    Double getNetWeight();
}
