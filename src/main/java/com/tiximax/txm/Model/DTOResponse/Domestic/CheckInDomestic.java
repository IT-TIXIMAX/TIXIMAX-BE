package com.tiximax.txm.Model.DTOResponse.Domestic;

import lombok.Data;

@Data
public class CheckInDomestic {
    public String orderCode;
    public String shipmentCode;
    public String customerCode;
    public String destinationName;
    public int waitImport;
    public int imported;
    public int inventory;
    public int totalWarehouseInFlight;
}
