package com.tiximax.txm.Model.DTOResponse.Warehouse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class PerformanceWHSummary {
    private long totalInboundCount;
    private double totalInboundKg;
    private double totalInboundNetKg;
    private long totalPackedCount;
    private double totalPackedKg;
}
