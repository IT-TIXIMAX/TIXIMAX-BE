package com.tiximax.txm.Model.Projections;

import java.time.LocalDate;

public interface ExportedQuantityProjection {
    LocalDate getDate();
    Long getTotalCode();
    Double getTotalWeight();
    Long getTotalCustomers();
}
