package com.tiximax.txm.Model.DTORequest.Order;

import java.util.List;

public class UpdateDestinationBatchRequest {
    private List<String> shipmentCodes;
    private Long destinationId;

    // Getters and Setters
    public List<String> getShipmentCodes() { return shipmentCodes; }
    public void setShipmentCodes(List<String> shipmentCodes) { this.shipmentCodes = shipmentCodes; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }
}