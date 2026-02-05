package com.tiximax.txm.Model.Projections;

public interface CustomerInventoryProjection {
    String getCustomerCode();
    String getCustomerName();
    String getStaffCode();
    String getStaffName();
    Long getExportedCode();
    Double getExportedWeight();
    Long getRemainingCode();
    Double getRemainingWeight();
}
