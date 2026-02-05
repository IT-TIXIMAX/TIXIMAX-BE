package com.tiximax.txm.Model.DTOResponse.Warehouse;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.chrono.Chronology;

@Data
@Builder

public class PerformanceWHDaily {
    private LocalDate date;
    private long inboundCount;
    private double inboundKg;
    private double inboundNetKg;
    private long packedCount;
    private double packedKg;
}
