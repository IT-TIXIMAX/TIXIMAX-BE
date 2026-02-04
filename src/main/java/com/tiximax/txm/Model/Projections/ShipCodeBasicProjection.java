package com.tiximax.txm.Model.Projections;

import java.time.LocalDateTime;

public interface ShipCodeBasicProjection {
    String getShipCode();
    Long getCustomerId();
    String getCustomerName();
    LocalDateTime getCreatedAt();
}
