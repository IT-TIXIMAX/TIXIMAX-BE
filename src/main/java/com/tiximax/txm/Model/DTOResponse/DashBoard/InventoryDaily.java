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
    private List<LocationSummary> pendingByLocation;
    private List<LocationSummary> stockByLocation;
    private List<LocationSummary> packedByLocation;

    public InventoryDaily(PendingSummary pending, StockSummary stock, PackedSummary packed, List<LocationSummary> pendingByLocation, List<LocationSummary> stockByLocation, List<LocationSummary> packedByLocation){
        this.pending = pending;
        this.stock = stock;
        this.packed = packed;
        this.pendingByLocation = pendingByLocation;
        this.stockByLocation = stockByLocation;
        this.packedByLocation = packedByLocation;
    }
}
