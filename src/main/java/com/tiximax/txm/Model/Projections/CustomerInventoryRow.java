package com.tiximax.txm.Model.Projections;

public interface CustomerInventoryRow {
    String getCustomerCode();
    String getCustomerName();
    String getStaffCode();
    String getStaffName();
    Long getExportedCode();
    Double getExportedWeightKg();
    Long getRemainingCode();
    Double getRemainingWeightKg();
}
