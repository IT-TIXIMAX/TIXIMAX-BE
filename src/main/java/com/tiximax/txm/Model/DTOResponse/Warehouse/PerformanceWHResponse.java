package com.tiximax.txm.Model.DTOResponse.Warehouse;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder

public class PerformanceWHResponse {
    private List<PerformanceWHDaily> dailyData;
    private PerformanceWHSummary totals;
}
