package com.tiximax.txm.Model.DTOResponse.DashBoard;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class InventoryDaily {
    private PendingSummary pending;
    private StockSummary stock;
    private PackedSummary packed;
    private PackedSummary awaitFlight;
    private List<LocationSummary> pendingByLocation;
    private List<LocationSummary> stockByLocation;
    private List<LocationSummary> packedByLocation;
    private List<LocationSummary> awaitFlightByLocation;

    public InventoryDaily(PendingSummary pending,
                          StockSummary stock,
                          PackedSummary packed,
                          PackedSummary awaitFlight,
                          List<LocationSummary> pendingByLocation,
                          List<LocationSummary> stockByLocation,
                          List<LocationSummary> packedByLocation,
                          List<LocationSummary> awaitFlightByLocation){
        this.pending = pending;
        this.stock = stock;
        this.packed = packed;
        this.awaitFlight = awaitFlight;
        this.pendingByLocation = pendingByLocation;
        this.stockByLocation = stockByLocation;
        this.packedByLocation = packedByLocation;
        this.awaitFlightByLocation = awaitFlightByLocation;
    }
}
