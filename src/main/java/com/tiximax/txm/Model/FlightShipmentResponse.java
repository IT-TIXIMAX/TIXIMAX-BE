package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter

public class FlightShipmentResponse {
    private Long flightShipmentId;
    private String flightCode;
    private String awbFilePath;
    private String invoiceFilePath;
    private BigDecimal totalVolumeWeight;
    private BigDecimal airFreightCost;
    private BigDecimal customsClearanceCost;
    private BigDecimal airportShippingCost;
    private BigDecimal otherCosts;
    private BigDecimal totalCost;
    private BigDecimal originCostPerKg;
    private BigDecimal grossProfit;
    private LocalDateTime arrivalDate;
    private String status;
    private boolean airFreightPaid;
    private LocalDateTime airFreightPaidDate;
    private boolean customsPaid;
    private LocalDateTime customsPaidDate;
    private String staffName;
    private LocalDateTime createdAt;
    private int numberOfWarehouses;
}
