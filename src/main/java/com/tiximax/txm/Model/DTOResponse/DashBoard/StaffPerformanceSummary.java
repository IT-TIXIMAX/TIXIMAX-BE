package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;@Data
@Getter
@Setter



public class StaffPerformanceSummary {
    private String staffCode;
    private String name;
    private String department;
    private Long totalOrders;
    private Long completedOrders;
    private Double completionRate;
    private Long totalParcels;

    public StaffPerformanceSummary(
            String staffCode,
            String name,
            String department,
            Long totalOrders,
            Long completedOrders,
            Double completionRate,
            Long totalParcels
    ) {
        this.staffCode = staffCode;
        this.name = name;
        this.department = department;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.completionRate = completionRate;
        this.totalParcels = totalParcels;
    }
}
