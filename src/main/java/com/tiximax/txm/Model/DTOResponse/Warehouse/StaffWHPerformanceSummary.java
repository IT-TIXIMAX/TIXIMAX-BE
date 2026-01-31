package com.tiximax.txm.Model.DTOResponse.Warehouse;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder

public class StaffWHPerformanceSummary {
    private Long staffId;
    private String staffCode;
    private String name;
    private String department;
    private PerformanceWHSummary totals;
    private List<PerformanceWHDaily> dailyData;
}
